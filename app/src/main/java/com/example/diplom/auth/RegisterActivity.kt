package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.database.entity.User
import com.example.diplom.databinding.ActivityRegisterBinding
import com.example.diplom.news.BaseActivity
import com.example.diplom.news.MainActivity
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.isValidEmail
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {
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

        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        setupListeners()
    }

    private fun setupListeners() {
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(name, email, password)) {
                registerUser(name, email, password)
            }
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        Log.d("RegisterActivity", "Starting registration for: $email")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("RegisterActivity", "Checking if user exists in local DB")
                val userExists = viewModel.isUserExists(email)
                if (userExists) {
                    Log.w("RegisterActivity", "Registration failed - user already exists")
                    showToast(getString(R.string.error_user_exists))
                    return@launch
                }

                Log.d("RegisterActivity", "Hashing password")
                val hashedPassword = SecurityUtils.sha256(password)
                Log.d("RegisterActivity", "Creating local user object")
                val localUser = User(
                    name = name,
                    email = email,
                    password = hashedPassword
                )

                Log.d("RegisterActivity", "Saving user to local DB")
                val userId = viewModel.registerUser(localUser)
                Log.d("RegisterActivity", "User saved to DB with ID: $userId")

                Log.d("RegisterActivity", "Navigating to MainActivity")
                startMainActivity(userId.toInt())
            } catch (e: Exception) {
                Log.e("RegisterActivity", "General registration error", e)
                showToast(getString(R.string.error_general))
            }
        }
    }

    private fun startMainActivity(userId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_required)
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_length)
            isValid = false
        }

        return isValid
    }
}