package com.example.diplom.news.adapter

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diplom.api.model.News
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.database.entity.User
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.ResultState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val recommendationEngine = RecommendationEngine()
    private val _recommendations = MutableLiveData<List<News>>()
    val recommendations: LiveData<List<News>> = _recommendations

    private val _newsState = MutableLiveData<ResultState<List<News>>>()

    private val _savedNews = MutableLiveData<List<SavedNews>>()
    val savedNews: LiveData<List<SavedNews>> = _savedNews

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _news = MutableLiveData<List<News>>()
    val news: LiveData<List<News>> = _news

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _loading = MutableLiveData<Boolean>()

    private val _navigationEvent = MutableLiveData<Event<NavigationEvent>>()

    sealed class NavigationEvent {
        object Logout : NavigationEvent()
    }

    class Event<T>(private val content: T) {
        var hasBeenHandled = false
            private set

        fun getContentIfNotHandled(): T? {
            return if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }
        }
    }

    fun loadNews(userId: Int) {
        viewModelScope.launch {
            _newsState.postValue(ResultState.Loading)
            try {
                val newsList = repository.getTopNewsForUser(userId)
                if (newsList.isEmpty()) {
                    _newsState.postValue(ResultState.Error("No news found"))
                } else {
                    _newsState.postValue(ResultState.Success(newsList))
                    _news.postValue(newsList)
                }
            } catch (e: Exception) {
                _newsState.postValue(ResultState.Error("Error: ${e.message}"))
            }
        }
    }

    fun saveNews(userId: Int, news: News) {
        viewModelScope.launch {
            try {
                val savedNews = SavedNews(
                    userId = userId,
                    title = news.title ?: "No title",
                    content = news.description ?: "",
                    url = news.url,
                    imageUrl = news.urlToImage,
                    mood = news.analyzeMood()
                )
                repository.saveNewsForUser(savedNews)

                loadSavedNews(userId)

            } catch (e: Exception) {
                _error.postValue("Save failed: ${e.message}")
            }
        }
    }

    fun getSavedNews(userId: Int): LiveData<List<SavedNews>> {
        loadSavedNews(userId)
        return savedNews
    }

    fun loadRecommendations(userId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _loading.postValue(true)

                recommendationEngine.getCachedRecommendations()?.let {
                    _recommendations.postValue(it)
                    return@launch
                }

                val savedNews = withContext(Dispatchers.IO) {
                    repository.getSavedNews(userId)
                }

                if (savedNews.size < 3) {
                    _error.postValue("Save at least 3 news to get recommendations")
                    return@launch
                }

                val keywords = recommendationEngine.extractKeywords(savedNews)
                Log.d("Recommendations", "Keywords: $keywords")

                val query = if (keywords.isNotEmpty()) {
                    keywords.joinToString(" OR ") { "\"$it\"" }
                } else {
                    "news OR update OR world"
                }

                val relevantNews = withContext(Dispatchers.IO) {
                    repository.searchRecommendedNews(query)
                }

                val recommended = recommendationEngine.recommendNews(relevantNews, keywords)
                recommendationEngine.updateCache(recommended)

                _recommendations.postValue(recommended)
            } catch (e: Exception) {
                Log.e("Recommendations", "Error: ${e.stackTraceToString()}")
                _error.postValue("Failed to load recommendations. Showing popular news.")
                loadFallbackNews()
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private suspend fun loadFallbackNews() {
        try {
            val fallbackNews = withContext(Dispatchers.IO) {
                repository.getTopNews()
            }
            _recommendations.postValue(fallbackNews.shuffled().take(10))
        } catch (_: Exception) {
            _error.postValue("No recommendations available")
        }
    }

    fun deleteNews(userId: Int, news: SavedNews) {
        viewModelScope.launch {
            try {
                repository.deleteSavedNews(news)
                loadSavedNews(userId)
            } catch (e: Exception) {
                _error.postValue("Delete failed: ${e.message}")
            }
        }
    }

    private fun loadSavedNews(userId: Int) {
        viewModelScope.launch {
            _savedNews.postValue(repository.getSavedNews(userId))
        }
    }

    fun updateUser(userId: Int, name: String, email: String) {
        viewModelScope.launch {
            repository.updateUser(userId, name, email)
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().currentUser?.delete()
                repository.deleteUser(userId)
                _navigationEvent.postValue(Event(NavigationEvent.Logout))
            } catch (e: Exception) {
                _error.postValue("Deletion error: ${e.message}")
            }
        }
    }

    fun loadUser(userId: Int) {
        viewModelScope.launch {
            repository.getUser(userId).collect { user ->
                _user.value = user
            }
        }
    }

    fun getUserByEmail(email: String): Flow<User?> {
        return repository.getUserByEmail(email)
    }

    fun updateLocalPassword(email: String, newHashedPassword: String) {
        viewModelScope.launch {
            try {
                repository.updateUserPassword(email, newHashedPassword)
            } catch (e: Exception) {
                _error.postValue("Password update error: ${e.message}")
            }
        }
    }

    fun updateUserCategories(userId: Int, selectedCategories: String?) {
        viewModelScope.launch {
            repository.updateUserCategories(userId, selectedCategories)
            loadUser(userId)
        }
    }
}