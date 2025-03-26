package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityRecommendBinding
import com.example.diplom.news.adapter.NewsAdapter
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory

class RecommendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecommendBinding
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
        binding = ActivityRecommendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1).takeIf { it != -1 } ?: run {
            finish()
            return
        }

        setupRecyclerView()
        setupNavigation()
        setupObservers()
        viewModel.loadRecommendations(userId)
    }


    private fun setupRecyclerView() {
        adapter = NewsAdapter { news ->
            viewModel.saveNews(userId, news)
            showToast(getString(R.string.saved_news))
        }

        binding.rvRecommend.apply {
            layoutManager = LinearLayoutManager(this@RecommendActivity)
            adapter = this@RecommendActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.recommendations.observe(this) { news ->
            if (news.isNullOrEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.rvRecommend.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.rvRecommend.visibility = View.VISIBLE
                adapter.submitList(news)
            }
        }

        viewModel.error.observe(this) { error ->
            if (error.isNotBlank()) {
                binding.emptyView.text = error
                binding.emptyView.visibility = View.VISIBLE
                binding.rvRecommend.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_recommend
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
                R.id.navigation_saved -> {
                    startActivity(Intent(this, SavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_recommend -> true
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