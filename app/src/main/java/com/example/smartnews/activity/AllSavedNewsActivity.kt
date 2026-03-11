package com.example.smartnews.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import com.example.smartnews.R

class AllSavedNewsActivity : BaseActivity() {

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
        "general", "business", "entertainment", "health",
        "science", "sports", "technology"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_saved_news)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.save_news)
        }

        toolbar.setNavigationOnClickListener { finish() }

        refreshCategoryButtons()
    }

    private fun refreshCategoryButtons() {
        val categoryContainer = findViewById<LinearLayout>(R.id.categoryContainer)
        categoryContainer.removeAllViews()

        for (categoryKey in orderedCategories) {
            categoriesMap[categoryKey]?.let { resId ->
                val button = LayoutInflater.from(this)
                    .inflate(R.layout.category_item, categoryContainer, false) as Button

                button.text = getString(resId).replaceFirstChar { it.uppercase() }

                button.setOnClickListener {
                    startActivity(Intent(this, OfflineCategorySavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        putExtra("CATEGORY", categoryKey)
                    })
                }
                categoryContainer.addView(button)
            }
        }
        Log.d("AllSavedNewsActivity", "Кнопки категорий обновлены (оффлайн-режим)")
    }

    override fun checkInternetConnection() {
    }
}