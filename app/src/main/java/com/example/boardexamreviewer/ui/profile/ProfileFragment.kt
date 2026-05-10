package com.example.boardexamreviewer.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.data.UserEntity
import com.example.boardexamreviewer.databinding.FragmentProfileBinding
import com.example.boardexamreviewer.databinding.ItemProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [SUB-MODULE: PROFILE]
 * This fragment allows users to "log in" by selecting or creating a profile.
 * Label: PROFILE_MODULE
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProfileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadProfiles()

        binding.btnAddProfile.setOnClickListener {
            showAddProfileDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProfileAdapter { user ->
            selectUser(user)
        }
        binding.rvProfiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProfiles.adapter = adapter
    }

    private fun loadProfiles() {
        val appContext = context?.applicationContext ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val users = db.appDao().getAllUsers()
            val currentUserId = appContext
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getInt("current_user_id", -1)

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    adapter.submitList(users, currentUserId)
                }
            }
        }
    }

    private fun showAddProfileDialog() {
        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val nameInput = EditText(requireContext())
        nameInput.hint = "Name"
        layout.addView(nameInput)

        val passInput = EditText(requireContext())
        passInput.hint = "Password"
        passInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(passInput)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Create Secure Profile")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString()
                val pass = passInput.text.toString()
                if (name.isNotBlank() && pass.isNotBlank()) {
                    createNewProfile(name, pass)
                } else {
                    Toast.makeText(context, "Name and Password required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewProfile(name: String, pass: String) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            val newUser = UserEntity(name = name, password = pass)
            db.appDao().insertUser(newUser)
            loadProfiles()
        }
    }

    private fun selectUser(user: UserEntity) {
        // [STEP: AUTHENTICATION]
        val input = EditText(requireContext())
        input.hint = "Enter password for ${user.name}"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(requireContext())
            .setTitle("Login")
            .setView(input)
            .setPositiveButton("Login") { _, _ ->
                val pass = input.text.toString()
                if (pass == user.password) {
                    performLogin(user)
                } else {
                    Toast.makeText(context, "Wrong password!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogin(user: UserEntity) {
        requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("current_user_id", user.id)
            .putString("current_user_name", user.name)
            .apply()
        
        Toast.makeText(context, "Access Granted: ${user.name}", Toast.LENGTH_SHORT).show()
        loadProfiles() // Refresh checkmark
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ProfileAdapter(private val onUserSelected: (UserEntity) -> Unit) :
    RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    private var users = listOf<UserEntity>()
    private var currentUserId = -1

    fun submitList(newList: List<UserEntity>, selectedId: Int) {
        users = newList
        currentUserId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class ViewHolder(private val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserEntity) {
            binding.tvUserName.text = user.name
            binding.ivCheck.visibility = if (user.id == currentUserId) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onUserSelected(user) }
        }
    }
}
