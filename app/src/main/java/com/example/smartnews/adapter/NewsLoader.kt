package com.example.smartnews.adapter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.api.NewsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NewsLoader {
    private const val TAG = "NewsLoader"

    fun loadNews(context: Context, adapter: RecyclerView.Adapter<*>) {
        if (!isInternetAvailable(context)) {
            runOnUiThread(context) {
                android.widget.Toast.makeText(context, "Нет интернет-соединения", android.widget.Toast.LENGTH_SHORT).show()
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
                        (adapter as NewsAdapter).setNews(newsList)
                    } else {
                        android.widget.Toast.makeText(context, "Нет доступных новостей", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error loading news", e)
                    android.widget.Toast.makeText(context, "Ошибка загрузки новостей: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun runOnUiThread(context: Context, action: () -> Unit) {
        if (context is AppCompatActivity) {
            context.runOnUiThread(action)
        }
    }
}