package com.example.diplom.news

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityChangePasswordBinding
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.SecurityUtils
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var viewModel: NewsViewModel
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = NewsRepository(
            AppDatabase.getDatabase(this).userDao(),
            AppDatabase.getDatabase(this).newsDao(),
            NewsApi.service
        )

        viewModel = NewsViewModelFactory(repository).create(NewsViewModel::class.java)

        setupUI()
    }

    private fun setupUI() {
        binding.btnChangePassword.setOnClickListener {
            showPasswordDialog()
        }
    }

    private fun showPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                if (validateInput(oldPassword, newPassword)) {
                    changePassword(oldPassword, newPassword)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        val email = user?.email ?: run {
            showToast("User not authenticated")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                updateLocalPassword(email, newPassword)
                                showToast("Password changed successfully")
                                finish()
                            } else {
                                showToast("Update failed: ${updateTask.exception?.message}")
                            }
                        }
                } else {
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun updateLocalPassword(email: String, newPassword: String) {
        val hashedPassword = SecurityUtils.sha256(newPassword)
        viewModel.updateLocalPassword(email, hashedPassword)
    }

    private fun validateInput(oldPass: String, newPass: String): Boolean {
        return when {
            oldPass.isEmpty() -> {
                showToast("Enter current password")
                false
            }
            newPass.length < 6 -> {
                showToast("Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}