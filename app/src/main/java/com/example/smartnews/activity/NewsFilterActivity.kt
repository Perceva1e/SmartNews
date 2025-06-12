package com.example.smartnews.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.adapter.CategoryAdapter
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.button.MaterialButton

class NewsFilterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private val selectedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_filter)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        dbHelper = DatabaseHelper(this)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val btnSaveFilter = findViewById<MaterialButton>(R.id.btnSaveFilter)
        val btnClearFilter = findViewById<MaterialButton>(R.id.btnClearFilter)

        ivBack.setOnClickListener { finish() }

        val categories = resources.getStringArray(R.array.filter_categories).toList()
        val user = dbHelper.getUser()
        user?.newsCategories?.split(",")?.filter { it.isNotBlank() }?.let { selectedCategories.addAll(it) }

        rvCategories.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryAdapter(categories, selectedCategories)
        rvCategories.adapter = adapter

        btnSaveFilter.setOnClickListener {
            val categoriesToSave = selectedCategories.joinToString(",").takeIf { it.isNotEmpty() }
            val user = dbHelper.getUser()
            if (user != null) {
                dbHelper.updateUser(user.id, user.name, user.email, user.password, categoriesToSave)
                showCustomDialog(
                    getString(R.string.success_title),
                    getString(R.string.filter_saved),
                    R.layout.custom_dialog_success
                ) { finish() }
            }
        }

        btnClearFilter.setOnClickListener {
            selectedCategories.clear()
            adapter.notifyDataSetChanged()
            val user = dbHelper.getUser()
            if (user != null) {
                dbHelper.updateUser(user.id, user.name, user.email, user.password, null)
                showCustomDialog(
                    getString(R.string.success_title),
                    getString(R.string.filter_cleared),
                    R.layout.custom_dialog_success
                )
            }
        }
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int, onOk: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvTitle)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}