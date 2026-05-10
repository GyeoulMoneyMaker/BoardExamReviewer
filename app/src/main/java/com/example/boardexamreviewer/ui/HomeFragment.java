package com.example.boardexamreviewer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.boardexamreviewer.R;
import com.example.boardexamreviewer.data.AppDatabase;
import com.example.boardexamreviewer.data.DocumentEntity;
import com.example.boardexamreviewer.databinding.FragmentHomeBinding;
import com.example.boardexamreviewer.utils.DocumentExtractor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HomeFragment is the first screen users see.
 * It contains buttons to upload documents and navigate to other parts of the app.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    handleSelectedFile(uri);
                }
            }
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnUpload.setOnClickListener(v -> openFilePicker());
        
        binding.btnViewFiles.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.navigation_files)
        );

        binding.btnGenReviewer.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.navigation_reviewer)
        );

        binding.btnGenQuiz.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.navigation_quiz)
        );

        binding.btnPomodoro.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.navigation_pomodoro)
        );
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {
            "application/pdf", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void handleSelectedFile(Uri uri) {
        Context appContext = requireContext().getApplicationContext();
        binding.progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                // 1. Extract Text
                String text = DocumentExtractor.extractText(appContext, uri);
                String fileName = DocumentExtractor.getFileName(appContext, uri);

                // 2. Save to Database
                AppDatabase db = AppDatabase.getDatabase(appContext);
                SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                int currentUserId = prefs.getInt("current_user_id", -1);

                DocumentEntity doc = new DocumentEntity(
                    currentUserId,
                    0,
                    fileName, 
                    uri.toString(), 
                    text
                );
                db.appDao().insertDocument(doc);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Uploaded to Profile: " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
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
