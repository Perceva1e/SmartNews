package com.example.diplom.utils

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData

fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
    observeForever(object : androidx.lifecycle.Observer<T> {
        override fun onChanged(value: T) {
            removeObserver(this)
            observer(value)
        }
    })
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun <T> kotlinx.coroutines.flow.Flow<T>.asLiveData() = this.asLiveData(
    kotlinx.coroutines.Dispatchers.Default
)
fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}