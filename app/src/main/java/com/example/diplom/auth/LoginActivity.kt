package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.api.NewsApi
import com.example.diplom.databinding.ActivityLoginBinding
import com.example.diplom.news.MainActivity
import com.example.diplom.repository.NewsRepository
import com.example.diplom.database.AppDatabase
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = NewsRepository(
            AppDatabase.getDatabase(this).userDao(),
            AppDatabase.getDatabase(this).newsDao(),
            NewsApi.service
        )

        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                CoroutineScope(Dispatchers.Main).launch {
                    val user = viewModel.login(email, SecurityUtils.sha256(password))
                    user?.let {
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java).apply {
                                putExtra("USER_ID", it.id)
                            }
                        )
                        finish()
                    } ?: showToast("Invalid credentials")
                }
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password required"
            isValid = false
        }
        return isValid
    }
}