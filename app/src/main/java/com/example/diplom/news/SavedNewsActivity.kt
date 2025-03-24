package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.R
import com.example.diplom.databinding.ActivitySavedNewsBinding
import com.example.diplom.repository.NewsRepository
import com.example.diplom.database.AppDatabase
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.example.diplom.api.NewsApi
import com.example.diplom.news.adapter.SavedNewsAdapter
import android.view.View

class SavedNewsActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()
        binding.bottomNavigation.selectedItemId = R.id.navigation_saved
        setupNavigation()
        setupRecyclerView()
        loadSavedNews()
    }


    private fun setupRecyclerView() {
        adapter = SavedNewsAdapter { newsToDelete ->
            viewModel.deleteNews(userId, newsToDelete)
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

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_saved
    }
    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                    true
                }

                R.id.navigation_saved -> true

                R.id.navigation_recommend -> {
                    startActivity(Intent(this, RecommendActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
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