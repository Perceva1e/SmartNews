package com.example.diplom.news.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.diplom.R
import com.example.diplom.api.model.News
import com.example.diplom.databinding.ItemNewsBinding

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
            with(binding) {
                Log.d("ADAPTER", "Binding news: ${news.title}")
                tvMood.apply {
                    text = when(news.analyzeMood()) {
                        News.MOOD_HAPPY -> context.getString(R.string.mood_happy)
                        News.MOOD_SAD -> context.getString(R.string.mood_sad)
                        else -> context.getString(R.string.mood_neutral)
                    }
                }
                tvTitle.text = news.title ?: root.context.getString(R.string.no_title)
                tvDescription.text = news.description ?: root.context.getString(R.string.no_description)

                news.urlToImage?.let { url ->
                    Glide.with(root)
                        .load(url)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("GLIDE", "Image load failed: $url", e)
                                ivNewsImage.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                ivNewsImage.visibility = View.VISIBLE
                                return false
                            }
                        })
                        .into(ivNewsImage)
                } ?: run {
                    ivNewsImage.visibility = View.GONE
                }

                btnSave.setOnClickListener { onSaveClick(news) }
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }

}