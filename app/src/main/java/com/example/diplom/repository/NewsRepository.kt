package com.example.diplom.repository

import android.util.Log
import com.example.diplom.api.NewsApiService
import com.example.diplom.api.model.News
import com.example.diplom.database.dao.NewsDao
import com.example.diplom.database.dao.UserDao
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.database.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException

class NewsRepository(
    private val userDao: UserDao,
    private val newsDao: NewsDao,
    private val newsApi: NewsApiService
) {
    suspend fun registerUser(user: User): Long = withContext(Dispatchers.IO) {
        userDao.insert(user)
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)?.takeIf { it.password == password }
    }

    suspend fun isEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email) != null
    }

    suspend fun getTopNews(): List<News> = withContext(Dispatchers.IO) {
        try {
            val response = newsApi.getTopHeadlines()
            Log.d("API_RESPONSE", "Received ${response.articles.size} items")
            response.articles.forEach {
                Log.d("NEWS_ITEM", "Title: ${it.title ?: "Untitled"}, URL: ${it.url}")
            }
            response.articles
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    suspend fun saveNews(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.saveNews(news)
    }

    suspend fun getSavedNews(userId: Int): List<SavedNews> = withContext(Dispatchers.IO) {
        newsDao.getSavedNewsByUser(userId).also {
            Log.d("NewsRepository", "Saved news for user $userId: ${it.size}")
        }
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    suspend fun getNewsFromApi(): List<News> = withContext(Dispatchers.IO) {
        try {
            newsApi.getTopHeadlines().articles
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNewsForUser(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.saveNews(news)
    }

    suspend fun getAllNewsFromApi(): List<News> = withContext(Dispatchers.IO) {
        try {
            val response = newsApi.getTopHeadlines(pageSize = 50)
            Log.d("NewsRepository", "Received ${response.articles.size} news")
            response.articles
        } catch (e: Exception) {
            Log.e("NewsRepository", "API Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRecommendedNews(query: String): List<News> {
        return try {
            newsApi.searchRecommendedNews(query).articles
        } catch (e: Exception) {
            emptyList()
        }
    }

}