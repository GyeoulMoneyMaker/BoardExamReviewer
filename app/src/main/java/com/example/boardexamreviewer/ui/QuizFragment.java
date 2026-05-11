package com.example.boardexamreviewer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.boardexamreviewer.R;
import com.example.boardexamreviewer.data.*;
import com.example.boardexamreviewer.databinding.FragmentQuizBinding;
import com.example.boardexamreviewer.utils.AppConfig;
import com.example.boardexamreviewer.utils.GeminiClient;
import com.example.boardexamreviewer.utils.NetworkHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This fragment uses AI to generate questions from your document.
 */
public class QuizFragment extends Fragment {

    private FragmentQuizBinding binding;
    private List<QuizQuestion> currentQuestions = new ArrayList<>();
    private int selectedSourceId = -1;
    private boolean isSourceReviewer = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCheckAnswers.setOnClickListener(v -> checkAnswers());
        binding.btnSaveQuiz.setOnClickListener(v -> saveQuiz());
        binding.btnGenerateQuiz.setOnClickListener(v -> generateQuiz());
        binding.btnViewHistory.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.navigation_saved)
        );
        binding.btnSelectQuizSource.setOnClickListener(v -> showSourceSelectionDialog());

        int requestedQuizId = getArguments() != null ? getArguments().getInt("quizId", -1) : -1;
        if (requestedQuizId != -1) {
            loadSpecificQuiz(requestedQuizId);
        } else {
            loadLatestQuiz();
        }
    }

    private void showSourceSelectionDialog() {
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
            List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> optionsList = new ArrayList<>();
                    for (DocumentEntity d : docs) optionsList.add("[Doc] " + d.fileName);
                    for (ReviewerEntity r : reviewers) optionsList.add("[Reviewer] " + r.title);

                    if (optionsList.isEmpty()) {
                        Toast.makeText(getContext(), "No files or reviewers found!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] allOptions = optionsList.toArray(new String[0]);

                    new AlertDialog.Builder(requireContext())
                        .setTitle("Choose Quiz Source")
                        .setItems(allOptions, (dialog, which) -> {
                            if (which < docs.size()) {
                                DocumentEntity selected = docs.get(which);
                                selectedSourceId = selected.id;
                                isSourceReviewer = false;
                                binding.tvQuizSource.setText("Source: " + selected.fileName);
                            } else {
                                ReviewerEntity selected = reviewers.get(which - docs.size());
                                selectedSourceId = selected.id;
                                isSourceReviewer = true;
                                binding.tvQuizSource.setText("Source: " + selected.title);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            }
        });
    }

    private void loadSpecificQuiz(int quizId) {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<QuizEntity> quizzes = db.appDao().getAllQuizzesByUser(currentUserId);
            QuizEntity quiz = null;
            for (QuizEntity q : quizzes) {
                if (q.id == quizId) {
                    quiz = q;
                    break;
                }
            }

            if (quiz != null) {
                final QuizEntity finalQuiz = quiz;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            List<QuizQuestion> questions = QuizParser.fromJson(finalQuiz.questionsJson);
                            displayQuestions(questions);
                            Toast.makeText(getContext(), "Retaking: " + finalQuiz.title, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadLatestQuiz() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<QuizEntity> quizzes = db.appDao().getAllQuizzesByUser(currentUserId);
            if (!quizzes.isEmpty()) {
                final QuizEntity latest = quizzes.get(0);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            processAiResponse(latest.questionsJson);
                        }
                    });
                }
            }
        });
    }

    private void generateQuiz() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        if (!NetworkHelper.isInternetAvailable(appContext)) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGenerateQuiz.setEnabled(false);

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                String sourceText = "";
                if (selectedSourceId != -1) {
                    if (isSourceReviewer) {
                        List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
                        for (ReviewerEntity r : reviewers) if (r.id == selectedSourceId) { sourceText = r.content; break; }
                    } else {
                        List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
                        for (DocumentEntity d : docs) if (d.id == selectedSourceId) { sourceText = d.extractedText; break; }
                    }
                } else {
                    DocumentEntity lastDoc = db.appDao().getLastDocumentByUser(currentUserId);
                    if (lastDoc != null) sourceText = lastDoc.extractedText;
                }

                if (sourceText.length() < 10) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                Toast.makeText(getContext(), "Upload a document first!", Toast.LENGTH_SHORT).show();
                                binding.progressBar.setVisibility(View.GONE);
                                binding.btnGenerateQuiz.setEnabled(true);
                            }
                        });
                    }
                    return;
                }

                String prompt = "Generate 5 multiple choice questions for a board exam based on this text:\n" +
                                sourceText + "\n" +
                                "Return ONLY a JSON array with: question, optionA, optionB, optionC, optionD, correctAnswer (A, B, C, or D).";

                List<GeminiRequest.Part> parts = new ArrayList<>();
                parts.add(new GeminiRequest.Part(prompt));
                List<GeminiRequest.Content> contents = new ArrayList<>();
                contents.add(new GeminiRequest.Content(parts));
                GeminiRequest request = new GeminiRequest(contents);

                GeminiClient.apiService.generateContent(AppConfig.GEMINI_MODEL, AppConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeminiResponse> call, @NonNull Response<GeminiResponse> response) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (binding != null) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnGenerateQuiz.setEnabled(true);

                                    if (response.isSuccessful() && response.body() != null) {
                                        GeminiResponse body = response.body();
                                        if (body.candidates != null && !body.candidates.isEmpty() && 
                                            body.candidates.get(0).content != null && 
                                            body.candidates.get(0).content.parts != null && 
                                            !body.candidates.get(0).content.parts.isEmpty()) {
                                            
                                            String jsonString = body.candidates.get(0).content.parts.get(0).text;
                                            if (jsonString != null && !jsonString.trim().isEmpty()) {
                                                processAiResponse(jsonString);
                                                binding.questionsLayout.requestFocus();
                                                Toast.makeText(getContext(), "Quiz Successfully Generated!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    } else {
                                        handleAiError(response.code());
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GeminiResponse> call, @NonNull Throwable t) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (binding != null) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnGenerateQuiz.setEnabled(true);
                                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnGenerateQuiz.setEnabled(true);
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void processAiResponse(String jsonString) {
        try {
            int firstBracket = jsonString.indexOf("[");
            int lastBracket = jsonString.lastIndexOf("]");
            
            if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                String cleanJson = jsonString.substring(firstBracket, lastBracket + 1);
                List<QuizQuestion> questions = QuizParser.fromJson(cleanJson);
                if (!questions.isEmpty()) {
                    displayQuestions(questions);
                }
            } else {
                Toast.makeText(getContext(), "AI format error. Try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "AI returned invalid data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayQuestions(List<QuizQuestion> questions) {
        currentQuestions = questions;
        binding.questionsLayout.removeAllViews();
        binding.tvScore.setVisibility(View.GONE);

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            View qView = getLayoutInflater().inflate(R.layout.item_quiz_question, binding.questionsLayout, false);
            ((TextView) qView.findViewById(R.id.tv_question_text)).setText((i + 1) + ". " + q.question);
            ((RadioButton) qView.findViewById(R.id.rb_a)).setText(q.optionA);
            ((RadioButton) qView.findViewById(R.id.rb_b)).setText(q.optionB);
            ((RadioButton) qView.findViewById(R.id.rb_c)).setText(q.optionC);
            ((RadioButton) qView.findViewById(R.id.rb_d)).setText(q.optionD);
            binding.questionsLayout.addView(qView);
        }
    }

    private void checkAnswers() {
        if (currentQuestions.isEmpty()) return;
        int score = 0;
        for (int i = 0; i < binding.questionsLayout.getChildCount(); i++) {
            View qView = binding.questionsLayout.getChildAt(i);
            RadioGroup radioGroup = qView.findViewById(R.id.rg_options);
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedButton = qView.findViewById(selectedId);
                int selectedIndex = radioGroup.indexOfChild(selectedButton);
                String selectedLetter = "";
                switch (selectedIndex) {
                    case 0: selectedLetter = "A"; break;
                    case 1: selectedLetter = "B"; break;
                    case 2: selectedLetter = "C"; break;
                    case 3: selectedLetter = "D"; break;
                }
                if (selectedLetter.equals(currentQuestions.get(i).correctAnswer)) score++;
            }
        }
        binding.tvScore.setText("Score: " + score + "/" + currentQuestions.size());
        binding.tvScore.setVisibility(View.VISIBLE);
    }

    private void saveQuiz() {
        if (currentQuestions.isEmpty()) return;
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);
        
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            String sourceName = "Document";
            if (selectedSourceId != -1) {
                if (isSourceReviewer) {
                    List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
                    for (ReviewerEntity r : reviewers) if (r.id == selectedSourceId) { sourceName = r.title; break; }
                } else {
                    List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
                    for (DocumentEntity d : docs) if (d.id == selectedSourceId) { sourceName = d.fileName; break; }
                }
            } else {
                DocumentEntity lastDoc = db.appDao().getLastDocumentByUser(currentUserId);
                if (lastDoc != null) sourceName = lastDoc.fileName;
            }

            QuizEntity quiz = new QuizEntity(
                currentUserId,
                isSourceReviewer ? 0 : selectedSourceId,
                "Quiz for " + sourceName,
                QuizParser.toJson(currentQuestions)
            );
            db.appDao().insertQuiz(quiz);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        Toast.makeText(getContext(), "Quiz Saved! View it in 'Saved Items' to retake later.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void handleAiError(int code) {
        String message;
        switch (code) {
            case 401: message = "Invalid API Key. Check AppConfig."; break;
            case 402: message = "AI balance exhausted."; break;
            default: message = "AI Error: " + code; break;
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
