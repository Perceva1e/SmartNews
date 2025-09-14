package com.example.smartnews.auth

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.activity.BaseActivity
import com.example.smartnews.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordActivity : BaseActivity() {
    private lateinit var etEmail: EditText
    private lateinit var btnSendResetEmail: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivBack: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail)
        progressBar = findViewById(R.id.progressBar)
        ivBack = findViewById(R.id.ivBack)

        ivBack.setOnClickListener {
            finish()
        }

        btnSendResetEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_empty_email),
                    R.layout.custom_dialog_error
                )
                return@setOnClickListener
            }

            lifecycleScope.launch {
                animateProgressBar(true)
                btnSendResetEmail.isEnabled = false
                try {
                    Log.d("ForgotPassword", "Attempting to send reset email to: $email")
                    auth.sendPasswordResetEmail(email).await()
                    Log.d("ForgotPassword", "Reset email sent successfully")
                    showCustomDialog(
                        getString(R.string.success_title),
                        getString(R.string.success_password_reset_email),
                        R.layout.custom_dialog_success
                    ) {
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("ForgotPassword", "Error sending reset email: ${e.message}", e)
                    val errorMessage = when {
                        e.message?.contains("no user record") == true -> getString(R.string.error_email_not_found)
                        else -> getString(R.string.error_password_reset_failed) + ": ${e.message}"
                    }
                    showCustomDialog(
                        getString(R.string.error_title),
                        errorMessage,
                        R.layout.custom_dialog_error
                    )
                } finally {
                    animateProgressBar(false)
                    btnSendResetEmail.isEnabled = true
                }
            }
        }
    }

    private fun animateProgressBar(show: Boolean) {
        val alpha = if (show) 1f else 0f
        ObjectAnimator.ofFloat(progressBar, View.ALPHA, alpha).apply {
            duration = 300
            start()
        }.also {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int, onOk: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}