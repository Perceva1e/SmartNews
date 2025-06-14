package com.example.smartnews.adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper

class SavedNewsFragment : Fragment() {

    private lateinit var adapter: SavedNewsAdapter

    companion object {
        private const val USER_ID = "user_id"
        private const val CATEGORY = "category"

        fun newInstance(userId: Int, category: String): SavedNewsFragment {
            val fragment = SavedNewsFragment()
            fragment.arguments = Bundle().apply {
                putInt(USER_ID, userId)
                putString(CATEGORY, category)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getInt(USER_ID) ?: 0
        val category = arguments?.getString(CATEGORY) ?: "general"

        val dbHelper = DatabaseHelper(requireContext())
        val user = dbHelper.getUserById(userId)
        val email = user?.email ?: ""

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing)
        recyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        val newsList = dbHelper.getSavedNewsByCategory(email, category)
        adapter = SavedNewsAdapter(newsList, email, object : SavedNewsAdapter.OnNewsDeletedListener {
            override fun onNewsDeleted() {
                val updatedNewsList = dbHelper.getSavedNewsByCategory(email, category)
                adapter.setSavedNews(updatedNewsList)
            }
        })
        recyclerView.adapter = adapter

        if (newsList.isEmpty()) {
            Log.d("SavedNewsFragment", "No news found for email: $email, category: $category")
        }
        adapter.setSavedNews(newsList)
    }
}