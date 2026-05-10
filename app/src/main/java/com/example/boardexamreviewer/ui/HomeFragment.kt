package com.example.boardexamreviewer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.boardexamreviewer.R
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.data.DocumentEntity
import com.example.boardexamreviewer.databinding.FragmentHomeBinding
import com.example.boardexamreviewer.utils.DocumentExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HomeFragment is the first screen users see.
 * It contains buttons to upload documents and navigate to other parts of the app.
 */
class HomeFragment : Fragment() {

    // _binding is used to access UI elements. It's nullable because UI is destroyed when navigating away.
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // registerForActivityResult is the modern way to handle activities that return a result (like picking a file).
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // If the user picked a file successfully, we get its "Uri" (the address of the file).
            val uri: Uri? = result.data?.data
            uri?.let { handleSelectedFile(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // We "inflate" the layout file (fragment_home.xml) so we can use it in code.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // [NAVIGATION SETUP]
        // These buttons take you to different screens.
        binding.btnUpload.setOnClickListener { openFilePicker() }
        
        binding.btnViewFiles.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_files)
        }

        binding.btnGenReviewer.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_reviewer)
        }

        binding.btnGenQuiz.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_quiz)
        }

        binding.btnPomodoro.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_pomodoro)
        }
    }

    /**
     * [STEP: FILE PICKER]
     * Opens the phone's file explorer to select PDF or DOCX.
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            val mimeTypes = arrayOf(
                "application/pdf", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
            )
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        filePickerLauncher.launch(intent)
    }

    /**
     * [STEP: FILE HANDLING]
     * Extracts text from the file and saves it to the database.
     */
    private fun handleSelectedFile(uri: Uri) {
        val appContext = requireContext().applicationContext
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Extract Text
                val text = DocumentExtractor.extractText(appContext, uri)
                val fileName = DocumentExtractor.getFileName(appContext, uri)

                // 2. Save to Database
                val db = AppDatabase.getDatabase(appContext)
                val prefs = appContext.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                val currentUserId = prefs.getInt("current_user_id", -1)

                val doc = DocumentEntity(
                    userId = currentUserId,
                    fileName = fileName, 
                    filePath = uri.toString(), 
                    extractedText = text
                )
                db.appDao().insertDocument(doc)

                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Uploaded to Profile: $fileName", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding to avoid memory leaks.
        _binding = null
    }
}
