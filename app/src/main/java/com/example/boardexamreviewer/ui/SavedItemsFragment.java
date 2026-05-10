package com.example.boardexamreviewer.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.boardexamreviewer.R;
import com.example.boardexamreviewer.data.AppDatabase;
import com.example.boardexamreviewer.data.QuizEntity;
import com.example.boardexamreviewer.data.ReviewerEntity;
import com.example.boardexamreviewer.databinding.FragmentSavedBinding;
import com.example.boardexamreviewer.databinding.ItemSavedBinding;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SavedItemsFragment displays saved reviewers and quizzes.
 */
public class SavedItemsFragment extends Fragment {

    private FragmentSavedBinding binding;
    private SavedItemsAdapter adapter;
    private boolean showingReviewers = true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSavedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new SavedItemsAdapter();
        binding.rvSavedItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSavedItems.setAdapter(adapter);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingReviewers = tab.getPosition() == 0;
                loadItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadItems();
    }

    private void loadItems() {
        Context appContext = requireContext().getApplicationContext();
        int currentUserId = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1);
        
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<SavedItem> items = new ArrayList<>();
            if (showingReviewers) {
                List<ReviewerEntity> reviewers = db.appDao().getAllReviewersByUser(currentUserId);
                for (ReviewerEntity r : reviewers) items.add(new SavedItem(r.id, r.title, r.timestamp, true));
            } else {
                List<QuizEntity> quizzes = db.appDao().getAllQuizzesByUser(currentUserId);
                for (QuizEntity q : quizzes) items.add(new SavedItem(q.id, q.title, q.timestamp, false));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        adapter.submitList(items);
                    }
                });
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

    private void onActionClicked(SavedItem item) {
        Bundle bundle = new Bundle();
        if (item.isReviewer) {
            bundle.putInt("reviewerId", item.id);
            NavHostFragment.findNavController(this).navigate(R.id.navigation_reviewer, bundle);
        } else {
            bundle.putInt("quizId", item.id);
            NavHostFragment.findNavController(this).navigate(R.id.navigation_quiz, bundle);
        }
    }

    // --- Helper Class ---
    static class SavedItem {
        final int id;
        final String title;
        final long timestamp;
        final boolean isReviewer;

        SavedItem(int id, String title, long timestamp, boolean isReviewer) {
            this.id = id;
            this.title = title;
            this.timestamp = timestamp;
            this.isReviewer = isReviewer;
        }
    }

    // --- Adapter Class ---
    class SavedItemsAdapter extends RecyclerView.Adapter<SavedItemsAdapter.ViewHolder> {
        private List<SavedItem> items = new ArrayList<>();

        void submitList(List<SavedItem> newList) {
            items = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSavedBinding binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
            private final ItemSavedBinding binding;

            ViewHolder(ItemSavedBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(SavedItem item) {
                binding.tvTitle.setText(item.title);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault());
                binding.tvDate.setText(sdf.format(new Date(item.timestamp)));
                
                binding.btnAction.setText(item.isReviewer ? "View" : "Retake");
                binding.btnAction.setOnClickListener(v -> onActionClicked(item));
            }
        }
    }
}
