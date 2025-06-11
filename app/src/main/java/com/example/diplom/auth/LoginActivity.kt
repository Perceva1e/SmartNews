package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityLoginBinding
import com.example.diplom.news.BaseActivity
import com.example.diplom.news.MainActivity
import com.example.diplom.news.ResetPasswordActivity
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.isValidEmail
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
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
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                authenticateUser(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }
    }

    private fun authenticateUser(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val localUser = viewModel.login(email, SecurityUtils.sha256(password))
            localUser?.let {
                startMainActivity(it.id)
            } ?: showToast("Invalid credentials")
        }
    }

    private fun startMainActivity(userId: Int) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email required"
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            isValid = false
        }

        return isValid
    }
}