package com.example.diplom.repository

import android.util.Log
import com.example.diplom.api.NewsApiService
import com.example.diplom.api.model.News
import com.example.diplom.database.dao.NewsDao
import com.example.diplom.database.dao.UserDao
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.database.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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

    fun getUserByEmail(email: String): Flow<User?> {
        return userDao.getUserByEmailFlow(email)
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

    suspend fun deleteSavedNews(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.deleteNews(news.id)
    }

    suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun updateUser(userId: Int, name: String, email: String) = withContext(Dispatchers.IO) {
        userDao.updateUser(userId, name, email)
    }

    suspend fun deleteUser(userId: Int) {
        withContext(Dispatchers.IO) {
            userDao.deleteUser(userId)
            newsDao.deleteAllUserNews(userId)
        }
    }

    fun getUser(userId: Int): Flow<User> = userDao.getUser(userId)

    fun getUserByEmailFlow(email: String): Flow<User?> {
        return userDao.getUserByEmailFlow(email)
    }

    suspend fun checkUserExists(email: String): Boolean {
        return userDao.checkUserExists(email)
    }

    suspend fun updateUserPassword(email: String, newHashedPassword: String) {
        try {
            val rowsUpdated = userDao.updatePassword(email, newHashedPassword)
            if (rowsUpdated == 0) throw Exception("User not found")
        } catch (e: Exception) {
            throw Exception("Database error: ${e.message}")
        }
    }
}