package com.example.smartnews.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NewsApi {
    private const val BASE_URL = "https://newsapi.org/v2/"
    private const val API_KEY = "4c0c19e6cad4422a8a177baf5a64ded3"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.newBuilder()
                .addQueryParameter("apiKey", API_KEY)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(url)
                .header("X-Api-Key", API_KEY)
                .build()
            chain.proceed(newRequest)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: NewsApiService by lazy {
        retrofit.create(NewsApiService::class.java)
    }
}