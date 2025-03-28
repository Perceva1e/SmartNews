package com.example.diplom.api.model

import com.google.gson.annotations.SerializedName

data class News(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    val url: String,
    val publishedAt: String,
    val content: String?
)