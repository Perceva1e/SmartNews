package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.example.smartnews.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class SavedNewsActivity : BaseActivity() {
    private var userId: Int = -1
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }
    private val categoriesMap = mapOf(
        "general" to R.string.category_general,
        "business" to R.string.category_business,
        "entertainment" to R.string.category_entertainment,
        "health" to R.string.category_health,
        "science" to R.string.category_science,
        "sports" to R.string.category_sports,
        "technology" to R.string.category_technology
    )
    private val orderedCategories = listOf(
        "general",
        "business",
        "entertainment",
        "health",
        "science",
        "sports",
        "technology"
    )
    private var lastKnownLanguage: String? = null

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
        Log.d("SavedNewsActivity", "attachBaseContext: Language set to $language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_news)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e("SavedNewsActivity", "Invalid USER_ID, finishing activity")
            finish()
            return
        }

        lastKnownLanguage = sharedPref.getString("app_language", "ru")
        Log.d("SavedNewsActivity", "onCreate: Language set to $lastKnownLanguage")

        supportActionBar?.title = getString(R.string.save_news)
        refreshCategoryButtons()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        val currentLang = sharedPref.getString("app_language", "ru")
        Log.d("SavedNewsActivity", "onResume: Current language $currentLang, Last known $lastKnownLanguage")
        if (currentLang != lastKnownLanguage) {
            Log.d("SavedNewsActivity", "Language changed detected, recreating activity")
            lastKnownLanguage = currentLang
            recreate()
        } else {
            findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.navigation_saved
            refreshCategoryButtons()
        }
    }

    private fun refreshCategoryButtons() {
        val categoryContainer = findViewById<LinearLayout>(R.id.categoryContainer)
        categoryContainer.removeAllViews()

        for (categoryKey in orderedCategories) {
            categoriesMap[categoryKey]?.let { categoryResId ->
                val button = LayoutInflater.from(this).inflate(R.layout.category_item, categoryContainer, false) as Button
                button.text = getString(categoryResId).replaceFirstChar { it.uppercase() }
                button.setOnClickListener {
                    val intent = Intent(this, CategorySavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        putExtra("CATEGORY", categoryKey)
                    }
                    startActivity(intent)
                }
                categoryContainer.addView(button)
            }
        }
        supportActionBar?.title = getString(R.string.save_news)
        Log.d("SavedNewsActivity", "refreshCategoryButtons: Updated with language ${sharedPref.getString("app_language", "ru")}")
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_saved -> true
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    })
                    applyTransition()
                    true
                }
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
}