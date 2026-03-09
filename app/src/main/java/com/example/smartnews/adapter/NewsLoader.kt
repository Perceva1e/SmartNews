package com.example.smartnews.adapter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.smartnews.R
import com.example.smartnews.api.NewsApi
import com.example.smartnews.api.model.News
import com.example.smartnews.bd.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

object NewsLoader {

    private const val TAG = "NewsLoader"

    fun loadNews(context: Context, adapter: NewsAdapter, email: String) {
        if (!isInternetAvailable(context)) {
            showCustomDialog(context, context.getString(R.string.error_title), context.getString(R.string.error_no_internet_desc))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbHelper = DatabaseHelper(context)
                val user = dbHelper.getUserByEmail(email)
                val categories = user?.newsCategories?.split(",")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                    ?: listOf("general")

                val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                var dateFrom = sharedPref.getString("news_date_from", null)
                var dateTo   = sharedPref.getString("news_date_to", null)
                val moodFilter = sharedPref.getString("news_mood", null)

                // Проверка дат — если в будущем, сбрасываем на null
                val today = LocalDate.now()
                if (dateFrom != null) {
                    try {
                        val fromDate = LocalDate.parse(dateFrom)
                        if (fromDate.isAfter(today)) {
                            dateFrom = null
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Дата «От» в будущем — показаны последние новости", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        dateFrom = null
                    }
                }
                if (dateTo != null) {
                    try {
                        val toDate = LocalDate.parse(dateTo)
                        if (toDate.isAfter(today)) {
                            dateTo = null
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Дата «До» в будущем — показаны последние новости", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        dateTo = null
                    }
                }

                val newsItems = mutableListOf<News>()

                categories.forEach { category ->
                    val response = if (dateFrom != null || dateTo != null) {
                        Log.d(TAG, "Запрос everything: q=$category, from=$dateFrom, to=$dateTo")
                        NewsApi.service.getEverything(
                            q = category,
                            from = dateFrom,
                            to = dateTo,
                            language = "ru",
                            pageSize = 100
                        )
                    } else {
                        Log.d(TAG, "Запрос top-headlines: category=$category")
                        NewsApi.service.getTopHeadlines(category = category, pageSize = 100)
                    }
                    newsItems.addAll(response.articles.map { it.copy(category = category) })
                }

                var filteredNews = newsItems.distinctBy { it.title }

                if (moodFilter != null && moodFilter != context.getString(R.string.mood_all)) {
                    filteredNews = filteredNews.filter { it.analyzeMood(context) == moodFilter }
                    Log.d(TAG, "После фильтра по настроению ($moodFilter): ${filteredNews.size} новостей")
                }

                Log.d(TAG, "Итого новостей после фильтров: ${filteredNews.size}")

                withContext(Dispatchers.Main) {
                    if (filteredNews.isNotEmpty()) {
                        adapter.setNews(filteredNews)
                    } else {
                        showCustomDialog(
                            context,
                            context.getString(R.string.error_title),
                            context.getString(R.string.error_no_news_desc)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки новостей", e)
                withContext(Dispatchers.Main) {
                    showCustomDialog(
                        context,
                        context.getString(R.string.error_title),
                        context.getString(R.string.error_loading_news_desc)
                    )
                }
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showCustomDialog(context: Context, title: String, message: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_error, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}