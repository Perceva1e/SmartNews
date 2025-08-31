package com.example.smartnews.api

import com.example.smartnews.api.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
        @Query("category") category: String? = null
    ): NewsResponse
}