package com.example.ui.vault

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch

data class ResolutionOption(
    val label: String,
    val resolution: String,
    val sizeText: String,
    val estimatedBytes: Long,
    val isAudio: Boolean = false
)

val RESOLUTION_OPTIONS = listOf(
    ResolutionOption("1080p Full HD", "1080p", "48.5 MB", 48_500_000L),
    ResolutionOption("720p HD", "720p", "24.2 MB", 24_200_000L),
    ResolutionOption("480p SD", "480p", "12.8 MB", 12_800_000L),
    ResolutionOption("360p Standard", "360p", "7.4 MB", 7_400_000L),
    ResolutionOption("MP3 Audio Only", "Audio (320kbps)", "4.1 MB", 4_100_000L, isAudio = true)
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VaultBrowserScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val vaultUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val browserSessionManager = viewModel.browserSessionManager
    val tabs = browserSessionManager.tabs
    val activeTabIndex = browserSessionManager.activeTabIndex
    val activeTab = browserSessionManager.activeTab
    val context = LocalContext.current

    var inputUrl by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var isInputFocused by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cornerStyle = LocalVaultCornerStyle.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Synchronize address bar input text with active tab URL when not focused by the user
    LaunchedEffect(activeTab.url, isInputFocused) {
        if (!isInputFocused) {
            inputUrl = activeTab.url
        }
    }

    // Lifecycle observer: pause active WebView when app/screen is paused or backgrounded,
    // detach from view hierarchy on disposal to avoid memory leaks while preserving session
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activeTab.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                browserSessionManager.pauseActiveWebView()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                browserSessionManager.resumeActiveWebView()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            browserSessionManager.pauseActiveWebView()
            browserSessionManager.detachWebView(activeTab)
        }
    }

    fun navigateTo(urlOrQuery: String) {
        val trimmed = urlOrQuery.trim()
        if (trimmed.isEmpty()) return

        val searchPrefix = when (vaultUiState.searchEngine.lowercase()) {
            "duckduckgo" -> "https://duckduckgo.com/?q="
            "brave" -> "https://search.brave.com/search?q="
            else -> "https://www.google.com/search?q="
        }

        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "$searchPrefix${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }
        activeTab.url = targetUrl
        inputUrl = targetUrl
        activeTab.hasError = false
        activeTab.errorMessage = null
        keyboardController?.hide()
        focusManager.clearFocus()

        val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
        wv.loadUrl(targetUrl)
    }

    fun goToHome() {
        activeTab.url = ""
        activeTab.title = "New Tab"
        activeTab.favicon = null
        activeTab.detectedVideoUrl = null
        activeTab.hasError = false
        activeTab.errorMessage = null
        activeTab.canGoBack = false
        activeTab.canGoForward = false
        inputUrl = ""
        keyboardController?.hide()
        focusManager.clearFocus()
        val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
        wv.loadUrl("about:blank")
    }

    fun reloadCurrentPage() {
        activeTab.hasError = false
        activeTab.errorMessage = null
        val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
        wv.reload()
    }

    // Natural browser back navigation:
    // 1. Exit fullscreen video if active
    // 2. Dismiss open picker/switcher dialogs
    // 3. Step back in WebView history
    // 4. Leave Browser screen only when no WebView history remains
    BackHandler {
        val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
        if (activeTab.customVideoView != null) {
            activeTab.customViewCallback?.onCustomViewHidden()
            activeTab.customVideoView = null
            activeTab.customViewCallback = null
        } else if (showResolutionPicker) {
            showResolutionPicker = false
        } else if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (wv.canGoBack()) {
            wv.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (activeTab.customVideoView == null) {
                Surface(
                    color = VaultCardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("browser_back_to_vault_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Vault",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Search & URL Input Field
                            BasicTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        navigateTo(inputUrl)
                                    },
                                    onGo = {
                                        navigateTo(inputUrl)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isInputFocused = it.isFocused }
                                    .testTag("browser_search_input"),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(21.dp))
                                            .background(VaultBackground)
                                            .border(
                                                width = 1.dp,
                                                color = if (isInputFocused) MaterialTheme.colorScheme.primary else VaultCardBorder,
                                                shape = RoundedCornerShape(21.dp)
                                            )
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val fav = activeTab.favicon
                                        if (fav != null && !isInputFocused && activeTab.url.isNotEmpty()) {
                                            Image(
                                                bitmap = fav.asImageBitmap(),
                                                contentDescription = "Favicon",
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .testTag("browser_favicon")
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = if (isInputFocused) MaterialTheme.colorScheme.primary else VaultTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (inputUrl.isEmpty()) {
                                                Text(
                                                    text = "Search or enter address",
                                                    color = VaultTextSecondary,
                                                    fontSize = 12.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }

                                        if (inputUrl.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    inputUrl = ""
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = VaultTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Tab Counter Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .clickable { showTabSwitcher = true }
                                    .testTag("browser_tab_switcher_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabs.size.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onOpenDownloads,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("browser_downloads_button")
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Downloads",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { showClearDataConfirm = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("browser_clear_data_button")
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Clear Browser Data",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (activeTab.pageProgress < 1f && activeTab.url.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { activeTab.pageProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (activeTab.customVideoView == null && activeTab.url.isNotEmpty() && !isInputFocused) {
                Surface(
                    color = VaultCardBackground,
                    border = BorderStroke(1.dp, VaultCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
                                if (wv.canGoBack()) wv.goBack()
                            },
                            enabled = activeTab.canGoBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (activeTab.canGoBack) Color.White else Color(0xFF555555)
                            )
                        }

                        IconButton(
                            onClick = {
                                val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
                                if (wv.canGoForward()) wv.goForward()
                            },
                            enabled = activeTab.canGoForward
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (activeTab.canGoForward) Color.White else Color(0xFF555555)
                            )
                        }

                        IconButton(
                            onClick = { goToHome() }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                        }

                        IconButton(
                            onClick = { reloadCurrentPage() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live WebView - Viewport is naturally placed between Scaffold's topBar and bottomBar
            AndroidView(
                factory = { ctx ->
                    browserSessionManager.getOrCreateWebView(activeTab, ctx)
                },
                update = {
                    // Session and state retention is handled by BrowserSessionManager
                },
                modifier = Modifier.fillMaxSize()
            )

            // Graceful browser error state when network or connection fails
            if (activeTab.hasError && activeTab.url.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultBackground)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Unable to Load Page",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeTab.errorMessage ?: "The webpage could not be loaded. Please check your connection and try again.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { reloadCurrentPage() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = cornerStyle.buttonShape
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }

                        Button(
                            onClick = { goToHome() },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultCardBackground),
                            border = BorderStroke(1.dp, VaultCardBorder),
                            shape = cornerStyle.buttonShape
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Go to Home", color = Color.White)
                        }
                    }
                }
            }

            // Minimal, elegant private browser start page (displayed when active tab URL is empty)
            if (activeTab.url.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Private Browser",
                        color = VaultTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Incognito search and secure web downloads",
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Clean Search suggestions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Google", "YouTube", "Wikipedia").forEach { site ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VaultCardBackground)
                                    .border(1.dp, VaultCardBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        when (site) {
                                            "Google" -> navigateTo("https://www.google.com")
                                            "YouTube" -> navigateTo("https://www.youtube.com")
                                            "Wikipedia" -> navigateTo("https://www.wikipedia.org")
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = site,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Floating Video Downloader Button
            AnimatedVisibility(
                visible = activeTab.detectedVideoUrl != null && activeTab.url.isNotEmpty() && !activeTab.hasError,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showResolutionPicker = true }
                        .testTag("snaptube_download_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download Video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Custom Fullscreen Web Video container (renders over full screen with black backdrop)
    if (activeTab.customVideoView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { activeTab.customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Resolution Picker Overlay Dialog
    if (showResolutionPicker) {
        Dialog(onDismissRequest = { showResolutionPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Resolution",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showResolutionPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Text(
                        text = activeTab.detectedVideoTitle,
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Video Resolutions",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RESOLUTION_OPTIONS.forEach { opt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.startVideoDownload(
                                        title = activeTab.detectedVideoTitle,
                                        url = activeTab.detectedVideoUrl ?: "https://example.com/stream.mp4",
                                        resolution = opt.label,
                                        estimatedBytes = opt.estimatedBytes
                                    )
                                    showResolutionPicker = false
                                }
                                .testTag("resolution_option_${opt.resolution}"),
                            colors = CardDefaults.cardColors(containerColor = VaultBackground),
                            border = BorderStroke(1.dp, VaultCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (opt.isAudio) Color(0xFFFBBF24).copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = opt.resolution,
                                            color = if (opt.isAudio) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = opt.label,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = opt.sizeText,
                                    color = VaultTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Multi-Tab Switcher Dialog
    if (showTabSwitcher) {
        Dialog(onDismissRequest = { showTabSwitcher = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open Tabs (${tabs.size})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showTabSwitcher = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs) { tab ->
                            val index = tabs.indexOf(tab)
                            val isActive = index == activeTabIndex
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        browserSessionManager.selectTab(index)
                                        inputUrl = tab.url
                                        showTabSwitcher = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = VaultBackground),
                                border = BorderStroke(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else VaultCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val tabFav = tab.favicon
                                    if (tabFav != null) {
                                        Image(
                                            bitmap = tabFav.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = VaultTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url.ifEmpty { "New Tab" },
                                            color = VaultTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (tabs.size > 1) {
                                        IconButton(
                                            onClick = {
                                                browserSessionManager.closeTab(index)
                                                inputUrl = browserSessionManager.activeTab.url
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            browserSessionManager.openNewTab()
                            inputUrl = ""
                            showTabSwitcher = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_tab_button"),
                        shape = cornerStyle.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open New Tab", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Clear Browser Data Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = {
                Text(
                    text = "Clear All Browser Data?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete all cookies, web storage, browser cache, and browsing history permanently.",
                    color = VaultTextSecondary,
                    fontSize = 14.sp
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        browserSessionManager.clearAllData {
                            inputUrl = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("Browser cache & history cleared")
                            }
                        }
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = cornerStyle.buttonShape,
                    modifier = Modifier.testTag("confirm_clear_browser_data_button")
                ) {
                    Text("Clear All Data", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataConfirm = false },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }
}
