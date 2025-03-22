package com.example.diplom.api

import com.example.diplom.api.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("pageSize") pageSize: Int = 100,
        @Query("category") category: String? = null
    ): NewsResponse

    @GET("everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("sortBy") sortBy: String = "publishedAt"
    ): NewsResponse
}