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
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_NEWS_CATEGORIES = "news_categories"
        private const val COLUMN_IS_VIP = "is_vip"
        private const val FIRESTORE_COLLECTION = "users"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_EMAIL TEXT,"
                + "$COLUMN_PASSWORD TEXT,"
                + "$COLUMN_NEWS_CATEGORIES TEXT,"
                + "$COLUMN_IS_VIP INTEGER DEFAULT 0)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_VIP INTEGER DEFAULT 0")
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
        val id = db.insert(TABLE_NAME, null, values)
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
                firestore.collection(FIRESTORE_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return id
    }

    fun checkUser(email: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?", arrayOf(email, password))
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
            TABLE_NAME,
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
        val rowsAffected = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
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
                firestore.collection(FIRESTORE_COLLECTION).document(email).set(userMap).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rowsAffected
    }

    suspend fun deleteUser(id: Int): Int {
        val db = this.writableDatabase
        val cursor = db.query(
            TABLE_NAME,
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

        val rowsAffected = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()

        if (rowsAffected > 0 && email != null) {
            try {
                firestore.collection(FIRESTORE_COLLECTION).document(email).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rowsAffected
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