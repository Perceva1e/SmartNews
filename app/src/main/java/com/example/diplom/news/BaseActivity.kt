package com.example.diplom.news

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.MyApplication
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val lastUserId = prefs.getInt("last_user_id", -1)
        val language = if (lastUserId != -1) {
            prefs.getString("language_$lastUserId", "en") ?: "en"
        } else {
            "en"
        }
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyUserLanguage()
        super.onCreate(savedInstanceState)
    }

    private fun applyUserLanguage() {
        val userId = intent.getIntExtra("USER_ID", -1)
        if (userId != -1) {
            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val language = prefs.getString("language_$userId", "en") ?: "en"
            (application as MyApplication).setLocale(language)
        }
    }
}