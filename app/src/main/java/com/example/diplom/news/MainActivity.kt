package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.databinding.ActivityMainBinding
import com.example.diplom.repository.NewsRepository
import com.example.diplom.database.AppDatabase
import com.example.diplom.api.NewsApi
import com.example.diplom.news.adapter.NewsAdapter
import com.example.diplom.viewmodel.NewsViewModelFactory
import android.view.View

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

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        setupRecyclerView()
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        observeNews()
        setupNavigation()
        updateNavigationSelection()

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
            when (item.itemId) {
                R.id.navigation_home -> true

                R.id.navigation_saved -> {
                    startActivity(
                        Intent(this, SavedNewsActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    )
                    applyTransition()
                    true
                }

                R.id.navigation_recommend -> {
                    startActivity(
                        Intent(this, RecommendActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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