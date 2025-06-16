package com.example.smartnews.activity

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }
    private var lastKnownLanguage: String? = null

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
        Log.d("PaymentActivity", "attachBaseContext: Language set to $language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        dbHelper = DatabaseHelper(this)
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e("PaymentActivity", "Invalid USER_ID, finishing activity")
            finish()
            return
        }

        lastKnownLanguage = sharedPref.getString("app_language", "ru")
        Log.d("PaymentActivity", "onCreate: Language set to $lastKnownLanguage")

        val etCardNumber = findViewById<EditText>(R.id.etCardNumber)
        val etCardHolder = findViewById<EditText>(R.id.etCardHolder)
        val etExpirationMonth = findViewById<EditText>(R.id.etExpirationMonth)
        val etExpirationYear = findViewById<EditText>(R.id.etExpirationYear)
        val etCvv = findViewById<EditText>(R.id.etCvv)
        val btnPay = findViewById<MaterialButton>(R.id.btnPay)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        btnPay.setOnClickListener {
            val cardNumber = etCardNumber.text.toString().trim()
            val cardHolder = etCardHolder.text.toString().trim()
            val expirationMonth = etExpirationMonth.text.toString().trim()
            val expirationYear = etExpirationYear.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (validateCardData(cardNumber, cardHolder, expirationMonth, expirationYear, cvv)) {
                val user = dbHelper.getUser()
                if (user != null) {
                    lifecycleScope.launch {
                        try {
                            dbHelper.updateUser(user.id, user.name, user.email, user.password, user.newsCategories, true)
                            showCustomDialog(
                                getString(R.string.success_title),
                                getString(R.string.success_vip_message),
                                R.layout.custom_dialog_success
                            ) {
                                finish()
                            }
                        } catch (e: Exception) {
                            Log.e("PaymentActivity", "Error updating user VIP status: ${e.message}")
                            showCustomDialog(
                                getString(R.string.error_title),
                                getString(R.string.error_operation_failed),
                                R.layout.custom_dialog_error
                            )
                        }
                    }
                } else {
                    Log.e("PaymentActivity", "User not found for ID: $userId")
                    showCustomDialog(
                        getString(R.string.error_title),
                        getString(R.string.error_user_not_found),
                        R.layout.custom_dialog_error
                    )
                }
            } else {
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_invalid_fields),
                    R.layout.custom_dialog_error
                )
            }
        }

        ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLang = sharedPref.getString("app_language", "ru")
        Log.d("PaymentActivity", "onResume: Current language $currentLang, Last known $lastKnownLanguage")
        if (currentLang != lastKnownLanguage) {
            Log.d("PaymentActivity", "Language changed detected, recreating activity")
            lastKnownLanguage = currentLang
            recreate()
        }
    }

    private fun validateCardData(cardNumber: String, cardHolder: String, month: String, year: String, cvv: String): Boolean {
        return cardNumber.length == 16 && cardHolder.isNotEmpty() && month.length == 2 && year.length == 2 && cvv.length == 3
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