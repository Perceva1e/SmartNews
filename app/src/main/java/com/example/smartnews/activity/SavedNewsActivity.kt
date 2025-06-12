package com.example.smartnews.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.smartnews.R
import com.example.smartnews.adapter.SavedNewsPagerAdapter
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Locale

class SavedNewsActivity : AppCompatActivity() {
    private var userId: Int = -1
    private lateinit var dbHelper: DatabaseHelper
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_news)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val categories = dbHelper.getAllCategories(userId).ifEmpty { listOf("general") }
        val adapter = SavedNewsPagerAdapter(this, userId, categories)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position].replaceFirstChar { it.uppercase() }
        }.attach()

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