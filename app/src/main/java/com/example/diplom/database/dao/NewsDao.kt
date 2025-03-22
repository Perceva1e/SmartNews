package com.example.diplom.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.diplom.database.entity.SavedNews
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Insert
    suspend fun saveNews(news: SavedNews)

    @Query("SELECT * FROM saved_news WHERE user_id = :userId")
    suspend fun getSavedNewsByUser(userId: Int): List<SavedNews>

    @Query("DELETE FROM saved_news WHERE id = :newsId")
    suspend fun deleteNews(newsId: Int)
}