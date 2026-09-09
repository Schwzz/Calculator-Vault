package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.VaultFileType
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultBackground
import com.example.ui.vault.VaultBrowserScreen
import com.example.ui.vault.VaultDashboardScreen
import com.example.ui.vault.VaultDownloadsScreen
import com.example.ui.vault.VaultFilesScreen
import com.example.ui.vault.VaultMediaPlayerDialog
import com.example.ui.vault.VaultNotesScreen
import com.example.ui.vault.VaultSettingsScreen
import com.example.ui.vault.VaultTrashScreen
import com.example.ui.vault.VaultViewModel

import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController

class MainActivity : ComponentActivity() {
    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private var navControllerRef: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply initial Window FLAG_SECURE based on security preferences
        val initialHideRecents = vaultViewModel.prefs.hideRecentsPreview
        val initialBlockScreenshots = vaultViewModel.prefs.blockScreenshots
        if (initialHideRecents || initialBlockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Auto-Reset to Calculator on Exit observer
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (vaultViewModel.prefs.resetOnExit) {
                    calculatorViewModel.resetKeypad()
                    calculatorViewModel.refreshState()
                    navControllerRef?.let { nav ->
                        if (nav.currentDestination?.route != "calculator") {
                            nav.navigate("calculator") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
        })

        setContent {
            val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

            // Dynamic FLAG_SECURE: Actively react to hideRecents or blockScreenshots state
            androidx.compose.runtime.LaunchedEffect(vaultUiState.hideRecentsPreview, vaultUiState.blockScreenshots) {
                val isHideRecentsEnabled = vaultUiState.hideRecentsPreview
                val isBlockScreenshotsEnabled = vaultUiState.blockScreenshots
                if (isHideRecentsEnabled || isBlockScreenshotsEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MyApplicationTheme(accentIndex = vaultUiState.accentIndex) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VaultBackground
                ) {
                    val navController = rememberNavController()
                    navControllerRef = navController

                    NavHost(
                        navController = navController,
                        startDestination = "calculator"
                    ) {
                        // Calculator Disguise
                        composable("calculator") {
                            CalculatorScreen(
                                viewModel = calculatorViewModel,
                                onNavigateToVault = {
                                    navController.navigate("vault_dashboard") {
                                        popUpTo("calculator") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Vault Dashboard
                        composable("vault_dashboard") {
                            VaultDashboardScreen(
                                viewModel = vaultViewModel,
                                onNavigate = { route ->
                                    navController.navigate(route)
                                },
                                onLockApp = {
                                    calculatorViewModel.resetKeypad()
                                    navController.navigate("calculator") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Category Files (PHOTO, VIDEO, AUDIO, FILE)
                        composable(
                            route = "vault_files/{category}",
                            arguments = listOf(navArgument("category") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val categoryStr = backStackEntry.arguments?.getString("category") ?: "FILE"
                            val categoryType = try {
                                VaultFileType.valueOf(categoryStr)
                            } catch (e: Exception) {
                                VaultFileType.FILE
                            }

                            VaultFilesScreen(
                                categoryType = categoryType,
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Private Notes
                        composable("vault_notes") {
                            VaultNotesScreen(
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Brave-style Browser with Snaptube Video Sniffer
                        composable("vault_browser") {
                            VaultBrowserScreen(
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenDownloads = { navController.navigate("vault_downloads") }
                            )
                        }

                        // Download Manager
                        composable("vault_downloads") {
                            VaultDownloadsScreen(
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Trash Bin
                        composable("vault_trash") {
                            VaultTrashScreen(
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Settings & Customization
                        composable("vault_settings") {
                            VaultSettingsScreen(
                                viewModel = vaultViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onLockApp = {
                                    calculatorViewModel.resetKeypad()
                                    navController.navigate("calculator") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    // Global Vault Media Player
                    vaultUiState.currentPlayingItem?.let { playingItem ->
                        VaultMediaPlayerDialog(
                            item = playingItem,
                            onDismiss = { vaultViewModel.closeMediaPlayer() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (vaultViewModel.prefs.resetOnExit) {
            calculatorViewModel.resetKeypad()
            calculatorViewModel.refreshState()
            try {
                navControllerRef?.let { nav ->
                    if (nav.currentDestination?.route != "calculator") {
                        nav.navigate("calculator") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (vaultViewModel.prefs.resetOnExit) {
            navControllerRef?.let { nav ->
                if (nav.currentDestination != null && nav.currentDestination?.route != "calculator") {
                    calculatorViewModel.resetKeypad()
                    calculatorViewModel.refreshState()
                    try {
                        nav.navigate("calculator") {
                            popUpTo(0) { inclusive = true }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
