package com.example.boardexamreviewer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.boardexamreviewer.data.*;
import com.example.boardexamreviewer.databinding.FragmentReviewerBinding;
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
 * [SUB-MODULE: AI REVIEWER GENERATOR]
 * This fragment generates a study summary (Reviewer) from your document.
 */
public class ReviewerFragment extends Fragment {

    private FragmentReviewerBinding binding;
    private int selectedDocumentId = -1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReviewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSave.setOnClickListener(v -> saveReviewer());
        binding.btnRegenerate.setOnClickListener(v -> generateReviewer());
        binding.btnSelectSource.setOnClickListener(v -> showSourceSelectionDialog());

        int requestedReviewerId = getArguments() != null ? getArguments().getInt("reviewerId", -1) : -1;
        if (requestedReviewerId != -1) {
            loadSpecificReviewer(requestedReviewerId);
        } else {
            loadLatestReviewer();
        }
    }

    private void showSourceSelectionDialog() {
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (docs.isEmpty()) {
                        Toast.makeText(getContext(), "No files uploaded yet!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] fileNames = new String[docs.size()];
                    for (int i = 0; i < docs.size(); i++) {
                        fileNames[i] = docs.get(i).fileName;
                    }

                    new AlertDialog.Builder(requireContext())
                        .setTitle("Select Source Document")
                        .setItems(fileNames, (dialog, which) -> {
                            DocumentEntity selected = docs.get(which);
                            selectedDocumentId = selected.id;
                            binding.tvCurrentSource.setText("Source: " + selected.fileName);
                            Toast.makeText(getContext(), "Selected: " + selected.fileName, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            }
        });
    }

    private void loadSpecificReviewer(int reviewerId) {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
            ReviewerEntity reviewer = null;
            for (ReviewerEntity r : reviewers) {
                if (r.id == reviewerId) {
                    reviewer = r;
                    break;
                }
            }

            if (reviewer != null) {
                final ReviewerEntity finalReviewer = reviewer;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.etReviewerContent.setText(finalReviewer.content);
                            Toast.makeText(getContext(), "Viewing: " + finalReviewer.title, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadLatestReviewer() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
            if (!reviewers.isEmpty()) {
                final ReviewerEntity latest = reviewers.get(0);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.etReviewerContent.setText(latest.content);
                        }
                    });
                }
            }
        });
    }

    private void generateReviewer() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        if (!NetworkHelper.isInternetAvailable(appContext)) {
            Toast.makeText(getContext(), "Check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegenerate.setEnabled(false);

        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                DocumentEntity docSource;
                if (selectedDocumentId != -1) {
                    List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
                    docSource = null;
                    for (DocumentEntity d : docs) {
                        if (d.id == selectedDocumentId) {
                            docSource = d;
                            break;
                        }
                    }
                } else {
                    docSource = db.appDao().getLastDocumentByUser(currentUserId);
                }

                if (docSource == null || docSource.extractedText.length() < 10) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                Toast.makeText(getContext(), "Upload a document first!", Toast.LENGTH_SHORT).show();
                                binding.progressBar.setVisibility(View.GONE);
                                binding.btnRegenerate.setEnabled(true);
                            }
                        });
                    }
                    return;
                }

                String prompt = "Based on the following text, generate a comprehensive reviewer summary for a board exam:\n\n" + docSource.extractedText;
                
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
                                    binding.btnRegenerate.setEnabled(true);

                                    if (response.isSuccessful() && response.body() != null) {
                                        GeminiResponse body = response.body();
                                        if (body.candidates != null && !body.candidates.isEmpty() && 
                                            body.candidates.get(0).content != null && 
                                            body.candidates.get(0).content.parts != null && 
                                            !body.candidates.get(0).content.parts.isEmpty()) {
                                            
                                            String result = body.candidates.get(0).content.parts.get(0).text;
                                            if (result != null && !result.trim().isEmpty()) {
                                                binding.etReviewerContent.setText(result);
                                                binding.etReviewerContent.requestFocus();
                                                Toast.makeText(getContext(), "Reviewer Successfully Generated!", Toast.LENGTH_SHORT).show();
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
                                    binding.btnRegenerate.setEnabled(true);
                                    Toast.makeText(getContext(), "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                            binding.btnRegenerate.setEnabled(true);
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void saveReviewer() {
        String content = binding.etReviewerContent.getText().toString();
        if (content.trim().isEmpty()) return;

        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);
        
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            DocumentEntity docSource;
            if (selectedDocumentId != -1) {
                List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
                docSource = null;
                for (DocumentEntity d : docs) {
                    if (d.id == selectedDocumentId) {
                        docSource = d;
                        break;
                    }
                }
            } else {
                docSource = db.appDao().getLastDocumentByUser(currentUserId);
            }

            ReviewerEntity reviewer = new ReviewerEntity(
                currentUserId,
                docSource != null ? docSource.id : 0,
                "Reviewer for " + (docSource != null ? docSource.fileName : "Document"),
                content
            );
            db.appDao().insertReviewer(reviewer);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        Toast.makeText(getContext(), "Reviewer saved!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void handleAiError(int code) {
        String message;
        switch (code) {
            case 401: message = "API Key is invalid. Please double-check your AppConfig."; break;
            case 402: message = "Google Balance Exhausted. Please top up your Gemini account."; break;
            case 404: message = "Model Not Found. I will fix the model name for you."; break;
            case 429: message = "Too many requests! Wait 30 seconds and try again."; break;
            case 503: message = "Google's servers are overloaded (503). Wait 10 seconds and click Generate again!"; break;
            default: message = "AI Connection Error (" + code + "). Check your internet!"; break;
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
