package com.example.boardexamreviewer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boardexamreviewer.data.*
import com.example.boardexamreviewer.databinding.FragmentReviewerBinding
import com.example.boardexamreviewer.utils.AppConfig
import com.example.boardexamreviewer.utils.GeminiClient
import com.example.boardexamreviewer.utils.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [SUB-MODULE: AI REVIEWER GENERATOR]
 * This fragment generates a study summary (Reviewer) from your document.
 */
class ReviewerFragment : Fragment() {

    private var _binding: FragmentReviewerBinding? = null
    private val binding get() = _binding!!

    private var selectedDocumentId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReviewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Click Listeners
        binding.btnSave.setOnClickListener { saveReviewer() }
        binding.btnRegenerate.setOnClickListener { generateReviewer() }
        binding.btnSelectSource.setOnClickListener { showSourceSelectionDialog() }

        // Check if we are viewing a specific saved reviewer
        val requestedReviewerId = arguments?.getInt("reviewerId", -1) ?: -1
        if (requestedReviewerId != -1) {
            loadSpecificReviewer(requestedReviewerId)
        } else {
            loadLatestReviewer()
        }
    }

    private fun showSourceSelectionDialog() {
        val appContext = requireContext().applicationContext
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val docs = db.appDao().getAllDocumentsByUser(currentUserId)
            
            withContext(Dispatchers.Main) {
                val fileNames = docs.map { it.fileName }.toTypedArray()
                if (fileNames.isEmpty()) {
                    Toast.makeText(context, "No files uploaded yet!", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Source Document")
                    .setItems(fileNames) { _, which ->
                        val selected = docs[which]
                        selectedDocumentId = selected.id
                        binding.tvCurrentSource.text = "Source: ${selected.fileName}"
                        Toast.makeText(context, "Selected: ${selected.fileName}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    /**
     * Loads a specific reviewer from the database.
     */
    private fun loadSpecificReviewer(reviewerId: Int) {
        val appContext = context?.applicationContext ?: return
        val currentUserId = appContext.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).getInt("current_user_id", -1)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val reviewer = db.appDao().getAllReviewersByUser(currentUserId).find { it.id == reviewerId }
            withContext(Dispatchers.Main) {
                if (_binding != null && reviewer != null) {
                    binding.etReviewerContent.setText(reviewer.content)
                    Toast.makeText(context, "Viewing: ${reviewer.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * [STEP: LOAD FROM DATABASE]
     * Pulls the saved summary from the phone's memory.
     */
    private fun loadLatestReviewer() {
        val appContext = context?.applicationContext ?: return
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val reviewers = db.appDao().getAllReviewersByUser(currentUserId)
            if (reviewers.isNotEmpty()) {
                val latest = reviewers.first()
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.etReviewerContent.setText(latest.content)
                    }
                }
            }
        }
    }

    /**
     * [STEP: AI GENERATION]
     * Talks to DeepSeek AI to summarize your document.
     */
    private fun generateReviewer() {
        val appContext = context?.applicationContext ?: return
        
        // Internet check
        if (!NetworkHelper.isInternetAvailable(appContext)) {
            Toast.makeText(context, "Check your internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegenerate.isEnabled = false

        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(appContext)
                
                // [STEP: GET SOURCE]
                val docSource = if (selectedDocumentId != -1) {
                    db.appDao().getAllDocumentsByUser(currentUserId).find { it.id == selectedDocumentId }
                } else {
                    db.appDao().getLastDocumentByUser(currentUserId)
                }
                
                // Error check: Need a document to summarize!
                if (docSource == null || docSource.extractedText.length < 10) {
                    withContext(Dispatchers.Main) {
                        if (_binding != null && context != null) {
                            Toast.makeText(requireContext(), "Upload a document first!", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegenerate.isEnabled = true
                        }
                    }
                    return@launch
                }

                // AI Prompt Setup
                val apiKey = AppConfig.GEMINI_API_KEY
                val prompt = "Based on the following text, generate a comprehensive reviewer summary for a board exam:\n\n${docSource.extractedText}"
                
                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )

                // Call Gemini
                val response = GeminiClient.apiService.generateContent(
                    AppConfig.GEMINI_MODEL,
                    apiKey,
                    request
                )

                withContext(Dispatchers.Main) {
                    if (_binding != null && context != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegenerate.isEnabled = true

                        if (response.isSuccessful) {
                            val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!result.isNullOrBlank()) {
                                binding.etReviewerContent.setText(result)
                                binding.etReviewerContent.requestFocus()
                                Toast.makeText(requireContext(), "Reviewer Successfully Generated!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            handleAiError(response.code())
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding != null && context != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegenerate.isEnabled = true
                        Toast.makeText(requireContext(), "Connection error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * [STEP: SAVE]
     * Saves the AI-generated text to the database.
     */
    private fun saveReviewer() {
        val content = binding.etReviewerContent.text.toString()
        if (content.isBlank()) return

        val appContext = requireContext().applicationContext
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            
            // [STEP: GET CORRECT SOURCE FOR TITLE]
            val docSource = if (selectedDocumentId != -1) {
                db.appDao().getAllDocumentsByUser(currentUserId).find { it.id == selectedDocumentId }
            } else {
                db.appDao().getLastDocumentByUser(currentUserId)
            }

            val reviewer = ReviewerEntity(
                userId = currentUserId,
                documentId = docSource?.id ?: 0,
                title = "Reviewer for ${docSource?.fileName ?: "Document"}",
                content = content
            )
            db.appDao().insertReviewer(reviewer)
            withContext(Dispatchers.Main) {
                if (_binding != null) Toast.makeText(context, "Reviewer saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * [STEP: ERROR HANDLING]
     * Translates confusing technical codes into helpful student messages.
     */
    private fun handleAiError(code: Int) {
        val message = when (code) {
            401 -> "API Key is invalid. Please double-check your AppConfig."
            402 -> "Google Balance Exhausted. Please top up your Gemini account."
            404 -> "Model Not Found. I will fix the model name for you."
            429 -> "Too many requests! Wait 30 seconds and try again."
            503 -> "Google's servers are overloaded (503). Wait 10 seconds and click Generate again!"
            else -> "AI Connection Error ($code). Check your internet!"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
