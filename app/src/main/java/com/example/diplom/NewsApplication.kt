package com.example.diplom

import android.app.Application
import com.example.diplom.database.AppDatabase

class NewsApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
