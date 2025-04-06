package com.example.diplom.news

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityCategorySelectionBinding
import com.example.diplom.news.adapter.NewsViewModel
import com.example.diplom.repository.NewsRepository
import com.example.diplom.viewmodel.NewsViewModelFactory

class CategorySelectionActivity : BaseActivity() {
    private lateinit var binding: ActivityCategorySelectionBinding
    private val viewModel: NewsViewModel by viewModels {
        NewsViewModelFactory(
            NewsRepository(
                AppDatabase.getDatabase(this).userDao(),
                AppDatabase.getDatabase(this).newsDao(),
                NewsApi.service
            )
        )
    }
    private var userId: Int = -1
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private val availableCategories =
        listOf("business", "entertainment", "general", "health", "science", "sports", "technology")
    private var selectedCategories: MutableList<String> = mutableListOf()

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("CategorySelectionActivity", "Received language change broadcast")
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategorySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        listView = binding.lvCategories
        val displayCategories = listOf(
            getString(R.string.category_business),
            getString(R.string.category_entertainment),
            getString(R.string.category_general),
            getString(R.string.category_health),
            getString(R.string.category_science),
            getString(R.string.category_sports),
            getString(R.string.category_technology)
        )
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, displayCategories)
        listView.adapter = adapter

        loadSelectedCategories()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveSelectedCategories()
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(languageChangeReceiver, IntentFilter(ACTION_LANGUAGE_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        loadLocale()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangeReceiver)
    }

    private fun loadSelectedCategories() {
        viewModel.user.observe(this) { user ->
            user?.let {
                selectedCategories.clear()
                selectedCategories.addAll(
                    it.selectedCategories?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                )
                updateListView()
            }
        }
        viewModel.loadUser(userId)
    }

    private fun updateListView() {
        for (i in 0 until availableCategories.size) {
            listView.setItemChecked(i, availableCategories[i] in selectedCategories)
        }
    }

    private fun saveSelectedCategories() {
        selectedCategories.clear()
        for (i in 0 until listView.count) {
            if (listView.isItemChecked(i)) {
                selectedCategories.add(availableCategories[i])
            }
        }
        val categoriesString = selectedCategories.joinToString(",")
        viewModel.updateUserCategories(userId, categoriesString)
        finish()
    }

    companion object {
        const val ACTION_LANGUAGE_CHANGED = "com.example.diplom.LANGUAGE_CHANGED"
    }
}