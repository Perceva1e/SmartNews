package com.example.diplom.news.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.diplom.R
import com.example.diplom.databinding.ItemSavedNewsBinding
import com.example.diplom.database.entity.SavedNews

class SavedNewsAdapter : ListAdapter<SavedNews, SavedNewsAdapter.SavedNewsViewHolder>(
    SavedNewsDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedNewsViewHolder {
        val binding = ItemSavedNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedNewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedNewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedNewsViewHolder(private val binding: ItemSavedNewsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(news: SavedNews) {
            binding.tvTitle.text = news.title
            binding.tvContent.text = news.content

            Glide.with(binding.root)
                .load(news.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(binding.ivSavedNewsImage)
        }
    }

    class SavedNewsDiffCallback : DiffUtil.ItemCallback<SavedNews>() {
        override fun areItemsTheSame(oldItem: SavedNews, newItem: SavedNews) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SavedNews, newItem: SavedNews) =
            oldItem == newItem
    }
}