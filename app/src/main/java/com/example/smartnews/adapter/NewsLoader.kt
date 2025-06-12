package com.example.smartnews.adapter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.smartnews.R
import com.example.smartnews.api.NewsApi
import com.example.smartnews.bd.DatabaseHelper
import kotlinx.coroutines.*
import android.view.LayoutInflater
import com.example.smartnews.api.model.News

object NewsLoader {
    private const val TAG = "NewsLoader"

    fun loadNews(context: Context, adapter: NewsAdapter, userId: Int) {
        if (!isInternetAvailable(context)) {
            showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_no_internet_desc))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbHelper = DatabaseHelper(context)
                val user = dbHelper.getUser()
                val categories = user?.newsCategories?.split(",")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                    ?: listOf("general")

                val newsItems = mutableListOf<News>()
                categories.forEach { category ->
                    val response = NewsApi.service.getTopHeadlines(category = category)
                    newsItems.addAll(response.articles.map { it.copy(category = category) })
                }

                withContext(Dispatchers.Main) {
                    if (newsItems.isNotEmpty()) {
                        adapter.setNews(newsItems.distinctBy { it.title })
                    } else {
                        showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_no_news_desc))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error loading news", e)
                    showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_loading_news_desc))
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

    private fun getString(context: Context, resId: Int): String {
        return context.getString(resId)
    }

    private fun showCustomDialog(context: Context, title: String, message: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_error, null)
        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}