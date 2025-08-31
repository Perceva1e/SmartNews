package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.utils.NetworkUtils
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val language = sharedPref.getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onResume() {
        super.onResume()
        checkInternetConnection()
    }

    open fun checkInternetConnection() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, NoInternetActivity::class.java))
        }
    }
}