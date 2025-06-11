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
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.smartnews.R

object NewsLoader {
    private const val TAG = "NewsLoader"

    fun loadNews(context: Context, adapter: RecyclerView.Adapter<*>) {
        if (!isInternetAvailable(context)) {
            showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_no_internet_desc), R.layout.custom_dialog_error)
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
                        showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_no_news_desc), R.layout.custom_dialog_error)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error loading news", e)
                    showCustomDialog(context, getString(context, R.string.error_title), getString(context, R.string.error_loading_news_desc), R.layout.custom_dialog_error)
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

    private fun getString(context: Context, resId: Int): String {
        return context.getString(resId)
    }

    private fun showCustomDialog(context: Context, title: String, message: String, layoutResId: Int) {
        val dialogView = LayoutInflater.from(context).inflate(layoutResId, null)
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