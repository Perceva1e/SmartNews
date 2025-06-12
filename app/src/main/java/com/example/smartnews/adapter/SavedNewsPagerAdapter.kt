package com.example.smartnews.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SavedNewsPagerAdapter(
    activity: FragmentActivity,
    private val userId: Int,
    private val categories: List<String>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return SavedNewsFragment.newInstance(userId, categories[position])
    }
}