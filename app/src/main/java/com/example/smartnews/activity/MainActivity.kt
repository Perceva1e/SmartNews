package com.example.smartnews.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnews.R
import com.example.smartnews.adapter.NewsAdapter
import com.example.smartnews.adapter.NewsLoader
import com.example.smartnews.adapter.SpacingItemDecoration
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private var userId: Int = -1
    private lateinit var adContainer: FrameLayout
    private lateinit var adView: AdView
    private lateinit var ivCloseAd: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adRequest: AdRequest
    private val showAdRunnable = Runnable {
        adContainer.visibility = View.VISIBLE
        adView.loadAd(adRequest)
    }
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedLang = sharedPref.getString("app_language", "ru")
        setLocale(savedLang ?: "ru")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e("MainActivity", "Invalid USER_ID, finishing activity")
            finish()
            return
        }
        Log.d("MainActivity", "User ID: $userId received, proceeding")

        dbHelper = DatabaseHelper(this)
        val user = dbHelper.getUserById(userId)
        val email = user?.email ?: ""
        if (email.isEmpty()) {
            Log.e("MainActivity", "User email not found for userId: $userId, finishing activity")
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing)
        recyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))
        newsAdapter = NewsAdapter(email = email)
        recyclerView.adapter = newsAdapter

        setupBottomNavigation()
        loadNews(email)

        adContainer = findViewById(R.id.adContainer)
        adView = findViewById(R.id.adView)
        ivCloseAd = findViewById(R.id.ivCloseAd)
        adRequest = AdRequest.Builder().build()

        updateAdVisibility()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.navigation_home
        loadNews(dbHelper.getUserById(userId)?.email ?: "")
        updateAdVisibility()
    }

    override fun onDestroy() {
        adView.destroy()
        handler.removeCallbacks(showAdRunnable)
        super.onDestroy()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadNews(dbHelper.getUserById(userId)?.email ?: "")
                    true
                }
                R.id.navigation_saved -> {
                    startActivity(Intent(this, SavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_recommend -> {
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
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun loadNews(email: String) {
        NewsLoader.loadNews(this, newsAdapter, email)
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

    private fun updateAdVisibility() {
        val user = dbHelper.getUser()
        if (user != null && user.isVip) {
            adContainer.visibility = View.GONE
            handler.removeCallbacks(showAdRunnable)
        } else {
            MobileAds.initialize(this) {}
            ivCloseAd.visibility = View.GONE
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    ivCloseAd.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    ivCloseAd.visibility = View.GONE
                    adContainer.visibility = View.GONE
                }
            }
            adView.loadAd(adRequest)
            ivCloseAd.setOnClickListener {
                adContainer.visibility = View.GONE
                ivCloseAd.visibility = View.GONE
                handler.postDelayed(showAdRunnable, 10000)
            }
        }
    }
}