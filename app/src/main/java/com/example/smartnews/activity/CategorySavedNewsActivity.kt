package com.example.smartnews.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.R
import com.example.smartnews.adapter.SavedNewsFragment
import java.util.Locale
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class CategorySavedNewsActivity : BaseActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_saved_news)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setContentInsetsAbsolute(0, 0)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            val category = intent.getStringExtra("CATEGORY")
            title = category?.let {
                categoriesMap[it]?.let { resId -> getString(resId).replaceFirstChar { it.uppercase() } } ?: it.replaceFirstChar { it.uppercase() }
            } ?: getString(R.string.save_news)
        }

        val arrowDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        arrowDrawable?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(this, android.R.color.white))
            supportActionBar?.setHomeAsUpIndicator(it)
        }

        val userId = intent.getIntExtra("USER_ID", -1)
        val category = intent.getStringExtra("CATEGORY")

        if (userId == -1 || category == null) {
            finish()
            return
        }

        val fragment = SavedNewsFragment.newInstance(userId, category)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }
}