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

    suspend fun getSavedNews(userId: Int): List<SavedNews> = withContext(Dispatchers.IO) {
        newsDao.getSavedNewsByUser(userId).also {
            Log.d("NewsRepository", "Saved news for user $userId: ${it.size}")
        }
    }

    fun getUserByEmail(email: String): Flow<User?> {
        return userDao.getUserByEmailFlow(email)
    }

    suspend fun saveNewsForUser(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.saveNews(news)
    }

    suspend fun searchRecommendedNews(query: String): List<News> {
        return try {
            newsApi.searchRecommendedNews(query).articles
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteSavedNews(news: SavedNews) = withContext(Dispatchers.IO) {
        newsDao.deleteNews(news.id)
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

    suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun updateUserCategories(userId: Int, selectedCategories: String?) =
        withContext(Dispatchers.IO) {
            userDao.updateUserCategories(userId, selectedCategories)
        }

    suspend fun getTopNewsForUser(userId: Int): List<News> = withContext(Dispatchers.IO) {
        val user = getUserById(userId)
        val categories =
            user?.selectedCategories?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        if (categories.isEmpty()) {
            newsApi.getTopHeadlines().articles
        } else {
            categories.flatMap { category ->
                newsApi.getTopHeadlines(category = category, pageSize = 20).articles
            }.distinctBy { it.url }
        }
    }
}