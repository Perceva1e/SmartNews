package com.example.smartnews.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R

class CategoryAdapter(
    private val categories: List<String>,
    private val selectedCategories: MutableSet<String>,
    private val context: Context
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val TAG = "CategoryAdapter"

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.cbCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.checkBox.text = getDisplayName(category)
        val isChecked = selectedCategories.contains(category)
        Log.d(TAG, "Binding category: $category, isChecked: $isChecked")
        holder.checkBox.isChecked = isChecked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Category $category checked: $isChecked")
            if (isChecked) {
                selectedCategories.add(category)
            } else {
                selectedCategories.remove(category)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    private fun getDisplayName(category: String): String {
        return when (category) {
            "general" -> context.getString(R.string.category_general)
            "business" -> context.getString(R.string.category_business)
            "entertainment" -> context.getString(R.string.category_entertainment)
            "health" -> context.getString(R.string.category_health)
            "science" -> context.getString(R.string.category_science)
            "sports" -> context.getString(R.string.category_sports)
            "technology" -> context.getString(R.string.category_technology)
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }
}