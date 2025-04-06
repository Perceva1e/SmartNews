package com.example.diplom.news

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.example.diplom.databinding.ActivityResetPasswordBinding
import com.example.diplom.utils.showToast
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            handleBackAction()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                binding.tilEmail.error = "Email required"
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Password reset email sent")
                    finish()
                } else {
                    showToast("Error: ${task.exception?.message}")
                }
            }
    }
    private fun handleBackAction() {
        if (!isFinishing) {
            supportFinishAfterTransition()
        }
    }
}