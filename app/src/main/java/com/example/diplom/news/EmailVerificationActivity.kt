package com.example.diplom.news

import android.content.Intent
import android.net.ConnectivityManager
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
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class EmailVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var handler: Handler
    private val checkInterval = 5000L
    private var userId: Int = -1
    private var lastResendTime = 0L
    private val RESEND_DELAY = TimeUnit.MINUTES.toMillis(1)

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
        binding.btnResend.setOnClickListener {
            if (isOnline()) {
                resendVerificationEmail()
            } else {
                showToast(getString(R.string.no_internet))
            }
        }
        binding.btnCancel.setOnClickListener { cancelRegistration() }
    }

    private fun isOnline(): Boolean {
        val connMgr = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return connMgr.activeNetworkInfo?.isConnected == true
    }

    private fun resendVerificationEmail() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastResendTime < RESEND_DELAY) {
            val remainingTime = RESEND_DELAY - (currentTime - lastResendTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime)
            showToast(getString(R.string.resend_limit, minutes + 1))
            return
        }

        val user = auth.currentUser ?: run {
            showToast(getString(R.string.user_not_found))
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                user.sendEmailVerification()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            lastResendTime = System.currentTimeMillis()
                            CoroutineScope(Dispatchers.Main).launch {
                                showToast(getString(R.string.verification_email_resent))
                                handler.removeCallbacks(checkVerificationRunnable)
                                handler.postDelayed(checkVerificationRunnable, checkInterval)
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                handleSendError(task.exception)
                            }
                        }
                    }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    handleSendError(e)
                }
            }
        }
    }

    private suspend fun handleSendError(exception: Exception?) {
        val error = exception?.message ?: getString(R.string.unknown_error)
        Log.e("EmailResend", "Error: $error")

        withContext(Dispatchers.Main) {
            when {
                error.contains("TOO_MANY_ATTEMPTS_TRY_LATER", true) ->
                    showToast(getString(R.string.too_many_requests))
                error.contains("invalid_recipient", true) ->
                    showToast(getString(R.string.invalid_email))
                else -> showToast(getString(R.string.error_verification_email))
            }
        }
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

    private fun proceedToApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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

    override fun onDestroy() {
        handler.removeCallbacks(checkVerificationRunnable)
        super.onDestroy()
    }
}