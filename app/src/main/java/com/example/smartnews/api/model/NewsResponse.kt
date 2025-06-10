package com.example.smartnews.api.model

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<News>
)