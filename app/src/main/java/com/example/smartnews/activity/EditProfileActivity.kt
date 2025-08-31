package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class EditProfileActivity : BaseActivity() {

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        lastKnownLanguage = sharedPref.getString("app_language", "ru")

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val spLanguage = findViewById<Spinner>(R.id.spLanguage)
        val spCurrency = findViewById<Spinner>(R.id.spCurrency)
        val ivLanguageIcon = findViewById<ImageView>(R.id.ivLanguageIcon)
        val ivCurrencyIcon = findViewById<ImageView>(R.id.ivCurrencyIcon)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val user = dbHelper.getUser()
        if (user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
        }

        val currentLang = sharedPref.getString("app_language", "ru")
        val currentCurrency = sharedPref.getString("app_currency", "RUB")

        val languages = resources.getStringArray(R.array.languages)
        val currencies = resources.getStringArray(R.array.currencies)

        val languageAdapter = ArrayAdapter(this, R.layout.spinner_item, languages)
        languageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spLanguage.adapter = languageAdapter

        val currencyAdapter = ArrayAdapter(this, R.layout.spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spCurrency.adapter = currencyAdapter

        val languageIndex = languages.indexOfFirst { it.contains(currentLang ?: "ru", ignoreCase = true) }
        spLanguage.setSelection(if (languageIndex != -1) languageIndex else 0)

        val currencyIndex = currencies.indexOfFirst { it.startsWith(currentCurrency ?: "RUB") }
        spCurrency.setSelection(if (currencyIndex != -1) currencyIndex else 0)

        updateLanguageIcon(spLanguage.selectedItemPosition, ivLanguageIcon)
        updateCurrencyIcon(spCurrency.selectedItemPosition, ivCurrencyIcon)

        spLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateLanguageIcon(position, ivLanguageIcon)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCurrencyIcon(position, ivCurrencyIcon)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val language = if (spLanguage.selectedItemPosition == 0) "ru" else "en"
            val currency = currencies[spCurrency.selectedItemPosition].substringBefore(" ")

            if (name.isNotEmpty() && email.isNotEmpty()) {
                lifecycleScope.launch {
                    val existingUser = dbHelper.getUser()
                    if (existingUser != null) {
                        dbHelper.updateUser(existingUser.id, name, email, existingUser.password, existingUser.newsCategories, existingUser.isVip)
                    } else {
                        dbHelper.addUser(name, email, "")
                    }
                    with(sharedPref.edit()) {
                        putString("app_language", language)
                        putString("app_currency", currency)
                        apply()
                    }
                    showCustomDialog(getString(R.string.success_title), getString(R.string.saved), R.layout.custom_dialog_success) {
                        // Перезапускаем приложение с новой локалью
                        val intent = Intent(this@EditProfileActivity, MainActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            } else {
                showCustomDialog(getString(R.string.error_title), getString(R.string.error_empty_fields), R.layout.custom_dialog_error)
            }
        }

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLang = sharedPref.getString("app_language", "ru")
        if (currentLang != lastKnownLanguage) {
            lastKnownLanguage = currentLang
            recreate()
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

    private fun updateLanguageIcon(position: Int, imageView: ImageView) {
        val drawableId = when (position) {
            0 -> R.drawable.ic_flag_ru
            1 -> R.drawable.ic_flag_en
            else -> R.drawable.ic_flag_ru
        }
        imageView.setImageResource(drawableId)
    }

    private fun updateCurrencyIcon(position: Int, imageView: ImageView) {
        val currency = resources.getStringArray(R.array.currencies)[position].substringBefore(" ")
        val drawableId = when (currency) {
            "RUB" -> R.drawable.ic_rub
            "USD" -> R.drawable.ic_usd
            else -> R.drawable.ic_rub
        }
        imageView.setImageResource(drawableId)
    }
}