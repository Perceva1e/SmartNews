package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.adapter.NewsAdapter
import com.example.smartnews.adapter.SpacingItemDecoration
import com.example.smartnews.api.model.News
import com.example.smartnews.bd.DatabaseHelper
import com.example.smartnews.bd.SavedNews
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

class RecommendActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
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
    private var lastLanguage: String? = null
    private var recommendationJob: Job? = null
    private var isLoadingRecommendations = false

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
        Log.d("RecommendActivity", "attachBaseContext: Language set to $language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e("RecommendActivity", "Invalid USER_ID, finishing activity")
            finish()
            return
        }
        Log.d("RecommendActivity", "User ID: $userId received, proceeding")

        lastLanguage = sharedPref.getString("app_language", "ru")

        dbHelper = DatabaseHelper(this)
        val user = dbHelper.getUserById(userId)
        val email = user?.email ?: ""
        if (email.isEmpty()) {
            Log.e("RecommendActivity", "User email not found for userId: $userId, finishing activity")
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

        adContainer = findViewById(R.id.adContainer)
        adView = findViewById(R.id.adView)
        ivCloseAd = findViewById(R.id.ivCloseAd)
        adRequest = AdRequest.Builder().build()

        updateAdVisibility()
        loadRecommendations(email)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val user = dbHelper.getUserById(userId)
            val email = user?.email ?: ""
            loadRecommendations(email)
            Log.d("RecommendActivity", "Filter updated, reloading recommendations for email: $email")
        }
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.navigation_recommend
        val user = dbHelper.getUserById(userId)
        val email = user?.email ?: ""
        val currentLanguage = sharedPref.getString("app_language", "ru")
        if (currentLanguage != lastLanguage) {
            Log.d("RecommendActivity", "Language changed from $lastLanguage to $currentLanguage, recreating activity")
            lastLanguage = currentLanguage
            recreate()
        } else {
            refreshUI(email)
        }
        updateAdVisibility()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onDestroy() {
        adView.destroy()
        handler.removeCallbacks(showAdRunnable)
        recommendationJob?.cancel()
        super.onDestroy()
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
                R.id.navigation_saved -> {
                    startActivity(Intent(this, SavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    })
                    applyTransition()
                    true
                }
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
        bottomNavigation.selectedItemId = R.id.navigation_recommend
    }

    private fun loadRecommendations(email: String) {
        if (isLoadingRecommendations) {
            Log.d("RecommendActivity", "loadRecommendations skipped, already in progress")
            return
        }
        isLoadingRecommendations = true
        recommendationJob?.cancel()
        recommendationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val savedNewsList = mutableListOf<SavedNews>()
                val categories = listOf("general", "business", "entertainment", "health", "science", "sports", "technology")
                categories.forEach { category ->
                    savedNewsList.addAll(dbHelper.getSavedNewsByCategory(email, category))
                }

                val requestMap = mapOf(
                    "saved_news" to savedNewsList
                )
                val requestBodyJson = Gson().toJson(requestMap)
                Log.d("RecommendActivity", "Request JSON: $requestBodyJson")

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("http://192.168.100.45:8000/recommend")
                    .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val type = object : TypeToken<List<News>>() {}.type
                    val recommendedNews: List<News> = Gson().fromJson(responseBody, type)

                    withContext(Dispatchers.Main) {
                        if (recommendedNews.isNotEmpty()) {
                            newsAdapter.setNews(recommendedNews)
                        } else {
                            showCustomDialog(
                                getString(R.string.error_title),
                                getString(R.string.error_no_recommendations)
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showCustomDialog(
                            getString(R.string.error_title),
                            getString(R.string.error_server_request_failed)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("RecommendActivity", "Error: ${e.message}", e)
                    showCustomDialog(
                        getString(R.string.error_title),
                        getString(R.string.error_loading_recommendations)
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingRecommendations = false
                    recommendationJob = null
                }
            }
        }
    }

    private fun applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
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

    private fun refreshUI(email: String) {
        loadRecommendations(email)
        supportActionBar?.title = getString(R.string.recommend)
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.menu?.apply {
            findItem(R.id.navigation_home)?.title = getString(R.string.home)
            findItem(R.id.navigation_saved)?.title = getString(R.string.saved)
            findItem(R.id.navigation_recommend)?.title = getString(R.string.recommend)
            findItem(R.id.navigation_profile)?.title = getString(R.string.profile)
        }
        Log.d("RecommendActivity", "UI refreshed with language: ${sharedPref.getString("app_language", "ru")}")
    }

    private fun showCustomDialog(title: String, message: String) {
        val dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.custom_dialog_error, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}