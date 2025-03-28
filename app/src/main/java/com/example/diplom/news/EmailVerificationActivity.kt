package com.example.diplom.news

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.R
import com.example.diplom.auth.RegisterActivity
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityEmailVerificationBinding
import com.example.diplom.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var handler: Handler
    private val checkInterval = 5000L
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = intent.getIntExtra("USER_ID", -1)

        setupUI()
        startEmailVerificationCheck()
    }

    private fun setupUI() {
        binding.tvEmail.text = auth.currentUser?.email
        binding.btnResend.setOnClickListener { resendVerificationEmail() }
        binding.btnCancel.setOnClickListener { cancelRegistration() }
    }

    private fun startEmailVerificationCheck() {
        handler = Handler(Looper.getMainLooper())
        handler.postDelayed(checkVerificationRunnable, checkInterval)
    }

    private val checkVerificationRunnable = object : Runnable {
        override fun run() {
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                    proceedToApp()
                } else {
                    handler.postDelayed(this, checkInterval)
                }
            }
        }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user == null) {
            showToast("User not found, please register again")
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(getString(R.string.verification_email_resent))
                    handler.removeCallbacks(checkVerificationRunnable)
                    handler.postDelayed(checkVerificationRunnable, checkInterval)
                } else {
                    val error = task.exception?.message ?: "Unknown error"
                    showToast("Failed to resend: $error")
                    Log.e("EmailResend", "Error: $error")
                }
            }
    }

    private fun cancelRegistration() {
        CoroutineScope(Dispatchers.IO).launch {
            auth.currentUser?.delete()
            AppDatabase.getDatabase(this@EmailVerificationActivity)
                .userDao()
                .deleteUserByEmail(auth.currentUser?.email ?: "")
        }
        startActivity(Intent(this, RegisterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun proceedToApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkVerificationRunnable)
        super.onDestroy()
    }
}