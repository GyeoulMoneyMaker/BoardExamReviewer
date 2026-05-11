package com.example.boardexamreviewer.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.boardexamreviewer.data.AppDatabase;
import com.example.boardexamreviewer.data.UserEntity;
import com.example.boardexamreviewer.databinding.FragmentProfileBinding;
import com.example.boardexamreviewer.databinding.ItemProfileBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This fragment allows users to "log in" by selecting or creating a profile.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadProfiles();

        binding.btnAddProfile.setOnClickListener(v -> showAddProfileDialog());
    }

    private void setupRecyclerView() {
        adapter = new ProfileAdapter(this::selectUser);
        binding.rvProfiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProfiles.setAdapter(adapter);
    }

    private void loadProfiles() {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            List<UserEntity> users = db.appDao().getAllUsers();
            int currentUserId = appContext
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getInt("current_user_id", -1);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        adapter.submitList(users, currentUserId);
                    }
                });
            }
        });
    }

    private void showAddProfileDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Name");
        layout.addView(nameInput);

        EditText passInput = new EditText(requireContext());
        passInput.setHint("Password");
        passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passInput);
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Create Secure Profile")
            .setView(layout)
            .setPositiveButton("Create", (dialog, which) -> {
                String name = nameInput.getText().toString();
                String pass = passInput.getText().toString();
                if (!name.trim().isEmpty() && !pass.trim().isEmpty()) {
                    createNewProfile(name, pass);
                } else {
                    Toast.makeText(getContext(), "Name and Password required!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createNewProfile(String name, String pass) {
        Context appContext = requireContext().getApplicationContext();
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            UserEntity newUser = new UserEntity(name, pass, 0);
            db.appDao().insertUser(newUser);
            loadProfiles();
        });
    }

    private void selectUser(UserEntity user) {
        EditText input = new EditText(requireContext());
        input.setHint("Enter password for " + user.name);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext())
            .setTitle("Login")
            .setView(input)
            .setPositiveButton("Login", (dialog, which) -> {
                String pass = input.getText().toString();
                if (pass.equals(user.password)) {
                    performLogin(user);
                } else {
                    Toast.makeText(getContext(), "Wrong password!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performLogin(UserEntity user) {
        requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("current_user_id", user.id)
            .putString("current_user_name", user.name)
            .apply();
        
        Toast.makeText(getContext(), "Access Granted: " + user.name, Toast.LENGTH_SHORT).show();
        loadProfiles();
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
    static class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
        private final List<UserEntity> users = new ArrayList<>();
        private int currentUserId = -1;
        private final OnUserSelectedListener listener;

        interface OnUserSelectedListener {
            void onUserSelected(UserEntity user);
        }

        ProfileAdapter(OnUserSelectedListener listener) {
            this.listener = listener;
        }

        void submitList(List<UserEntity> newList, int selectedId) {
            users.clear();
            users.addAll(newList);
            currentUserId = selectedId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemProfileBinding binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemProfileBinding binding;

            ViewHolder(ItemProfileBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(UserEntity user) {
                binding.tvUserName.setText(user.name);
                binding.ivCheck.setVisibility(user.id == currentUserId ? View.VISIBLE : View.GONE);
                binding.getRoot().setOnClickListener(v -> listener.onUserSelected(user));
            }
        }
    }
}
