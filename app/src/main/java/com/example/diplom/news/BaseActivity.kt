package com.example.diplom.news

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.diplom.MyApplication
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    companion object {
        const val ACTION_LANGUAGE_CHANGED = "com.example.diplom.LANGUAGE_CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
    }

    protected fun loadLocale() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val lastUserId = prefs.getInt("last_user_id", -1)
        val language = if (lastUserId != -1) {
            prefs.getString("language_$lastUserId", "en") ?: "en"
        } else {
            "en"
        }
        setLocale(language, false)
    }

    protected open fun setLocale(language: String, notify: Boolean = true) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        resources.updateConfiguration(config, resources.displayMetrics)
        (application as MyApplication).setLocale(language)

        if (notify) {
            val intent = Intent(ACTION_LANGUAGE_CHANGED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

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
        val config = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}