package com.example.smartnews.auth

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.activity.BaseActivity
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt
import java.util.Locale

class RegisterActivity : BaseActivity() {
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button
    private lateinit var localDb: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        localDb = DatabaseHelper(this)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_empty_fields),
                    R.layout.custom_dialog_error
                )
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_invalid_email),
                    R.layout.custom_dialog_error
                )
                return@setOnClickListener
            }

            if (password.length < 6 || !password.contains(Regex("[^a-zA-Z0-9]"))) {
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_invalid_password),
                    R.layout.custom_dialog_error
                )
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        firebaseUser.sendEmailVerification().await()
                        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                        val result = localDb.addUser(name, email, hashedPassword)
                        if (result != -1L) {
                            val userMap = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "password" to hashedPassword,
                                "verified" to false
                            )
                            firestore.collection("users").document(email).set(userMap).await()

                            showCustomDialog(
                                getString(R.string.success_title),
                                getString(R.string.success_registration_verify_email),
                                R.layout.custom_dialog_success
                            ) {
                                val intent = Intent(this@RegisterActivity, EmailVerificationActivity::class.java)
                                intent.putExtra("EMAIL", email)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            firebaseUser.delete().await()
                            showCustomDialog(
                                getString(R.string.error_title),
                                getString(R.string.error_registration),
                                R.layout.custom_dialog_error
                            )
                        }
                    } else {
                        showCustomDialog(
                            getString(R.string.error_title),
                            getString(R.string.error_registration),
                            R.layout.custom_dialog_error
                        )
                    }
                } catch (e: Exception) {
                    showCustomDialog(
                        getString(R.string.error_title),
                        getString(R.string.error_registration) + ": ${e.message}",
                        R.layout.custom_dialog_error
                    )
                }
            }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
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