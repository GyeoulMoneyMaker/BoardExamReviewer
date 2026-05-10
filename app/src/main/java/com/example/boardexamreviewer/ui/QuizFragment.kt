package com.example.boardexamreviewer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boardexamreviewer.R
import com.example.boardexamreviewer.data.*
import com.example.boardexamreviewer.databinding.FragmentQuizBinding
import com.example.boardexamreviewer.utils.AppConfig
import com.example.boardexamreviewer.utils.GeminiClient
import com.example.boardexamreviewer.utils.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [SUB-MODULE: AI QUIZ GENERATOR]
 * This fragment uses AI to generate questions from your document.
 */
class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private var currentQuestions = listOf<QuizQuestion>()

    private var selectedSourceId: Int = -1
    private var isSourceReviewer: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Click Listeners
        binding.btnCheckAnswers.setOnClickListener { checkAnswers() }
        binding.btnSaveQuiz.setOnClickListener { saveQuiz() }
        binding.btnGenerateQuiz.setOnClickListener { generateQuiz() }
        binding.btnViewHistory.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.navigation_saved)
        }
        binding.btnSelectQuizSource.setOnClickListener { showSourceSelectionDialog() }

        // 2. Load the requested quiz or the latest one
        val requestedQuizId = arguments?.getInt("quizId", -1) ?: -1
        if (requestedQuizId != -1) {
            loadSpecificQuiz(requestedQuizId)
        } else {
            loadLatestQuiz()
        }
    }

    private fun showSourceSelectionDialog() {
        val appContext = requireContext().applicationContext
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val docs = db.appDao().getAllDocumentsByUser(currentUserId)
            val reviewers = db.appDao().getAllReviewersByUser(currentUserId)
            
            withContext(Dispatchers.Main) {
                val docOptions = docs.map { "[Doc] ${it.fileName}" }
                val reviewerOptions = reviewers.map { "[Reviewer] ${it.title}" }
                val allOptions = docOptions + reviewerOptions

                if (allOptions.isEmpty()) {
                    Toast.makeText(context, "No files or reviewers found!", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Choose Quiz Source")
                    .setItems(allOptions.toTypedArray()) { _, which ->
                        if (which < docs.size) {
                            val selected = docs[which]
                            selectedSourceId = selected.id
                            isSourceReviewer = false
                            binding.tvQuizSource.text = "Source: ${selected.fileName}"
                        } else {
                            val selected = reviewers[which - docs.size]
                            selectedSourceId = selected.id
                            isSourceReviewer = true
                            binding.tvQuizSource.text = "Source: ${selected.title}"
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    /**
     * [STEP: LOAD SPECIFIC QUIZ]
     * Loads a specific quiz from history for retaking.
     */
    private fun loadSpecificQuiz(quizId: Int) {
        val appContext = context?.applicationContext ?: return
        val currentUserId = appContext.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).getInt("current_user_id", -1)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val quiz = db.appDao().getAllQuizzesByUser(currentUserId).find { it.id == quizId }
            quiz?.let {
                val questions = QuizParser.fromJson(it.questionsJson)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        displayQuestions(questions)
                        Toast.makeText(context, "Retaking: ${it.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * [STEP: LOAD FROM DATABASE]
     * This pulls the saved quiz from the phone's memory.
     */
    private fun loadLatestQuiz() {
        val appContext = context?.applicationContext ?: return
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val quizzes = db.appDao().getAllQuizzesByUser(currentUserId)
            if (quizzes.isNotEmpty()) {
                val latest = quizzes.first()
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        processAiResponse(latest.questionsJson)
                    }
                }
            }
        }
    }

    /**
     * [STEP: AI GENERATION]
     * This talks to the Gemini AI to create a new quiz.
     */
    private fun generateQuiz() {
        val appContext = context?.applicationContext ?: return
        
        // Check internet first
        if (!NetworkHelper.isInternetAvailable(appContext)) {
            Toast.makeText(context, "Please connect to the internet", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerateQuiz.isEnabled = false

        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(appContext)
                
                // [STEP: GET SOURCE TEXT]
                val sourceText = if (selectedSourceId != -1) {
                    if (isSourceReviewer) {
                        db.appDao().getAllReviewersByUser(currentUserId).find { it.id == selectedSourceId }?.content ?: ""
                    } else {
                        db.appDao().getAllDocumentsByUser(currentUserId).find { it.id == selectedSourceId }?.extractedText ?: ""
                    }
                } else {
                    db.appDao().getLastDocumentByUser(currentUserId)?.extractedText ?: ""
                }

                // Error check: Need a document to generate a quiz!
                if (sourceText.length < 10) {
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            Toast.makeText(requireContext(), "Upload a document first!", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                            binding.btnGenerateQuiz.isEnabled = true
                        }
                    }
                    return@launch
                }

                // AI Prompt Setup
                val apiKey = AppConfig.GEMINI_API_KEY
                val prompt = """
                    Generate 5 multiple choice questions for a board exam based on this text:
                    $sourceText
                    Return ONLY a JSON array with: question, optionA, optionB, optionC, optionD, correctAnswer (A, B, C, or D).
                """.trimIndent()
                
                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )

                // Call the Gemini Service
                val response = GeminiClient.apiService.generateContent(
                    AppConfig.GEMINI_MODEL,
                    apiKey,
                    request
                )

                withContext(Dispatchers.Main) {
                    if (_binding != null && context != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnGenerateQuiz.isEnabled = true

                        if (response.isSuccessful) {
                            val jsonString = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!jsonString.isNullOrBlank()) {
                                processAiResponse(jsonString)
                                binding.questionsLayout.requestFocus()
                                Toast.makeText(requireContext(), "Quiz Successfully Generated!", Toast.LENGTH_SHORT).show()
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
                        binding.btnGenerateQuiz.isEnabled = true
                        Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * [STEP: CLEAN AI RESPONSE]
     * AI sometimes adds extra text. We clean it to find the JSON data.
     */
    private fun processAiResponse(jsonString: String) {
        try {
            val firstBracket = jsonString.indexOf("[")
            val lastBracket = jsonString.lastIndexOf("]")
            
            if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                val cleanJson = jsonString.substring(firstBracket, lastBracket + 1)
                val questions = QuizParser.fromJson(cleanJson)
                if (questions.isNotEmpty()) {
                    displayQuestions(questions)
                    Toast.makeText(requireContext(), "Quiz Generated!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "AI format error. Try again.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "AI returned invalid data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayQuestions(questions: List<QuizQuestion>) {
        currentQuestions = questions
        binding.questionsLayout.removeAllViews()
        binding.tvScore.visibility = View.GONE

        questions.forEachIndexed { index, q ->
            val qView = layoutInflater.inflate(R.layout.item_quiz_question, binding.questionsLayout, false)
            qView.findViewById<TextView>(R.id.tv_question_text).text = "${index + 1}. ${q.question}"
            qView.findViewById<RadioButton>(R.id.rb_a).text = q.optionA
            qView.findViewById<RadioButton>(R.id.rb_b).text = q.optionB
            qView.findViewById<RadioButton>(R.id.rb_c).text = q.optionC
            qView.findViewById<RadioButton>(R.id.rb_d).text = q.optionD
            binding.questionsLayout.addView(qView)
        }
    }

    private fun checkAnswers() {
        if (currentQuestions.isEmpty()) return
        var score = 0
        for (i in 0 until binding.questionsLayout.childCount) {
            val qView = binding.questionsLayout.getChildAt(i)
            val radioGroup = qView.findViewById<RadioGroup>(R.id.rg_options)
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedButton = qView.findViewById<RadioButton>(selectedId)
                val selectedIndex = radioGroup.indexOfChild(selectedButton)
                val selectedLetter = when(selectedIndex) {
                    0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> ""
                }
                if (selectedLetter == currentQuestions[i].correctAnswer) score++
            }
        }
        binding.tvScore.text = "Score: $score/${currentQuestions.size}"
        binding.tvScore.visibility = View.VISIBLE
    }

    private fun saveQuiz() {
        if (currentQuestions.isEmpty()) return
        val appContext = requireContext().applicationContext
        val currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            
            // [STEP: GET CORRECT SOURCE FOR TITLE]
            val sourceName = if (selectedSourceId != -1) {
                if (isSourceReviewer) {
                    db.appDao().getAllReviewersByUser(currentUserId).find { it.id == selectedSourceId }?.title ?: "Reviewer"
                } else {
                    db.appDao().getAllDocumentsByUser(currentUserId).find { it.id == selectedSourceId }?.fileName ?: "Document"
                }
            } else {
                db.appDao().getLastDocumentByUser(currentUserId)?.fileName ?: "Document"
            }

            val quiz = QuizEntity(
                userId = currentUserId,
                documentId = if (isSourceReviewer) 0 else selectedSourceId,
                title = "Quiz for $sourceName",
                questionsJson = QuizParser.toJson(currentQuestions)
            )
            db.appDao().insertQuiz(quiz)
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    Toast.makeText(context, "Quiz Saved! View it in 'Saved Items' to retake later.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * [STEP: ERROR HANDLING]
     * Tells the student exactly what went wrong with the AI.
     */
    private fun handleAiError(code: Int) {
        val errorMsg = when(code) {
            401 -> "Invalid API Key. Check AppConfig."
            402 -> "AI balance exhausted."
            else -> "AI Error: $code"
        }
        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
    }
}
