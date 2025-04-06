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
import com.example.diplom.news.EmailVerificationActivity
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.isValidEmail
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.AuthViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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

                Log.d("RegisterActivity", "Attempting Firebase registration")
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { firebaseTask ->
                        if (firebaseTask.isSuccessful) {
                            Log.d("RegisterActivity", "Firebase user created successfully")
                            val firebaseUser = auth.currentUser

                            Log.d("RegisterActivity", "Updating Firebase profile with name: $name")
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            firebaseUser?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        Log.d("RegisterActivity", "Firebase profile updated successfully")
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
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

                                                Log.d("RegisterActivity", "Starting email verification")
                                                startEmailVerificationActivity(userId.toInt(), email)
                                            } catch (e: Exception) {
                                                Log.e("RegisterActivity", "Error saving user to local DB", e)
                                                firebaseUser.delete()
                                                    .addOnCompleteListener {
                                                        Log.w("RegisterActivity", "Firebase user rolled back due to local DB error")
                                                        showToast(getString(R.string.error_registration_failed))
                                                    }
                                            }
                                        }
                                    } else {
                                        Log.e("RegisterActivity", "Firebase profile update failed", profileTask.exception)
                                        firebaseUser?.delete()
                                            ?.addOnCompleteListener {
                                                Log.w("RegisterActivity", "Firebase user rolled back due to profile update error")
                                                showToast(getString(R.string.error_profile_update))
                                            }
                                    }
                                }
                        } else {
                            Log.e("RegisterActivity", "Firebase registration failed", firebaseTask.exception)
                            showToast(getString(R.string.error_registration_failed))
                        }
                    }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "General registration error", e)
                showToast(getString(R.string.error_general))
            }
        }
    }

    private fun startEmailVerificationActivity(userId: Int, email: String) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, EmailVerificationActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        putExtra("EMAIL", email)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    showToast(getString(R.string.error_verification_email))
                }
            }
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
    override fun onBackPressed() {
        super.onBackPressed()
        FirebaseAuth.getInstance().signOut()
    }
}