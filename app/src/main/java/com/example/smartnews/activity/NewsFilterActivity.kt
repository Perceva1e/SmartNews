package com.example.smartnews.activity

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.adapter.CategoryAdapter
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.button.MaterialButton
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class NewsFilterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private val selectedCategories = mutableSetOf<String>()
    private val TAG = "NewsFilterActivity"

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_filter)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e(TAG, "Invalid USER_ID, finishing activity")
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val btnSaveFilter = findViewById<MaterialButton>(R.id.btnSaveFilter)
        val btnClearFilter = findViewById<MaterialButton>(R.id.btnClearFilter)

        ivBack.setOnClickListener { finish() }

        val categories = listOf("business", "entertainment", "general", "health", "science", "sports", "technology")
        Log.d(TAG, "Loaded categories: $categories")
        if (categories.isEmpty()) {
            Log.e(TAG, "No categories found")
            showCustomDialog(
                getString(R.string.error_title),
                getString(R.string.error_no_categories),
                R.layout.custom_dialog_error
            )
            return
        }

        val user = dbHelper.getUser()
        user?.newsCategories?.split(",")?.filter { it.isNotBlank() }?.let {
            selectedCategories.addAll(it)
            Log.d(TAG, "Loaded selected categories: $selectedCategories")
        }

        rvCategories.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryAdapter(categories, selectedCategories, this)
        rvCategories.adapter = adapter

        btnSaveFilter.setOnClickListener {
            val categoriesToSave = selectedCategories.joinToString(",").takeIf { it.isNotEmpty() }
            Log.d(TAG, "Saving categories: $categoriesToSave")
            val user = dbHelper.getUser()
            if (user != null) {
                lifecycleScope.launch {
                    try {
                        dbHelper.updateUser(user.id, user.name, user.email, user.password, categoriesToSave, user.isVip)
                        setResult(RESULT_OK)
                        showCustomDialog(
                            getString(R.string.success_title),
                            getString(R.string.filter_saved),
                            R.layout.custom_dialog_success
                        ) { finish() }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving categories: ${e.message}")
                        showCustomDialog(
                            getString(R.string.error_title),
                            getString(R.string.error_operation_failed),
                            R.layout.custom_dialog_error
                        )
                    }
                }
            } else {
                Log.e(TAG, "User not found")
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_user_not_found),
                    R.layout.custom_dialog_error
                )
            }
        }

        btnClearFilter.setOnClickListener {
            selectedCategories.clear()
            Log.d(TAG, "Cleared selected categories")
            adapter.notifyDataSetChanged()
            val user = dbHelper.getUser()
            if (user != null) {
                lifecycleScope.launch {
                    try {
                        dbHelper.updateUser(user.id, user.name, user.email, user.password, null, user.isVip)
                        setResult(RESULT_OK)
                        showCustomDialog(
                            getString(R.string.success_title),
                            getString(R.string.filter_cleared),
                            R.layout.custom_dialog_success
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing categories: ${e.message}")
                        showCustomDialog(
                            getString(R.string.error_title),
                            getString(R.string.error_operation_failed),
                            R.layout.custom_dialog_error
                        )
                    }
                }
            } else {
                Log.e(TAG, "User not found")
                showCustomDialog(
                    getString(R.string.error_title),
                    getString(R.string.error_user_not_found),
                    R.layout.custom_dialog_error
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int, onOk: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }
}