package com.example.smartnews.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.bd.SavedNews

class SavedNewsAdapter(
    private var newsList: List<SavedNews>,
    private val email: String,
    private val listener: OnNewsDeletedListener?
) : RecyclerView.Adapter<SavedNewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.saved_news_item, parent, false)
        return SavedNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedNewsViewHolder, position: Int) {
        holder.bind(email, newsList[position], listener)
    }

    override fun getItemCount(): Int = newsList.size

    fun setSavedNews(news: List<SavedNews>) {
        newsList = news
        notifyDataSetChanged()
    }

    interface OnNewsDeletedListener {
        fun onNewsDeleted()
    }
}