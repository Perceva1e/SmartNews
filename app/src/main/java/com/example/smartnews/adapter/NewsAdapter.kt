package com.example.smartnews.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.api.model.News
import com.example.smartnews.view.NewsViewHolder

class NewsAdapter(
    private val userId: Int = -1
) : RecyclerView.Adapter<NewsViewHolder>() {

    private var newsList: List<News> = emptyList()

    fun setNews(news: List<News>) {
        newsList = news
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position], userId)
    }

    override fun getItemCount(): Int = newsList.size
}