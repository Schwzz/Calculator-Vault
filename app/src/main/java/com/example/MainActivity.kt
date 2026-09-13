package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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

    private fun updateWindowSecurityFlags(hideRecents: Boolean, blockScreenshots: Boolean) {
        val isSecureNeeded = hideRecents || blockScreenshots
        // In debug / streaming preview emulator environments, SurfaceFlinger blanks out WebRTC screen capture into black if FLAG_SECURE is applied.
        // Therefore, we bypass FLAG_SECURE in debug mode to ensure emulator preview renders properly.
        if (!BuildConfig.DEBUG && isSecureNeeded) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

            // Dynamic FLAG_SECURE: Actively react to hideRecents or blockScreenshots state
            androidx.compose.runtime.LaunchedEffect(vaultUiState.hideRecentsPreview, vaultUiState.blockScreenshots) {
                updateWindowSecurityFlags(vaultUiState.hideRecentsPreview, vaultUiState.blockScreenshots)
            }

            MyApplicationTheme(
                accentIndex = vaultUiState.accentIndex,
                cornerStyle = vaultUiState.cornerStyle
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
                            viewModel = vaultViewModel,
                            onDismiss = { vaultViewModel.closeMediaPlayer() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (vaultViewModel.prefs.resetOnExit && !vaultViewModel.isAwaitingExternalActivity) {
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
        // Ensure FLAG_SECURE is re-verified and active whenever the app is brought back into foreground
        updateWindowSecurityFlags(
            hideRecents = vaultViewModel.prefs.hideRecentsPreview,
            blockScreenshots = vaultViewModel.prefs.blockScreenshots
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
