package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.auth.LoginActivity
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityMainBinding
import com.example.diplom.news.adapter.NewsAdapter
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.AppEvents
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAuthenticationStatus()
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        setupRecyclerView()
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        observeNews()
        setupNavigation()
        updateNavigationSelection()

    }

    private fun startMainActivity() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        viewModel.loadNews()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun updateNavigationSelection() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter { news ->
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