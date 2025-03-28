package com.example.diplom.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object AppEvents {
    private val _newsUpdates = MutableSharedFlow<Pair<Int, String>>(replay = 1)
    val newsUpdates = _newsUpdates.asSharedFlow()

    fun notifyNewsChanged(userId: Int, action: String) {
        CoroutineScope(Dispatchers.Default).launch {
            _newsUpdates.emit(userId to action)
        }
    }
}