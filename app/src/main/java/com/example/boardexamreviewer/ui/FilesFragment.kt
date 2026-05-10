package com.example.boardexamreviewer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.data.DocumentEntity
import com.example.boardexamreviewer.databinding.FragmentFilesBinding
import com.example.boardexamreviewer.databinding.ItemFileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * FilesFragment shows a list of all documents you have uploaded.
 * Student Note: We use a RecyclerView to show the list efficiently.
 */
class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FilesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our basic adapter
        adapter = FilesAdapter(
            onItemClick = { doc -> showExtractedText(doc) },
            onDeleteClick = { doc -> deleteDocument(doc) }
        )

        binding.rvFiles.layoutManager = LinearLayoutManager(context)
        binding.rvFiles.adapter = adapter

        loadDocuments()
    }

    private fun loadDocuments() {
        // Safe way to get context
        val appContext = context?.applicationContext ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        
        // [STEP: GET CURRENT USER]
        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getInt("current_user_id", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val docs = db.appDao().getAllDocumentsByUser(currentUserId)
                
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        adapter.updateList(docs) // Refresh the list
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun showExtractedText(doc: DocumentEntity) {
        val scrollView = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext()).apply {
            text = doc.extractedText
            setPadding(40, 40, 40, 40)
            textSize = 16f
        }
        scrollView.addView(textView)

        AlertDialog.Builder(requireContext())
            .setTitle(doc.fileName)
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun deleteDocument(doc: DocumentEntity) {
        val appContext = requireContext().applicationContext
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete ${doc.fileName}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(appContext)
                    db.appDao().deleteDocument(doc)
                    loadDocuments() // Reload the list after deleting
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Basic RecyclerView Adapter for Students.
 * This is easier to understand than the "ListAdapter" version.
 */
class FilesAdapter(
    private val onItemClick: (DocumentEntity) -> Unit,
    private val onDeleteClick: (DocumentEntity) -> Unit
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    private var items = mutableListOf<DocumentEntity>()

    // Simple function to update the data
    fun updateList(newList: List<DocumentEntity>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged() // Tells the list to refresh itself
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DocumentEntity) {
            binding.tvFileName.text = item.fileName
            
            // Format the date to look nice (e.g., May 3, 2026 - 12:00 PM)
            val sdf = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault())
            binding.tvFileDate.text = sdf.format(Date(item.timestamp))

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}

