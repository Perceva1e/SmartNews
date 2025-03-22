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

class NewsRepository(
    private val userDao: UserDao,
    private val newsDao: NewsDao,
    private val newsApi: NewsApiService
) {
    // User operations
    suspend fun registerUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insert(user)
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)?.takeIf { it.password == password }
    }

    suspend fun isEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email) != null
    }

    // News operations
    suspend fun getTopNews(): List<News> = withContext(Dispatchers.IO) {
        try {
            val response = newsApi.getTopHeadlines()
            Log.d("API", "Received ${response.articles.size} news items")
            response.articles
        } catch (e: Exception) {
            Log.e("API", "Error fetching news: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveNews(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.saveNews(news)
    }

    suspend fun getSavedNews(userId: Int): List<SavedNews> =
        newsDao.getSavedNewsByUser(userId)

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
}