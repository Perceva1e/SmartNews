package com.example.diplom.news

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diplom.api.model.News
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.news.adapter.RecommendationEngine
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.ResultState
import kotlinx.coroutines.launch
import java.io.IOException

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val recommendationEngine = RecommendationEngine()
    private val _recommendations = MutableLiveData<List<News>>()
    val recommendations: LiveData<List<News>> = _recommendations

    private val _newsState = MutableLiveData<ResultState<List<News>>>()
    val newsState: LiveData<ResultState<List<News>>> = _newsState

    private val _savedNews = MutableLiveData<List<SavedNews>>()
    val savedNews: LiveData<List<SavedNews>> = _savedNews

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _news = MutableLiveData<List<News>>()
    val news: LiveData<List<News>> = _news

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _newsState.postValue(ResultState.Loading)
            try {
                val news = repository.getTopNews()
                if (news.isEmpty()) {
                    _newsState.postValue(ResultState.Error("No news found"))
                } else {
                    _newsState.postValue(ResultState.Success(news))
                    _news.postValue(news)
                }
            } catch (e: IOException) {
                _newsState.postValue(ResultState.Error("Check internet connection"))
            } catch (e: Exception) {
                _newsState.postValue(ResultState.Error("Error: ${e.message}"))
            }
        }
    }

    fun saveNews(userId: Int, news: News) {
        viewModelScope.launch {
            val savedNews = SavedNews(
                userId = userId,
                title = news.title.toString(),
                content = news.description ?: "",
                url = news.url,
                imageUrl = news.urlToImage
            )
            repository.saveNewsForUser(savedNews)
        }
    }

    fun getSavedNews(userId: Int): LiveData<List<SavedNews>> {
        loadSavedNews(userId)
        return savedNews
    }

    private fun loadSavedNews(userId: Int) {
        viewModelScope.launch {
            _savedNews.value = repository.getSavedNews(userId)
        }
    }

    fun clearError() {
        _error.value = ""
    }

    fun loadRecommendations(userId: Int) {
        viewModelScope.launch {
            try {
                Log.d("Recommendations", "=== STARTING RECOMMENDATION PROCESS ===")

                val savedNews = repository.getSavedNews(userId).also {
                    Log.d("Recommendations", "Saved news: ${it.size}")
                }

                if (savedNews.size < 3) {
                    _error.postValue("Save at least 3 news to get recommendations")
                    return@launch
                }

                val keywords = recommendationEngine.extractKeywords(savedNews).also {
                    Log.d("Recommendations", "Keywords: $it")
                }

                if (keywords.isEmpty()) {
                    Log.w("Recommendations", "Fallback to popular news")
                    showFallbackRecommendations()
                    _error.postValue("Using popular news as recommendations")
                    return@launch
                }

                val query = keywords.joinToString(" OR ") { "\"$it\"" }
                    .takeIf { it.isNotBlank() }
                    ?: run {
                        Log.w("Recommendations", "Empty query generated")
                        showFallbackRecommendations()
                        return@launch
                    }

                val relevantNews = repository.searchRecommendedNews(query).also {
                    Log.d("Recommendations", "Relevant news: ${it.size}")
                }

                if (relevantNews.isEmpty()) {
                    _error.postValue("No relevant news found")
                    return@launch
                }

                recommendationEngine.updateUserProfile(savedNews)
                val recommended = recommendationEngine.recommendNews(relevantNews)

                val savedUrls = savedNews.map { it.url.trim().lowercase() }.toSet()
                val filtered = recommended.filterNot {
                    savedUrls.contains(it.url.trim().lowercase())
                }

                _recommendations.postValue(filtered.take(10))

            } catch (e: Exception) {
                Log.e("Recommendations", "Error: ${e.stackTraceToString()}")
                _error.postValue("Failed to load recommendations")
            }
        }
    }

    private fun showFallbackRecommendations() {
        _news.value?.let { currentNews ->
            _recommendations.postValue(currentNews.shuffled().take(10))
            _error.postValue("Showing popular news instead")
        } ?: run {
            _error.postValue("No recommendations available")
        }
    }
}