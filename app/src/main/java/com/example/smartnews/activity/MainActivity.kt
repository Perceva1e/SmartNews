package com.example.smartnews.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartnews.R
import com.example.smartnews.api.NewsApi
import com.example.smartnews.api.model.News
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        newsAdapter = NewsAdapter()
        recyclerView.adapter = newsAdapter

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadNews()
                    true
                }
                R.id.navigation_saved -> {
                    // Логика для сохраненных новостей
                    true
                }
                R.id.navigation_recommend -> {
                    // Логика для рекомендаций
                    true
                }
                R.id.navigation_profile -> {
                    // Логика для профиля
                    true
                }
                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.navigation_home
        loadNews()
    }

    private fun loadNews() {
        if (!isInternetAvailable()) {
            runOnUiThread {
                android.widget.Toast.makeText(this, "Нет интернет-соединения", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NewsApi.service.getTopHeadlines()
                Log.d(TAG, "Response: $response")
                val newsList = response.articles
                withContext(Dispatchers.Main) {
                    if (newsList.isNotEmpty()) {
                        newsAdapter.setNews(newsList)
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "Нет доступных новостей", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error loading news", e)
                    android.widget.Toast.makeText(this@MainActivity, "Ошибка загрузки новостей: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var newsList: List<News> = emptyList()

    fun setNews(news: List<News>) {
        newsList = news
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NewsViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount(): Int = newsList.size

    class NewsViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
        private val tvDescription = itemView.findViewById<android.widget.TextView>(R.id.tvDescription)
        private val tvDate = itemView.findViewById<android.widget.TextView>(R.id.tvDate)
        private val tvSource = itemView.findViewById<android.widget.TextView>(R.id.tvSource)
        private val tvMood = itemView.findViewById<android.widget.TextView>(R.id.tvMood)
        private val ivNewsImage = itemView.findViewById<android.widget.ImageView>(R.id.ivNewsImage)

        fun bind(news: News) {
            tvTitle.text = news.title ?: "No title"
            tvDescription.text = news.description ?: "No description"
            tvDate.text = news.publishedAt?.substring(0, 10) ?: "No date"
            tvSource.text = news.url?.substringAfter("://")?.substringBefore("/") ?: "Unknown source"
            tvSource.setOnClickListener {
                news.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    itemView.context.startActivity(intent)
                }
            }
            tvMood.text = "Mood: ${news.analyzeMood()}"
            if (news.urlToImage != null) {
                Glide.with(itemView.context)
                    .load(news.urlToImage)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(ivNewsImage)
            } else {
                ivNewsImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}