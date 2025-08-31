package com.example.smartnews.activity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.smartnews.R
import com.example.smartnews.utils.NetworkUtils

class NoInternetActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)

        val tvMessage = findViewById<TextView>(R.id.tv_no_internet_message)
        tvMessage.text = getString(R.string.no_internet_message)

        val btnRetry = findViewById<Button>(R.id.btn_retry)
        btnRetry.setOnClickListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                finish()
            } else {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.still_no_internet),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun checkInternetConnection() {
    }
}