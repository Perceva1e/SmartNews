package com.example.smartnews.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.adapter.CategoryAdapter
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class NewsFilterActivity : BaseActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private val selectedCategories = mutableSetOf<String>()
    private val TAG = "NewsFilterActivity"
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }

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
            Log.e(TAG, "Invalid USER_ID → finish")
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val etDateFrom = findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = findViewById<EditText>(R.id.etDateTo)
        val spMood = findViewById<Spinner>(R.id.spMood)
        val btnSaveFilter = findViewById<MaterialButton>(R.id.btnSaveFilter)
        val btnClearFilter = findViewById<MaterialButton>(R.id.btnClearFilter)

        ivBack.setOnClickListener { finish() }

        etDateFrom.setOnClickListener { showDatePicker(etDateFrom) }
        etDateTo.setOnClickListener { showDatePicker(etDateTo) }

        val moods = resources.getStringArray(R.array.moods)
        val moodAdapter = ArrayAdapter(this, R.layout.spinner_item, moods)
        moodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spMood.adapter = moodAdapter

        etDateFrom.setText(sharedPref.getString("news_date_from", ""))
        etDateTo.setText(sharedPref.getString("news_date_to", ""))
        val currentMood = sharedPref.getString("news_mood", getString(R.string.mood_all)) ?: getString(R.string.mood_all)
        spMood.setSelection(moods.indexOfFirst { it == currentMood }.coerceAtLeast(0))

        val categories = listOf("business", "entertainment", "general", "health", "science", "sports", "technology")
        Log.d(TAG, "Loaded categories: $categories")

        val user = dbHelper.getUser()
        user?.newsCategories?.split(",")?.filter { it.isNotBlank() }?.let {
            selectedCategories.addAll(it)
        }

        rvCategories.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryAdapter(categories, selectedCategories, this)
        rvCategories.adapter = adapter

        btnSaveFilter.setOnClickListener {
            val categoriesToSave = selectedCategories.joinToString(",").takeIf { it.isNotEmpty() }
            val dateFrom = etDateFrom.text.toString().trim().takeIf { it.isNotEmpty() }
            val dateTo   = etDateTo.text.toString().trim().takeIf { it.isNotEmpty() }
            val mood     = spMood.selectedItem?.toString()?.takeIf { it != getString(R.string.mood_all) }

            Log.d(TAG, "Сохранение фильтров: cat=$categoriesToSave, from=$dateFrom, to=$dateTo, mood=$mood")

            sharedPref.edit().apply {
                putString("news_date_from", dateFrom)
                putString("news_date_to", dateTo)
                putString("news_mood", mood)
                apply()
            }

            val currentUser = dbHelper.getUser()
            if (currentUser != null) {
                lifecycleScope.launch {
                    try {
                        dbHelper.updateUser(
                            currentUser.id,
                            currentUser.name,
                            currentUser.email,
                            currentUser.password,
                            categoriesToSave,
                            currentUser.isVip
                        )
                        Log.d(TAG, "Категории успешно обновлены в БД")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка обновления категорий: ${e.message}", e)
                    }
                }
            }

            showCustomDialog(
                getString(R.string.success_title),
                getString(R.string.filter_saved),
                R.layout.custom_dialog_success
            ) {
                setResult(RESULT_OK)
                finish()
            }
        }

        btnClearFilter.setOnClickListener {
            selectedCategories.clear()
            adapter.notifyDataSetChanged()
            etDateFrom.text?.clear()
            etDateTo.text?.clear()
            spMood.setSelection(0)

            sharedPref.edit().apply {
                remove("news_date_from")
                remove("news_date_to")
                remove("news_mood")
                apply()
            }

            val currentUser = dbHelper.getUser()
            if (currentUser != null) {
                lifecycleScope.launch {
                    try {
                        dbHelper.updateUser(currentUser.id, currentUser.name, currentUser.email, currentUser.password, null, currentUser.isVip)
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка очистки категорий: ${e.message}")
                    }
                }
            }

            showCustomDialog(
                getString(R.string.success_title),
                getString(R.string.filter_cleared),
                R.layout.custom_dialog_success
            ) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun showDatePicker(et: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            et.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int, onOk: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk()
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}