package com.example.smartnews.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.bd.SavedNews
import com.example.smartnews.view.SavedNewsViewHolder

class SavedNewsAdapter(
    private val userId: Int,
    private val listener: OnNewsDeletedListener? = null
) : RecyclerView.Adapter<SavedNewsViewHolder>() {

    private var newsList: List<SavedNews> = emptyList()

    fun setSavedNews(news: List<SavedNews>) {
        newsList = news
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedNewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.saved_news_item, parent, false)
        return SavedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedNewsViewHolder, position: Int) {
        holder.bind(userId, newsList[position], listener)
    }

    override fun getItemCount(): Int = newsList.size

    interface OnNewsDeletedListener {
        fun onNewsDeleted()
    }
}