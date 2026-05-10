package com.example.boardexamreviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.boardexamreviewer.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // [STEP: TOOLBAR]
        setSupportActionBar(binding.toolbar)

        val navView: BottomNavigationView = binding.bottomNavigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
        
        navHostFragment?.let { host ->
            val navController = host.navController
            navView.setupWithNavController(navController)
            
            // Link ActionBar to NavController for automatic title updates
            setupActionBarWithNavController(navController, AppBarConfiguration(navView.menu))
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_switch_profile -> {
                // Navigate to profile fragment
                findNavController(R.id.nav_host_fragment).navigate(R.id.navigation_profile)
                true
            }
            android.R.id.home -> {
                findNavController(R.id.nav_host_fragment).navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
