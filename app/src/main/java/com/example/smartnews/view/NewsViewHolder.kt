package com.example.smartnews.view

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartnews.R
import com.example.smartnews.api.model.News

class NewsViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    private val tvSource: TextView = itemView.findViewById(R.id.tvSource)
    private val tvMood: TextView = itemView.findViewById(R.id.tvMood)
    private val ivNewsImage: ImageView = itemView.findViewById(R.id.ivNewsImage)
    private val ivShare: ImageView = itemView.findViewById(R.id.ivShare)

    fun bind(news: News) {
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
    }
}