package com.example.boardexamreviewer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.boardexamreviewer.data.AppDatabase;
import com.example.boardexamreviewer.data.DocumentEntity;
import com.example.boardexamreviewer.databinding.FragmentFilesBinding;
import com.example.boardexamreviewer.databinding.ItemFileBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FilesFragment shows a list of all documents you have uploaded.
 */
public class FilesFragment extends Fragment {

    private FragmentFilesBinding binding;
    private FilesAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new FilesAdapter(
            this::showExtractedText,
            this::deleteDocument
        );

        binding.rvFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFiles.setAdapter(adapter);

        loadDocuments();
    }

    private void loadDocuments() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        
        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("current_user_id", -1);

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                List<DocumentEntity> docs = db.appDao().getAllDocumentsByUser(currentUserId);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            adapter.updateList(docs);
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void showExtractedText(DocumentEntity doc) {
        ScrollView scrollView = new ScrollView(requireContext());
        TextView textView = new TextView(requireContext());
        textView.setText(doc.extractedText);
        textView.setPadding(40, 40, 40, 40);
        textView.setTextSize(16f);
        scrollView.addView(textView);

        new AlertDialog.Builder(requireContext())
            .setTitle(doc.fileName)
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show();
    }

    private void deleteDocument(DocumentEntity doc) {
        Context appContext = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete " + doc.fileName + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                executorService.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(appContext);
                    db.appDao().deleteDocument(doc);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadDocuments);
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
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

    // --- Adapter Class ---
    static class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
        private final List<DocumentEntity> items = new ArrayList<>();
        private final OnItemClickListener onItemClick;
        private final OnItemClickListener onDeleteClick;

        interface OnItemClickListener {
            void onClick(DocumentEntity doc);
        }

        FilesAdapter(OnItemClickListener onItemClick, OnItemClickListener onDeleteClick) {
            this.onItemClick = onItemClick;
            this.onDeleteClick = onDeleteClick;
        }

        void updateList(List<DocumentEntity> newList) {
            items.clear();
            items.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemFileBinding binding = ItemFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemFileBinding binding;

            ViewHolder(ItemFileBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(DocumentEntity item) {
                binding.tvFileName.setText(item.fileName);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault());
                binding.tvFileDate.setText(sdf.format(new Date(item.timestamp)));

                binding.getRoot().setOnClickListener(v -> onItemClick.onClick(item));
                binding.btnDelete.setOnClickListener(v -> onDeleteClick.onClick(item));
            }
        }
    }
}
