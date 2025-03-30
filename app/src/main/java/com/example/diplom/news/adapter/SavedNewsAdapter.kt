package com.example.diplom.news.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.diplom.R
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.databinding.ItemSavedNewsBinding
import com.example.diplom.utils.AppEvents

class SavedNewsAdapter(
    private val userId: Int,
    private val onDeleteClick: (SavedNews) -> Unit
) : ListAdapter<SavedNews, SavedNewsAdapter.SavedNewsViewHolder>(SavedNewsDiffCallback()) {

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
            with(binding) {
                binding.tvTitle.text = news.title
                binding.tvContent.text = news.content
                tvMood.apply {
                    text = when (news.mood) {
                        SavedNews.MOOD_HAPPY -> "😊"
                        SavedNews.MOOD_SAD -> "😢"
                        else -> "😐"
                    }
                }
                Glide.with(binding.root)
                    .load(news.imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(binding.ivSavedNewsImage)

                binding.ibDelete.setOnClickListener {
                    onDeleteClick(news)
                    AppEvents.notifyNewsChanged(userId, "DELETE")
                }
            }
        }
    }

    class SavedNewsDiffCallback : DiffUtil.ItemCallback<SavedNews>() {
        override fun areItemsTheSame(oldItem: SavedNews, newItem: SavedNews): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedNews, newItem: SavedNews): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.content == newItem.content &&
                    oldItem.mood == newItem.mood
        }
    }
}