package com.example.smartnews.bd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_EMAIL TEXT,"
                + "$COLUMN_PASSWORD TEXT,"
                + "$COLUMN_NEWS_CATEGORIES TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO temp_users")
            onCreate(db)
            db.execSQL("INSERT INTO $TABLE_NAME ($COLUMN_ID, $COLUMN_NAME, $COLUMN_EMAIL, $COLUMN_PASSWORD) " +
                    "SELECT $COLUMN_ID, $COLUMN_NAME, $COLUMN_EMAIL, $COLUMN_PASSWORD FROM temp_users")
            db.execSQL("DROP TABLE temp_users")
        }
    }

    fun addUser(name: String, email: String, password: String, newsCategories: String? = null): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NEWS_CATEGORIES, newsCategories)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
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
                newsCategories = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORIES))
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
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EMAIL, COLUMN_PASSWORD, COLUMN_NEWS_CATEGORIES),
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
                newsCategories = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NEWS_CATEGORIES))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun updateUser(id: Int, name: String, email: String, password: String, newsCategories: String? = null): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NEWS_CATEGORIES, newsCategories)
        }
        val rowsAffected = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteUser(id: Int): Int {
        val db = this.writableDatabase
        val rowsAffected = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsAffected
    }
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val newsCategories: String? = null
)