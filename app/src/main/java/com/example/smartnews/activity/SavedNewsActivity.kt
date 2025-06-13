package com.example.smartnews.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import java.util.Locale

class SavedNewsActivity : AppCompatActivity() {
    private var userId: Int = -1
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }
    private val categories = listOf("business", "entertainment", "general", "health", "science", "sports", "technology")

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_news)

        supportActionBar?.title = getString(R.string.save_news)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        val categoryContainer = findViewById<LinearLayout>(R.id.categoryContainer)

        for (category in categories) {
            val button = LayoutInflater.from(this).inflate(R.layout.category_item, categoryContainer, false) as Button
            button.text = category.replaceFirstChar { it.uppercase() }
            button.setOnClickListener {
                val intent = Intent(this, CategorySavedNewsActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("CATEGORY", category)
                }
                startActivity(intent)
            }
            categoryContainer.addView(button)
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
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
                R.id.navigation_saved -> true
                R.id.navigation_recommend -> true
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    })
                    applyTransition()
                    true
                }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.navigation_saved
    }

    private fun applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }
}