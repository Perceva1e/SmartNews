package com.example.diplom.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import android.util.Log

object NewsApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer 4c0c19e6cad4422a8a177baf5a64ded3")
                        .build()
                    Log.d("NETWORK", "Request: ${request.url}")
                    chain.proceed(request)
                }
                .build()
        )
        .build()

    val service: NewsApiService = retrofit.create(NewsApiService::class.java)
}