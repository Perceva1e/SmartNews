package com.example.smartnews

import com.google.firebase.FirebaseApp
import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}