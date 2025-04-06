package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.auth.LoginActivity
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityMainBinding
import com.example.diplom.news.adapter.NewsAdapter
import com.example.diplom.news.adapter.NewsViewModel
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.AppEvents
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: NewsViewModel by viewModels {
        NewsViewModelFactory(
            NewsRepository(
                AppDatabase.getDatabase(this).userDao(),
                AppDatabase.getDatabase(this).newsDao(),
                NewsApi.service
            )
        )
    }

    private lateinit var adapter: NewsAdapter
    private var userId: Int = -1
    private lateinit var adView: AdView
    private val adRefreshHandler = Handler(Looper.getMainLooper())
    private var lastRefreshTime = 0L
    private var adClosedTime = 0L
    private var isAdManuallyClosed = false
    private val adReshowDelay = 5 * 60 * 1000L

    private val adRefreshRunnable = object : Runnable {
        override fun run() {
            Log.d("AdRefresh", "Refreshing ad")
            adView.loadAd(AdRequest.Builder().build())
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(this, adReshowDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAuthenticationStatus()
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

        setupRecyclerView()
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        observeNews()
        setupNavigation()

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
                Log.d("AdListener", "Ad loaded successfully")
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

    override fun onPause() {
        super.onPause()
        adView.pause()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    private fun startMainActivity() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        viewModel.loadNews()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
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

    private fun updateNavigationSelection() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter(userId) { news ->
            viewModel.saveNews(userId, news)
            AppEvents.notifyNewsChanged(userId, "SAVE")
            showToast(getString(R.string.saved_news))
        }

        binding.rvNews.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun observeNews() {
        viewModel.news.observe(this) { news ->
            if (news.isNotEmpty()) {
                binding.emptyView.visibility = View.GONE
                binding.rvNews.visibility = View.VISIBLE
                adapter.submitList(news) {
                    binding.rvNews.post {
                        binding.rvNews.invalidateItemDecorations()
                    }
                }
            } else {
                binding.emptyView.visibility = View.VISIBLE
                binding.rvNews.visibility = View.GONE
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_saved -> {
                    startActivity(
                        Intent(this, SavedNewsActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }

                R.id.navigation_recommend -> {
                    startActivity(
                        Intent(this, RecommendActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
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

    private fun checkAuthenticationStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            lifecycleScope.launch {
                viewModel.getUserByEmail(currentUser.email!!).collect { user ->
                    user?.let {
                        userId = it.id
                        startMainActivity()
                    } ?: run {
                        redirectToLogin()
                    }
                }
            }
        } else {
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}