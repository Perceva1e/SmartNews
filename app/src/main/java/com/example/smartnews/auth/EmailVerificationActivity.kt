package com.example.smartnews.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.activity.BaseActivity
import com.example.smartnews.R
import com.example.smartnews.activity.MainActivity
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmailVerificationActivity : BaseActivity() {
    private lateinit var tvEmail: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var btnResend: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var email: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var isChecking = true

    private val checkVerificationRunnable = object : Runnable {
        override fun run() {
            if (isChecking) {
                checkEmailVerification()
                handler.postDelayed(this, 5000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        auth = FirebaseAuth.getInstance()
        tvEmail = findViewById(R.id.tvEmail)
        tvInstruction = findViewById(R.id.tvInstruction)
        btnResend = findViewById(R.id.btnResend)
        progressBar = findViewById(R.id.progressBar)

        email = intent.getStringExtra("EMAIL") ?: ""
        tvEmail.text = getString(R.string.verification_sent_to, email)

        btnResend.setOnClickListener {
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                btnResend.isEnabled = false
                try {
                    val user = auth.currentUser
                    if (user != null && user.email == email) {
                        user.sendEmailVerification().await()
                        showCustomDialog(
                            getString(R.string.success_title),
                            getString(R.string.success_code_sent),
                            R.layout.custom_dialog_success
                        )
                    } else {
                        showCustomDialog(
                            getString(R.string.error_title),
                            getString(R.string.error_verification_failed),
                            R.layout.custom_dialog_error
                        )
                    }
                } catch (e: Exception) {
                    showCustomDialog(
                        getString(R.string.error_title),
                        getString(R.string.error_send_code_failed) + ": ${e.message}",
                        R.layout.custom_dialog_error
                    )
                } finally {
                    progressBar.visibility = View.GONE
                    btnResend.isEnabled = true
                }
            }
        }
        startVerificationCheck()
    }

    private fun startVerificationCheck() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            isChecking = true
            handler.post(checkVerificationRunnable)
        }
    }

    private fun checkEmailVerification() {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user != null && user.email == email) {
                    user.reload().await()
                    if (user.isEmailVerified) {
                        isChecking = false
                        progressBar.visibility = View.GONE
                        firestore.collection("users").document(email).update("verified", true).await()
                        val localDb = DatabaseHelper(this@EmailVerificationActivity)
                        val dbUser = localDb.getUserByEmail(email)
                        if (dbUser != null) {
                            showCustomDialog(
                                getString(R.string.success_title),
                                getString(R.string.success_verified),
                                R.layout.custom_dialog_success
                            ) {
                                startActivity(Intent(this@EmailVerificationActivity, MainActivity::class.java).apply {
                                    putExtra("USER_ID", dbUser.id)
                                })
                                handler.removeCallbacks(checkVerificationRunnable)
                                finish()
                            }
                        } else {
                            showCustomDialog(
                                getString(R.string.error_title),
                                getString(R.string.error_user_not_found),
                                R.layout.custom_dialog_error
                            )
                        }
                    }
                } else {
                    isChecking = false
                    progressBar.visibility = View.GONE
                    showCustomDialog(
                        getString(R.string.error_title),
                        getString(R.string.error_verification_failed),
                        R.layout.custom_dialog_error
                    )
                }
            } catch (e: Exception) {
                isChecking = false
                progressBar.visibility = View.GONE
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_verification_failed) + ": ${e.message}",
                    R.layout.custom_dialog_error
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isChecking = false
        handler.removeCallbacks(checkVerificationRunnable)
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
        dialogView.findViewById<MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}