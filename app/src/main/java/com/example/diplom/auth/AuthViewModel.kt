package com.example.diplom.auth

import androidx.lifecycle.ViewModel
import com.example.diplom.database.entity.User
import com.example.diplom.repository.NewsRepository

class AuthViewModel(private val repository: NewsRepository) : ViewModel() {

    suspend fun registerUser(user: User): Long {
        return repository.registerUser(user)
    }

    suspend fun login(email: String, password: String): User? {
        return repository.login(email, password)
    }

    suspend fun isUserExists(email: String): Boolean {
        return repository.checkUserExists(email)
    }
}