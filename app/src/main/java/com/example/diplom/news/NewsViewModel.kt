package com.example.diplom.news

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diplom.api.model.News
import com.example.diplom.database.entity.SavedNews
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.ResultState
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

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
    private fun loadNews() {
        viewModelScope.launch {
            Log.d("VIEWMODEL", "Starting news load")
            try {
                val news = repository.getTopNews()
                Log.d("VIEWMODEL", "Loaded ${news.size} items")
                _news.postValue(news)
            } catch (e: Exception) {
                Log.e("VIEWMODEL", "Error: ${e.message}")
            }
        }
    }


    fun saveNews(userId: Int, news: News) {
        viewModelScope.launch {
            val savedNews = SavedNews(
                userId = userId,
                title = news.title,
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
}