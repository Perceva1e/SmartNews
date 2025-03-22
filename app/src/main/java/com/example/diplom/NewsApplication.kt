package com.diplom.smartnews

import android.app.Application
import com.example.diplom.database.AppDatabase
import com.example.diplom.api.NewsApi
import com.example.diplom.api.NewsApiService

class NewsApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val newsApi: NewsApiService by lazy { NewsApi.service }
}