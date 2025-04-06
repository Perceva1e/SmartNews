package com.example.diplom

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import java.util.Locale

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this) {}
        applyUserLanguage()
    }

    fun applyUserLanguage() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val defaultUserId = prefs.getInt("last_user_id", -1)
        val language = if (defaultUserId != -1) {
            prefs.getString("language_$defaultUserId", "en") ?: "en"
        } else {
            "en"
        }
        setLocale(language)
    }

    fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        resources.updateConfiguration(config, resources.displayMetrics)
        Log.d("MyApplication", "Установлена локаль приложения: $language")
    }

    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val defaultUserId = prefs.getInt("last_user_id", -1)
        val language = if (defaultUserId != -1) {
            prefs.getString("language_$defaultUserId", "en") ?: "en"
        } else {
            "en"
        }
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val newContext = base.createConfigurationContext(config)
        Log.d("MyApplication", "attachBaseContext установил локаль: $language")
        super.attachBaseContext(newContext)
    }
}