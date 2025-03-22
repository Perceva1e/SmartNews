package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.api.NewsApi
import com.example.diplom.databinding.ActivityRegisterBinding
import com.example.diplom.database.entity.User
import com.example.diplom.repository.NewsRepository
import com.example.diplom.database.AppDatabase
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = NewsRepository(
            AppDatabase.getDatabase(this).userDao(),
            AppDatabase.getDatabase(this).newsDao(),
            NewsApi.service
        )
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInput(name, email, password)) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (viewModel.isUserExists(email)) {
                        showToast("User already exists")
                    } else {
                        val user = User(
                            name = name,
                            email = email,
                            password = SecurityUtils.sha256(password)
                        )
                        viewModel.registerUser(user)
                        showToast("Registration successful")
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            binding.tilName.error = "Name required"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email required"
            isValid = false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            isValid = false
        }
        return isValid
    }
}