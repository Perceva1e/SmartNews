package com.example.smartnews.bd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "user.db"
        private const val DATABASE_VERSION = 3
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
        private const val COLUMN_EMAIL_FK = "email"
        private const val FIRESTORE_USERS_COLLECTION = "users"
        private const val FIRESTORE_SAVED_NEWS_COLLECTION = "saved_news"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_EMAIL TEXT UNIQUE,"
                + "$COLUMN_PASSWORD TEXT,"
                + "$COLUMN_NEWS_CATEGORIES TEXT,"
                + "$COLUMN_IS_VIP INTEGER DEFAULT 0)")
        db.execSQL(createUsersTable)

        val createSavedNewsTable = ("CREATE TABLE $TABLE_SAVED_NEWS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_EMAIL_FK TEXT,"
                + "$COLUMN_NEWS_TITLE TEXT,"
                + "$COLUMN_NEWS_DESCRIPTION TEXT,"
                + "$COLUMN_NEWS_URL TEXT,"
                + "$COLUMN_NEWS_URL_TO_IMAGE TEXT,"
                + "$COLUMN_NEWS_PUBLISHED_AT TEXT,"
                + "$COLUMN_NEWS_CATEGORY TEXT,"
                + "FOREIGN KEY($COLUMN_EMAIL_FK) REFERENCES $TABLE_USERS($COLUMN_EMAIL))")
        db.execSQL(createSavedNewsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            val createNewSavedNewsTable = ("CREATE TABLE temp_saved_news ("
                    + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "$COLUMN_EMAIL_FK TEXT,"
                    + "$COLUMN_NEWS_TITLE TEXT,"
                    + "$COLUMN_NEWS_DESCRIPTION TEXT,"
                    + "$COLUMN_NEWS_URL TEXT,"
                    + "$COLUMN_NEWS_URL_TO_IMAGE TEXT,"
                    + "$COLUMN_NEWS_PUBLISHED_AT TEXT,"
                    + "$COLUMN_NEWS_CATEGORY TEXT,"
                    + "FOREIGN KEY($COLUMN_EMAIL_FK) REFERENCES $TABLE_USERS($COLUMN_EMAIL))")
            db.execSQL(createNewSavedNewsTable)

            try {
                db.execSQL(
                    "INSERT INTO temp_saved_news ($COLUMN_ID, $COLUMN_EMAIL_FK, $COLUMN_NEWS_TITLE, $COLUMN_NEWS_DESCRIPTION, $COLUMN_NEWS_URL, $COLUMN_NEWS_URL_TO_IMAGE, $COLUMN_NEWS_PUBLISHED_AT, $COLUMN_NEWS_CATEGORY) "
                            + "SELECT sn.$COLUMN_ID, u.$COLUMN_EMAIL, sn.$COLUMN_NEWS_TITLE, sn.$COLUMN_NEWS_DESCRIPTION, sn.$COLUMN_NEWS_URL, sn.$COLUMN_NEWS_URL_TO_IMAGE, sn.$COLUMN_NEWS_PUBLISHED_AT, sn.$COLUMN_NEWS_CATEGORY "
                            + "FROM $TABLE_SAVED_NEWS sn JOIN $TABLE_USERS u ON sn.user_id = u.$COLUMN_ID"
                )
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error during data migration: ${e.message}")
            }

            db.execSQL("DROP TABLE IF EXISTS $TABLE_SAVED_NEWS")
            db.execSQL("ALTER TABLE temp_saved_news RENAME TO $TABLE_SAVED_NEWS")
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
                COLUMN_IS_VIP to isVip,
                "verified" to false
            )
            try {
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error saving to Firestore: ${e.message}")
            }
        }
        return id
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
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, hashedPassword)
            put(COLUMN_NEWS_CATEGORIES, newsCategories)
            put(COLUMN_IS_VIP, if (isVip) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_USERS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()

        if (rowsAffected > 0) {
            val userMap = hashMapOf(
                COLUMN_NAME to name,
                COLUMN_EMAIL to email,
                COLUMN_PASSWORD to hashedPassword,
                COLUMN_NEWS_CATEGORIES to newsCategories,
                COLUMN_IS_VIP to isVip,
                "verified" to (auth.currentUser?.isEmailVerified ?: false)
            )
            try {
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error updating Firestore: ${e.message}")
            }
        }
        return rowsAffected
    }

    suspend fun deleteUser(id: Int): Int {
        val db = this.writableDatabase
        var email: String? = null
        var newsIds: List<Int> = emptyList()

        val userCursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_EMAIL),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        if (userCursor.moveToFirst()) {
            email = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_EMAIL))
        }
        userCursor.close()

        if (email != null) {
            val newsCursor = db.query(
                TABLE_SAVED_NEWS,
                arrayOf(COLUMN_ID),
                "$COLUMN_EMAIL_FK = ?",
                arrayOf(email),
                null,
                null,
                null
            )
            newsIds = mutableListOf<Int>().apply {
                while (newsCursor.moveToNext()) {
                    add(newsCursor.getInt(newsCursor.getColumnIndexOrThrow(COLUMN_ID)))
                }
            }
            newsCursor.close()

            val newsDeleted = db.delete(TABLE_SAVED_NEWS, "$COLUMN_EMAIL_FK = ?", arrayOf(email))
            Log.d("DatabaseHelper", "Deleted $newsDeleted saved news for email: $email")
        }

        val rowsAffected = db.delete(TABLE_USERS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()

        if (rowsAffected > 0 && email != null) {
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && firebaseUser.email == email) {
                    firebaseUser.delete().await()
                    Log.d("DatabaseHelper", "Deleted user from Firebase Auth: $email")
                }
                firestore.collection(FIRESTORE_USERS_COLLECTION).document(email).delete().await()
                Log.d("DatabaseHelper", "Deleted user from Firestore: $email")
                newsIds.forEach { newsId ->
                    firestore.collection(FIRESTORE_SAVED_NEWS_COLLECTION)
                        .document("$email-$newsId")
                        .delete()
                        .await()
                    Log.d("DatabaseHelper", "Deleted saved news from Firestore: $email-$newsId")
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error deleting from Firebase: ${e.message}")
            }
        }

        return rowsAffected
    }

    suspend fun saveNews(email: String, news: SavedNews): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EMAIL_FK, email)
            put(COLUMN_NEWS_TITLE, news.title)
            put(COLUMN_NEWS_DESCRIPTION, news.description)
            put(COLUMN_NEWS_URL, news.url)
            put(COLUMN_NEWS_URL_TO_IMAGE, news.urlToImage)
            put(COLUMN_NEWS_PUBLISHED_AT, news.publishedAt)
            put(COLUMN_NEWS_CATEGORY, news.category)
        }
        val id = db.insert(TABLE_SAVED_NEWS, null, values)
        db.close()

        Log.d("DatabaseHelper", "SQLite insert result: id=$id, email=$email")

        if (id != -1L) {
            val newsMap = hashMapOf(
                "email" to email,
                "title" to news.title,
                "description" to news.description,
                "url" to news.url,
                "url_to_image" to news.urlToImage,
                "published_at" to news.publishedAt,
                "category" to news.category
            )
            try {
                Log.d("DatabaseHelper", "Saving to Firestore: document=$email-$id")
                firestore.collection(FIRESTORE_SAVED_NEWS_COLLECTION)
                    .document("$email-$id")
                    .set(newsMap)
                    .await()
                Log.d("DatabaseHelper", "Successfully saved to Firestore: $email-$id")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error saving to Firestore: $email-$id", e)
            }
        }
        return id
    }

    suspend fun deleteNews(email: String, newsId: Int): Int {
        val db = this.writableDatabase
        val rowsAffected = db.delete(
            TABLE_SAVED_NEWS,
            "$COLUMN_ID = ? AND $COLUMN_EMAIL_FK = ?",
            arrayOf(newsId.toString(), email)
        )
        db.close()

        if (rowsAffected > 0) {
            try {
                firestore.collection(FIRESTORE_SAVED_NEWS_COLLECTION)
                    .document("$email-$newsId")
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error deleting from Firestore: ${e.message}")
            }
        }
        return rowsAffected
    }

    fun getSavedNewsByCategory(email: String, category: String): List<SavedNews> {
        val db = this.readableDatabase
        val newsList = mutableListOf<SavedNews>()
        val cursor = db.query(
            TABLE_SAVED_NEWS,
            arrayOf(
                COLUMN_ID, COLUMN_NEWS_TITLE, COLUMN_NEWS_DESCRIPTION,
                COLUMN_NEWS_URL, COLUMN_NEWS_URL_TO_IMAGE, COLUMN_NEWS_PUBLISHED_AT, COLUMN_NEWS_CATEGORY
            ),
            "$COLUMN_EMAIL_FK = ? AND $COLUMN_NEWS_CATEGORY = ?",
            arrayOf(email, category),
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

    fun getUserById(id: Int): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EMAIL, COLUMN_PASSWORD, COLUMN_NEWS_CATEGORIES, COLUMN_IS_VIP),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
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

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EMAIL, COLUMN_PASSWORD, COLUMN_NEWS_CATEGORIES, COLUMN_IS_VIP),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
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