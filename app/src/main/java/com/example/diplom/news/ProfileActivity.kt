package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.auth.LoginActivity
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityProfileBinding
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: NewsViewModel by viewModels {
        NewsViewModelFactory(
            NewsRepository(
                AppDatabase.getDatabase(this).userDao(),
                AppDatabase.getDatabase(this).newsDao(),
                NewsApi.service
            )
        )
    }
    private var userId: Int = -1
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        setupViews()
        loadUserData()
        setupNavigation()
    }
    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        loadUserData()
    }
    private fun setupViews() {
        binding.btnSaveChanges.setOnClickListener {
            val newName = binding.etName.text.toString()
            val newEmail = binding.etEmail.text.toString()

            if (validateInput(newName, newEmail)) {
                viewModel.updateUser(userId, newName, newEmail)
                showToast("Changes saved")
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.etPassword)

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Enter your password to confirm deletion")
            .setView(dialogView)
            .setPositiveButton("Delete") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    deleteUser(password)
                } else {
                    passwordInput.error = "Password required"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(password: String) {
        val user = auth.currentUser
        val email = user?.email ?: run {
            showToast("User not authenticated")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    user.delete()
                        .addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                viewModel.deleteUser(userId)
                                logoutUser()
                            } else {
                                showToast("Deletion failed: ${deleteTask.exception?.message}")
                            }
                        }
                } else {
                    showToast("Authentication failed: ${authTask.exception?.message}")
                }
            }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private fun loadUserData() {
        viewModel.user.observe(this) { user ->
            user?.let {
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
            }
        }
        viewModel.loadUser(userId)
    }

    private fun validateInput(name: String, email: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            binding.tilName.error = "Name required"
            isValid = false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email required"
            isValid = false
        }
        return isValid
    }

    private fun setupNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }
                R.id.navigation_saved -> {
                    startActivity(
                        Intent(this, SavedNewsActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }
                R.id.navigation_recommend -> {
                    startActivity(
                        Intent(this, RecommendActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }
                R.id.navigation_profile -> true
                else -> false
            }
        }
    }


    private fun applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}