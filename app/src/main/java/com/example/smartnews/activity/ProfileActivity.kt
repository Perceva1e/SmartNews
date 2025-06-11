package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        dbHelper = DatabaseHelper(this)

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentLang = sharedPref.getString("app_language", "ru")
        val currentCurrency = sharedPref.getString("app_currency", "RUB")

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val spLanguage = findViewById<Spinner>(R.id.spLanguage)
        val spCurrency = findViewById<Spinner>(R.id.spCurrency)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)

        val user = dbHelper.getUser()
        if (user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
        }

        val languages = resources.getStringArray(R.array.languages)
        val currencies = resources.getStringArray(R.array.currencies)
        spLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCurrency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spLanguage.setSelection(if (currentLang == "en") 1 else 0)
        spCurrency.setSelection(currencies.indexOfFirst { it.startsWith(currentCurrency ?: "RUB") })

        btnSave?.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val language = if (spLanguage.selectedItemPosition == 0) "ru" else "en"
            val currency = currencies[spCurrency.selectedItemPosition].substringBefore(" ")

            if (name.isNotEmpty() && email.isNotEmpty()) {
                val existingUser = dbHelper.getUser()
                if (existingUser != null) {
                    dbHelper.updateUser(existingUser.id, name, email, existingUser.password)
                } else {
                    dbHelper.addUser(name, email, "")
                }
                with(sharedPref.edit()) {
                    putString("app_language", language)
                    putString("app_currency", currency)
                    apply()
                }
                setLocale(language)
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeleteAccount?.setOnClickListener {
            val existingUser = dbHelper.getUser()
            if (existingUser != null) {
                dbHelper.deleteUser(existingUser.id)
                with(sharedPref.edit()) {
                    clear()
                    apply()
                }
                Toast.makeText(this, "Аккаунт удален", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Аккаунт не найден", Toast.LENGTH_SHORT).show()
            }
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_saved -> {
                    true
                }
                R.id.navigation_recommend -> {
                    true
                }
                R.id.navigation_profile -> {
                    true
                }
                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.navigation_profile
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        val intent = intent
        finish()
        startActivity(intent)
    }

    private fun applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(androidx.appcompat.app.AppCompatActivity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}