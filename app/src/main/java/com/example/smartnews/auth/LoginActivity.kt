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
import com.example.smartnews.activity.MainActivity
import com.example.smartnews.bd.DatabaseHelper
import com.example.smartnews.bd.SavedNews
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt
import java.util.Locale

class LoginActivity : BaseActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button
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
                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        if (firebaseUser.isEmailVerified) {
                            var user = localDb.getUserByEmail(email)
                            if (user == null || !BCrypt.checkpw(password, user.password)) {
                                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                                val document = firestore.collection("users").document(email).get().await()
                                if (document.exists()) {
                                    val firestoreUser = document.data
                                    val name = firestoreUser?.get("name") as? String ?: ""
                                    val newsCategories = firestoreUser?.get("news_categories") as? String
                                    val isVip = firestoreUser?.get("is_vip") as? Boolean ?: false
                                    val result = localDb.updateUser(user?.id ?: 0, name, email, hashedPassword, newsCategories, isVip)
                                    if (result > 0) {
                                        firestore.collection("users").document(email).update("password", hashedPassword).await()
                                    } else {
                                        val addResult = localDb.addUser(name, email, hashedPassword, newsCategories, isVip)
                                        if (addResult == -1L) {
                                            showCustomDialog(getString(R.string.error_title), getString(R.string.error_registration), R.layout.custom_dialog_error)
                                            return@launch
                                        }
                                    }
                                    user = localDb.getUserByEmail(email)
                                } else {
                                    showCustomDialog(getString(R.string.error_title), getString(R.string.error_invalid_credentials), R.layout.custom_dialog_error)
                                    return@launch
                                }
                            }
                            syncSavedNewsFromFirestore(email)
                            showCustomDialog(getString(R.string.success_title), getString(R.string.success_login), R.layout.custom_dialog_success) {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                                    putExtra("USER_ID", user!!.id)
                                })
                                finish()
                            }
                        } else {
                            showCustomDialog(
                                getString(R.string.error_title),
                                getString(R.string.error_not_verified),
                                R.layout.custom_dialog_error
                            ) {
                                val intent = Intent(this@LoginActivity, EmailVerificationActivity::class.java)
                                intent.putExtra("EMAIL", email)
                                startActivity(intent)
                            }
                        }
                    } else {
                        showCustomDialog(getString(R.string.error_title), getString(R.string.error_invalid_credentials), R.layout.custom_dialog_error)
                    }
                } catch (e: Exception) {
                    showCustomDialog(getString(R.string.error_title), getString(R.string.error_operation_failed) + ": ${e.message}", R.layout.custom_dialog_error)
                }
            }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<Button>(R.id.btnForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private suspend fun syncSavedNewsFromFirestore(email: String) {
        try {
            val querySnapshot = firestore.collection("saved_news")
                .whereEqualTo("email", email)
                .get()
                .await()
            for (document in querySnapshot.documents) {
                val news = SavedNews(
                    id = document.id.split("-").last().toInt(),
                    title = document.getString("title"),
                    description = document.getString("description"),
                    url = document.getString("url"),
                    urlToImage = document.getString("url_to_image"),
                    publishedAt = document.getString("published_at"),
                    category = document.getString("category") ?: ""
                )
                val existingNews = localDb.getSavedNewsByCategory(email, news.category)
                    .find { it.id == news.id }
                if (existingNews == null) {
                    localDb.saveNews(email, news)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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