package com.example.diplom.news

import android.content.Intent
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

        supportActionBar?.hide()

        setupNavigation()
        setupRecyclerView()
        loadSavedNews()
    }

    private fun setupRecyclerView() {
        adapter = SavedNewsAdapter()
        binding.rvSavedNews.apply {
            layoutManager = LinearLayoutManager(this@SavedNewsActivity)
            adapter = this@SavedNewsActivity.adapter
        }
    }

    private fun loadSavedNews() {
        viewModel.getSavedNews(userId).observe(this) { savedNews ->
            adapter.submitList(savedNews)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                    true
                }
                R.id.navigation_saved -> {
                    true
                }
                R.id.navigation_recommend -> {
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.navigation_saved
    }
}