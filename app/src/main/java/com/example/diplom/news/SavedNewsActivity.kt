package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivitySavedNewsBinding
import com.example.diplom.news.adapter.NewsViewModel
import com.example.diplom.news.adapter.SavedNewsAdapter
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.AppEvents
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SavedNewsActivity : BaseActivity() {
    private var eventsJob: Job? = null
    private lateinit var binding: ActivitySavedNewsBinding
    private val viewModel: NewsViewModel by viewModels {
        NewsViewModelFactory(
            NewsRepository(
                AppDatabase.getDatabase(this).userDao(),
                AppDatabase.getDatabase(this).newsDao(),
                NewsApi.service
            )
        )
    }

    private lateinit var adapter: SavedNewsAdapter
    private var userId: Int = -1
    private lateinit var adView: AdView
    private val adRefreshHandler = Handler(Looper.getMainLooper())
    private var lastRefreshTime = 0L
    private var adClosedTime = 0L
    private var isAdManuallyClosed = false
    private val adReshowDelay = 5 * 60 * 1000L

    private val adRefreshRunnable = object : Runnable {
        override fun run() {
            Log.d("AdRefresh", "Refreshing ad in SavedNewsActivity")
            adView.loadAd(AdRequest.Builder().build())
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(this, adReshowDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        adView = findViewById(R.id.adView)
        setupAdListener()
        val adRequest = AdRequest.Builder().build()
        if (!isSubscribed()) {
            adView.loadAd(adRequest)
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
        }

        binding.bottomNavigation.selectedItemId = R.id.navigation_saved
        setupNavigation()
        setupRecyclerView()
        loadSavedNews()

        binding.btnCloseAd.setOnClickListener {
            adView.visibility = View.GONE
            binding.btnCloseAd.visibility = View.GONE
            adRefreshHandler.removeCallbacks(adRefreshRunnable)
            isAdManuallyClosed = true
            adClosedTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed({
                if (isAdManuallyClosed) {
                    reloadAd()
                }
            }, adReshowDelay)
        }
    }

    private fun setupAdListener() {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                if (!isSubscribed()) {
                    binding.adView.visibility = View.VISIBLE
                    binding.btnCloseAd.visibility = View.VISIBLE
                }
                isAdManuallyClosed = false
                Log.d("AdListener", "Ad loaded successfully in SavedNewsActivity")
            }

            override fun onAdClosed() {
                super.onAdClosed()
                binding.adView.visibility = View.GONE
                binding.btnCloseAd.visibility = View.GONE
            }
        }
    }

    private fun reloadAd() {
        if (!isSubscribed()) {
            binding.adView.visibility = View.VISIBLE
            adView.loadAd(AdRequest.Builder().build())
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
        }
        isAdManuallyClosed = false
    }

    private fun isSubscribed(): Boolean {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean("isSubscribed_$userId", false)
    }

    override fun onStart() {
        super.onStart()
        eventsJob = CoroutineScope(Dispatchers.Main).launch {
            AppEvents.newsUpdates.collect {
                loadSavedNews()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_saved
        adView.resume()
        val currentTime = System.currentTimeMillis()

        if (!isSubscribed()) {
            if (isAdManuallyClosed && currentTime - adClosedTime >= adReshowDelay) {
                reloadAd()
            } else if (!isAdManuallyClosed && currentTime - lastRefreshTime > adReshowDelay && binding.adView.visibility == View.VISIBLE) {
                adView.loadAd(AdRequest.Builder().build())
                lastRefreshTime = currentTime
                adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
            } else if (binding.adView.visibility == View.VISIBLE) {
                adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    override fun onStop() {
        super.onStop()
        eventsJob?.cancel()
        eventsJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    private fun setupRecyclerView() {
        adapter = SavedNewsAdapter(userId) { newsToDelete ->
            viewModel.deleteNews(userId, newsToDelete)
            AppEvents.notifyNewsChanged(userId, "DELETE")
        }

        binding.rvSavedNews.apply {
            layoutManager = LinearLayoutManager(this@SavedNewsActivity)
            adapter = this@SavedNewsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadSavedNews() {
        viewModel.getSavedNews(userId).observe(this) { savedNews ->
            adapter.submitList(savedNews)
            binding.emptyView.visibility = if (savedNews.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

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
                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    applyTransition()
                    true
                }

                R.id.navigation_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }

                else -> false
            }
        }
    }

    private fun applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}