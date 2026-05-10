package com.example.boardexamreviewer.ui.progress

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.data.StudySessionEntity
import com.example.boardexamreviewer.databinding.FragmentProgressBinding
import com.example.boardexamreviewer.databinding.ItemFileBinding // Reuse for simple list
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * [SUB-MODULE: PROGRESS TRACKING]
 * This fragment shows the user's total study time and session history.
 * Label: PROGRESS_MODULE
 */
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SessionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SessionAdapter()
        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter

        loadProgress()
    }

    private fun loadProgress() {
        val appContext = context?.applicationContext ?: return
        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("current_user_id", -1)

        if (userId == -1) {
            binding.tvTotalTime.text = "Please Log In"
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val totalTime = db.appDao().getTotalStudyTime(userId) ?: 0
            val sessions = db.appDao().getStudySessionsByUser(userId)

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.tvTotalTime.text = "$totalTime Minutes"
                    adapter.submitList(sessions)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SessionAdapter : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

    private var items = listOf<StudySessionEntity>()

    fun submitList(newList: List<StudySessionEntity>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StudySessionEntity) {
            binding.tvFileName.text = "${item.durationMinutes} min ${item.sessionType} Session"
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.tvFileDate.text = sdf.format(Date(item.timestamp))
            binding.btnDelete.visibility = View.GONE
        }
    }
}
