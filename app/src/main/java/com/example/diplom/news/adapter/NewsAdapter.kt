package com.example.diplom.news.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.diplom.R
import com.example.diplom.databinding.ItemNewsBinding
import com.example.diplom.api.model.News

class NewsAdapter(
    private val onSaveClick: (News) -> Unit
) : ListAdapter<News, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(private val binding: ItemNewsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(news: News) {
            Log.d("ADAPTER", "Binding news: ${news.title}")
            binding.tvTitle.text = news.title ?: "No title"
            binding.tvDescription.text = news.description ?: "No description"
            binding.btnSave.setOnClickListener { onSaveClick(news) }
            Glide.with(binding.root.context)
                .load(news.urlToImage)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivNewsImage)

            binding.ivNewsImage.visibility =
                if (news.urlToImage.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News) =
            oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: News, newItem: News) =
            oldItem == newItem
    }
}