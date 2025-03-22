package com.example.diplom.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.diplom.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT EXISTS(SELECT * FROM users WHERE email = :email)")
    fun checkUserExists(email: String): Flow<Boolean>
}