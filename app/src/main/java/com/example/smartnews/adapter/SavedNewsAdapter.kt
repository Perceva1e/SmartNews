package com.example.smartnews.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.api.model.News
import com.example.smartnews.bd.SavedNews
import com.example.smartnews.view.NewsViewHolder

class SavedNewsAdapter : RecyclerView.Adapter<NewsViewHolder>() {

    private var newsList: List<SavedNews> = emptyList()

    fun setSavedNews(news: List<SavedNews>) {
        newsList = news
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val savedNews = newsList[position]
        val news = News(
            title = savedNews.title,
            description = savedNews.description,
            url = savedNews.url,
            urlToImage = savedNews.urlToImage,
            publishedAt = savedNews.publishedAt,
            content = null,
            category = savedNews.category
        )
        holder.bind(news, 0, savedNews.category)
    }

    override fun getItemCount(): Int = newsList.size
}