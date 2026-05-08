package com.example.intelligentfish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.intelligentfish.navigation.NavGraph
import com.example.intelligentfish.navigation.Screen
import com.example.intelligentfish.navigation.bottomNavItems
import com.example.intelligentfish.ui.theme.IntelligentFishTheme
import com.example.intelligentfish.viewmodel.FishTankViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "WiFi scan needs location permission", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        enableEdgeToEdge()
        setContent {
            IntelligentFishTheme {
                val navController = rememberNavController()
                val viewModel: FishTankViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collect { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            bottomNavItems.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Text(screen.icon, style = MaterialTheme.typography.titleMedium) },
                                    label = { Text(screen.label) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }
}
