package com.example.smartnews.view

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartnews.R
import com.example.smartnews.api.model.News
import com.example.smartnews.bd.DatabaseHelper
import com.example.smartnews.bd.SavedNews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    private val tvSource: TextView = itemView.findViewById(R.id.tvSource)
    private val tvMood: TextView = itemView.findViewById(R.id.tvMood)
    private val ivNewsImage: ImageView = itemView.findViewById(R.id.ivNewsImage)
    private val ivShare: ImageView = itemView.findViewById(R.id.ivShare)
    private val ivSave: ImageView = itemView.findViewById(R.id.ivSave)

    fun bind(news: News, userId: Int, category: String) {
        tvTitle.text = news.title ?: "No title"
        tvDescription.text = news.description ?: "No description"
        tvDate.text = news.publishedAt?.substring(0, 10) ?: "No date"
        val sourceText = news.url?.substringAfter("://")?.substringBefore("/") ?: "Unknown source"
        tvSource.text = sourceText
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

        ivShare.setOnClickListener {
            news.url?.let { url ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, news.title ?: "News Article")
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                itemView.context.startActivity(Intent.createChooser(shareIntent, "Share news via"))
            }
        }

        ivSave.setOnClickListener {
            val savedNews = SavedNews(
                title = news.title,
                description = news.description,
                url = news.url,
                urlToImage = news.urlToImage,
                publishedAt = news.publishedAt,
                category = category
            )
            CoroutineScope(Dispatchers.IO).launch {
                val dbHelper = DatabaseHelper(itemView.context)
                val result = dbHelper.saveNews(userId, savedNews)
                if (result != -1L) {
                    withContext(Dispatchers.Main) {
                        showCustomDialog(
                            title = itemView.context.getString(R.string.success_title),
                            message = itemView.context.getString(R.string.news_saved_success),
                            layoutResId = R.layout.custom_dialog_success
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showCustomDialog(
                            title = itemView.context.getString(R.string.error_title),
                            message = itemView.context.getString(R.string.news_save_failed),
                            layoutResId = R.layout.custom_dialog_error
                        )
                    }
                }
            }
        }
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int) {
        val dialogView = LayoutInflater.from(itemView.context).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(itemView.context)
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