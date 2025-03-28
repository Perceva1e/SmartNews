package com.example.diplom.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.diplom.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT EXISTS(SELECT * FROM users WHERE email = :email)")
    suspend fun checkUserExists(email: String): Boolean

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): User?

    @Query("UPDATE users SET name = :name, email = :email WHERE id = :userId")
    suspend fun updateUser(userId: Int, name: String, email: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Int)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): Flow<User>

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmailFlow(email: String): Flow<User?>

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    suspend fun updatePassword(email: String, newPassword: String): Int
}