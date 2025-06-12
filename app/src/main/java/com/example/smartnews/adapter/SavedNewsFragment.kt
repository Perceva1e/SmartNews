package com.example.smartnews.adapter

import android.os.Bundle
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

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SavedNewsAdapter(userId, object : SavedNewsAdapter.OnNewsDeletedListener {
            override fun onNewsDeleted() {
                val dbHelper = DatabaseHelper(requireContext())
                val newsList = dbHelper.getSavedNewsByCategory(userId, category)
                adapter.setSavedNews(newsList)
            }
        })
        recyclerView.adapter = adapter

        val dbHelper = DatabaseHelper(requireContext())
        val newsList = dbHelper.getSavedNewsByCategory(userId, category)
        adapter.setSavedNews(newsList)
    }
}