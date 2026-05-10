package com.example.boardexamreviewer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.NavHostFragment
import com.example.boardexamreviewer.R
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.databinding.FragmentSavedBinding
import com.example.boardexamreviewer.databinding.ItemSavedBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SavedItemsFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SavedItemsAdapter
    private var showingReviewers = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SavedItemsAdapter()
        binding.rvSavedItems.layoutManager = LinearLayoutManager(context)
        binding.rvSavedItems.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingReviewers = tab?.position == 0
                loadItems()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadItems()
    }

    private fun loadItems() {
        val appContext = requireContext().applicationContext
        val currentUserId = appContext.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).getInt("current_user_id", -1)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val items = if (showingReviewers) {
                db.appDao().getAllReviewersByUser(currentUserId).map { SavedItem(it.id, it.title, it.timestamp, true) }
            } else {
                db.appDao().getAllQuizzesByUser(currentUserId).map { SavedItem(it.id, it.title, it.timestamp, false) }
            }
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    adapter.submitList(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * [STEP: HANDLE ACTIONS]
     * This decides where to go when you click "View" or "Retake".
     */
    private fun onActionClicked(item: SavedItem) {
        val bundle = Bundle()
        if (item.isReviewer) {
            bundle.putInt("reviewerId", item.id)
            NavHostFragment.findNavController(this).navigate(R.id.navigation_reviewer, bundle)
        } else {
            bundle.putInt("quizId", item.id)
            NavHostFragment.findNavController(this).navigate(R.id.navigation_quiz, bundle)
        }
    }

    inner class SavedItemsAdapter : RecyclerView.Adapter<SavedItemsAdapter.ViewHolder>() {

        private var items = listOf<SavedItem>()

        fun submitList(newList: List<SavedItem>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemSavedBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: SavedItem) {
                binding.tvTitle.text = item.title
                val sdf = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault())
                binding.tvDate.text = sdf.format(Date(item.timestamp))
                
                binding.btnAction.text = if (item.isReviewer) "View" else "Retake"
                binding.btnAction.setOnClickListener { onActionClicked(item) }
            }
        }
    }
}

data class SavedItem(val id: Int, val title: String, val timestamp: Long, val isReviewer: Boolean)
