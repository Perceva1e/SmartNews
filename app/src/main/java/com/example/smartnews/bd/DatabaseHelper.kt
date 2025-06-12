package com.example.smartnews.bd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "user.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_USERS = "users"
        private const val TABLE_SAVED_NEWS = "saved_news"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_NEWS_CATEGORIES = "news_categories"
        private const val COLUMN_IS_VIP = "is_vip"
        private const val COLUMN_NEWS_TITLE = "title"
        private const val COLUMN_NEWS_DESCRIPTION = "description"
        private const val COLUMN_NEWS_URL = "url"
        private const val COLUMN_NEWS_URL_TO_IMAGE = "url_to_image"
        private const val COLUMN_NEWS_PUBLISHED_AT = "published_at"
        private const val COLUMN_NEWS_CATEGORY = "category"
        private const val COLUMN_USER_ID = "user_id"
        private const val FIRESTORE_USERS_COLLECTION = "users"
        private const val FIRESTORE_SAVED_NEWS_COLLECTION = "saved_news"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_EMAIL TEXT,"
                + "$COLUMN_PASSWORD TEXT,"
                + "$COLUMN_NEWS_CATEGORIES TEXT,"
                + "$COLUMN_IS_VIP INTEGER DEFAULT 0)")
        db.execSQL(createUsersTable)

        val createSavedNewsTable = ("CREATE TABLE $TABLE_SAVED_NEWS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_USER_ID INTEGER,"
                + "$COLUMN_NEWS_TITLE TEXT,"
                + "$COLUMN_NEWS_DESCRIPTION TEXT,"
                + "$COLUMN_NEWS_URL TEXT,"
                + "$COLUMN_NEWS_URL_TO_IMAGE TEXT,"
                + "$COLUMN_NEWS_PUBLISHED_AT TEXT,"
                + "$COLUMN_NEWS_CATEGORY TEXT,"
                + "FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID))")
        db.execSQL(createSavedNewsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createSavedNewsTable = ("CREATE TABLE $TABLE_SAVED_NEWS ("
                    + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "$COLUMN_USER_ID INTEGER,"
                    + "$COLUMN_NEWS_TITLE TEXT,"
                    + "$COLUMN_NEWS_DESCRIPTION TEXT,"
                    + "$COLUMN_NEWS_URL TEXT,"
                    + "$COLUMN_NEWS_URL_TO_IMAGE TEXT,"
                    + "$COLUMN_NEWS_PUBLISHED_AT TEXT,"
                    + "$COLUMN_NEWS_CATEGORY TEXT,"
                    + "FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID))")
            db.execSQL(createSavedNewsTable)
        }
    }

    suspend fun addUser(name: String, email: String, password: String, newsCategories: String? = null, isVip: Boolean = false): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NEWS_CATEGORIES, newsCategories)
            put(COLUMN_IS_VIP, if (isVip) 1 else 0)
        }
        val id = db.insert(TABLE_USERS, null, values)
        db.close()

        if (id != -1L) {
            val userMap = hashMapOf(
                COLUMN_NAME to name,
                COLUMN_EMAIL to email,
                COLUMN_PASSWORD to password,
                COLUMN_NEWS_CATEGORIES to newsCategories,
                COLUMN_IS_VIP to isVip
            )
            try {
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return id
    }

    fun checkUser(email: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?", arrayOf(email, password))
        return if (cursor.moveToFirst()) {
            User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                newsCategories = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORIES)),
                isVip = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_VIP)) == 1
            )
        } else {
            cursor.close()
            null
        }
    }

    fun getUser(): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EMAIL, COLUMN_PASSWORD, COLUMN_NEWS_CATEGORIES, COLUMN_IS_VIP),
            null,
            null,
            null,
            null,
            null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                newsCategories = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORIES)),
                isVip = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_VIP)) == 1
            )
        }
        cursor.close()
        db.close()
        return user
    }

    suspend fun updateUser(id: Int, name: String, email: String, password: String, newsCategories: String? = null, isVip: Boolean = false): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NEWS_CATEGORIES, newsCategories)
            put(COLUMN_IS_VIP, if (isVip) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_USERS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()

        if (rowsAffected > 0) {
            val userMap = hashMapOf(
                COLUMN_NAME to name,
                COLUMN_EMAIL to email,
                COLUMN_PASSWORD to password,
                COLUMN_NEWS_CATEGORIES to newsCategories,
                COLUMN_IS_VIP to isVip
            )
            try {
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rowsAffected
    }

    suspend fun deleteUser(id: Int): Int {
        val db = this.writableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_EMAIL),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        var email: String? = null
        if (cursor.moveToFirst()) {
            email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
        }
        cursor.close()

        val rowsAffected = db.delete(TABLE_USERS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()

        if (rowsAffected > 0 && email != null) {
            try {
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rowsAffected
    }

    suspend fun saveNews(userId: Int, news: SavedNews): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_NEWS_TITLE, news.title)
            put(COLUMN_NEWS_DESCRIPTION, news.description)
            put(COLUMN_NEWS_URL, news.url)
            put(COLUMN_NEWS_URL_TO_IMAGE, news.urlToImage)
            put(COLUMN_NEWS_PUBLISHED_AT, news.publishedAt)
            put(COLUMN_NEWS_CATEGORY, news.category)
        }
        val id = db.insert(TABLE_SAVED_NEWS, null, values)
        db.close()

        if (id != -1L) {
            val newsMap = hashMapOf(
                "user_id" to userId,
                "title" to news.title,
                "description" to news.description,
                "url" to news.url,
                "url_to_image" to news.urlToImage,
                "published_at" to news.publishedAt,
                "category" to news.category
            )
            try {
                firestore.collection(FIRESTORE_SAVED_NEWS_COLLECTION)
                    .document("$userId-$id")
                    .set(newsMap)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return id
    }

    fun getSavedNewsByCategory(userId: Int, category: String): List<SavedNews> {
        val db = this.readableDatabase
        val newsList = mutableListOf<SavedNews>()
        val cursor = db.query(
            TABLE_SAVED_NEWS,
            arrayOf(
                COLUMN_ID, COLUMN_NEWS_TITLE, COLUMN_NEWS_DESCRIPTION,
                COLUMN_NEWS_URL, COLUMN_NEWS_URL_TO_IMAGE, COLUMN_NEWS_PUBLISHED_AT, COLUMN_NEWS_CATEGORY
            ),
            "$COLUMN_USER_ID = ? AND $COLUMN_NEWS_CATEGORY = ?",
            arrayOf(userId.toString(), category),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            newsList.add(
                SavedNews(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_DESCRIPTION)),
                    url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_URL)),
                    urlToImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_URL_TO_IMAGE)),
                    publishedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_PUBLISHED_AT)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORY))
                )
            )
        }
        cursor.close()
        db.close()
        return newsList
    }

    fun getAllCategories(userId: Int): List<String> {
        val db = this.readableDatabase
        val categories = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT $COLUMN_NEWS_CATEGORY FROM $TABLE_SAVED_NEWS WHERE $COLUMN_USER_ID = ?", arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORY))?.let {
                categories.add(it)
            }
        }
        cursor.close()
        db.close()
        return categories
    }
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val newsCategories: String? = null,
    val isVip: Boolean = false
)

data class SavedNews(
    val id: Int = 0,
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val category: String
)