package com.example.smartnews.auth

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.R
import com.example.smartnews.activity.MainActivity
import com.example.smartnews.bd.DatabaseHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button
    private lateinit var localDb: DatabaseHelper
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        localDb = DatabaseHelper(this)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showCustomDialog(getString(R.string.error_title), getString(R.string.error_empty_fields), R.layout.custom_dialog_error)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val user = localDb.checkUser(email, password)
                    if (user != null) {
                        showCustomDialog(getString(R.string.success_title), getString(R.string.success_login), R.layout.custom_dialog_success) {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                                putExtra("USER_ID", user.id)
                            })
                            finish()
                        }
                    } else {
                        val document = firestore.collection("users").document(email).get().await()
                        if (document.exists()) {
                            val firestoreUser = document.data
                            val firestorePassword = firestoreUser?.get("password") as? String
                            if (firestorePassword == password) {
                                val name = firestoreUser["name"] as? String ?: ""
                                val newsCategories = firestoreUser["news_categories"] as? String
                                val isVip = firestoreUser["is_vip"] as? Boolean ?: false
                                val result = localDb.addUser(name, email, password, newsCategories, isVip)
                                if (result != -1L) {
                                    showCustomDialog(getString(R.string.success_title), getString(R.string.success_login), R.layout.custom_dialog_success) {
                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                                            putExtra("USER_ID", result.toInt())
                                        })
                                        finish()
                                    }
                                } else {
                                    showCustomDialog(getString(R.string.error_title), getString(R.string.error_registration), R.layout.custom_dialog_error)
                                }
                            } else {
                                showCustomDialog(getString(R.string.error_title), getString(R.string.error_invalid_credentials), R.layout.custom_dialog_error)
                            }
                        } else {
                            showCustomDialog(getString(R.string.error_title), getString(R.string.error_invalid_credentials), R.layout.custom_dialog_error)
                        }
                    }
                } catch (e: Exception) {
                    showCustomDialog(getString(R.string.error_title), getString(R.string.error_operation_failed), R.layout.custom_dialog_error)
                }
            }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
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