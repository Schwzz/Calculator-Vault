package com.example.ui.vault

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
import com.example.model.BrowserHistoryItem
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultCornerStyle
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

data class QuickShortcut(
    val title: String,
    val url: String,
    val initial: String,
    val color: Color
)

val QUICK_SHORTCUTS = listOf(
    QuickShortcut("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
    QuickShortcut("YouTube", "https://www.youtube.com", "Y", Color(0xFFFF0000)),
    QuickShortcut("Wikipedia", "https://www.wikipedia.org", "W", Color(0xFF9E9E9E)),
    QuickShortcut("Reddit", "https://www.reddit.com", "R", Color(0xFFFF4500)),
    QuickShortcut("DuckDuckGo", "https://duckduckgo.com", "D", Color(0xFFDE5833)),
    QuickShortcut("GitHub", "https://github.com", "G", Color(0xFF6E5494)),
    QuickShortcut("BBC News", "https://www.bbc.com/news", "B", Color(0xFFBB1919)),
    QuickShortcut("X / Twitter", "https://twitter.com", "X", Color(0xFF1DA1F2))
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VaultBrowserScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val vaultUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val browserHistory by viewModel.browserHistory.collectAsStateWithLifecycle()
    val browserSessionManager = viewModel.browserSessionManager
    val tabs = browserSessionManager.tabs
    val activeTabIndex = browserSessionManager.activeTabIndex
    val activeTab = browserSessionManager.activeTab
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var inputUrl by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var isInputFocused by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSslDialog by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cornerStyle = LocalVaultCornerStyle.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        browserSessionManager.resumeActiveWebView()
        wv.loadUrl(targetUrl)
        wv.requestFocus()
        wv.invalidate()
    }

    if (showTabSwitcher) {
        BrowserTabOverviewScreen(
            browserSessionManager = browserSessionManager,
            cornerStyle = cornerStyle,
            onClose = { showTabSwitcher = false },
            onSelectTab = { index ->
                browserSessionManager.selectTab(index)
                inputUrl = browserSessionManager.activeTab.url
                showTabSwitcher = false
            },
            onNewTab = { isIncognito ->
                browserSessionManager.openNewTab(isIncognito = isIncognito)
                inputUrl = ""
                showTabSwitcher = false
            },
            onCloseTab = { index ->
                browserSessionManager.closeTab(index)
                inputUrl = browserSessionManager.activeTab.url
            },
            onCloseAllTabs = {
                browserSessionManager.closeAllTabs()
                inputUrl = ""
            }
        )
        return
    }

    if (showHistoryDialog) {
        BrowserHistoryScreen(
            history = browserHistory,
            cornerStyle = cornerStyle,
            onClose = { showHistoryDialog = false },
            onOpenUrl = { url ->
                showHistoryDialog = false
                navigateTo(url)
            },
            onDeleteEntry = { id -> viewModel.deleteBrowserHistoryItem(id) },
            onClearAll = { viewModel.clearBrowserHistory() }
        )
        return
    }

    // Synchronize address bar input text with active tab URL when not actively typed into
    LaunchedEffect(activeTab.url, isInputFocused) {
        if (!isInputFocused) {
            inputUrl = activeTab.url
        }
    }

    // Ensure WebView is resumed whenever active tab is selected
    LaunchedEffect(activeTab.id) {
        browserSessionManager.resumeActiveWebView()
    }

    // Lifecycle observer: pause active WebView when app/screen is paused or backgrounded,
    // detach from view hierarchy on disposal to prevent leaks while retaining full session & state
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

    fun goToHome() {
        activeTab.url = ""
        activeTab.title = if (activeTab.isIncognito) "Incognito Tab" else "New Tab"
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

    fun stopLoadingPage() {
        val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
        wv.stopLoading()
        activeTab.isLoading = false
    }

    // Natural browser back navigation:
    // 1. Exit fullscreen video if active
    // 2. Dismiss open picker/dialog sheets
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
        } else if (showHistoryDialog) {
            showHistoryDialog = false
        } else if (showClearDataDialog) {
            showClearDataDialog = false
        } else if (showSslDialog) {
            showSslDialog = false
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
                                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back to Vault Button
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("browser_back_to_vault_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Vault",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Omnibox / Modern Address & Search Bar
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = VaultBackground,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isInputFocused) MaterialTheme.colorScheme.primary else VaultCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Security Indicator / Favicon / Search Icon
                                    val fav = activeTab.favicon
                                    if (activeTab.isIncognito) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "Incognito Mode",
                                            tint = Color(0xFFA855F7),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { showSslDialog = true }
                                                .testTag("browser_incognito_badge")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else if (fav != null && !isInputFocused && activeTab.url.isNotEmpty()) {
                                        Image(
                                            bitmap = fav.asImageBitmap(),
                                            contentDescription = "Favicon",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .testTag("browser_favicon")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else if (activeTab.url.isNotEmpty()) {
                                        val isHttps = activeTab.isSecureConnection
                                        Icon(
                                            imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = if (isHttps) "Connection is secure" else "Connection not secure",
                                            tint = if (isHttps) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            modifier = Modifier
                                                .size(17.dp)
                                                .clickable { showSslDialog = true }
                                                .testTag("browser_ssl_button")
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

                                    // Address Input Field
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (inputUrl.isEmpty()) {
                                            Text(
                                                text = if (activeTab.isIncognito) "Search or enter address (Private)" else "Search or enter address",
                                                color = VaultTextSecondary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        BasicTextField(
                                            value = inputUrl,
                                            onValueChange = { inputUrl = it },
                                            singleLine = true,
                                            maxLines = 1,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = { navigateTo(inputUrl) },
                                                onGo = { navigateTo(inputUrl) }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { isInputFocused = it.isFocused }
                                                .testTag("browser_search_input")
                                        )
                                    }

                                    // Right Action inside Omnibox: Clear, Stop, or Reload
                                    if (isInputFocused && inputUrl.isNotEmpty()) {
                                        IconButton(
                                            onClick = { inputUrl = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear address",
                                                tint = VaultTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else if (activeTab.isLoading && activeTab.url.isNotEmpty()) {
                                        IconButton(
                                            onClick = { stopLoadingPage() },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .testTag("browser_stop_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Stop loading",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    } else if (activeTab.url.isNotEmpty()) {
                                        IconButton(
                                            onClick = { reloadCurrentPage() },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .testTag("browser_reload_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reload page",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Tab Switcher Counter Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeTab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (activeTab.isIncognito) Color(0xFFA855F7) else Color.White.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showTabSwitcher = true }
                                    .testTag("browser_tab_switcher_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabs.size.toString(),
                                    color = if (activeTab.isIncognito) Color(0xFFA855F7) else Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // 3-Dots Menu Button
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("browser_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Browser Menu",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .background(VaultCardBackground)
                                        .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New Tab", color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        onClick = {
                                            showMenu = false
                                            browserSessionManager.openNewTab()
                                            inputUrl = ""
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("New Incognito Tab", color = Color(0xFFA855F7)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFA855F7))
                                        },
                                        onClick = {
                                            showMenu = false
                                            browserSessionManager.openNewTab(isIncognito = true)
                                            inputUrl = ""
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("History", color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showHistoryDialog = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Downloads", color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onOpenDownloads()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Desktop Site", color = Color.White)
                                                Checkbox(
                                                    checked = activeTab.isDesktopSite,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.primary,
                                                        checkmarkColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Laptop, contentDescription = null, tint = Color.White)
                                        },
                                        onClick = {
                                            showMenu = false
                                            browserSessionManager.toggleDesktopSite(activeTab)
                                        }
                                    )

                                    if (activeTab.url.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Copy Link", color = Color.White) },
                                            leadingIcon = {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                                            },
                                            onClick = {
                                                showMenu = false
                                                clipboardManager.setText(AnnotatedString(activeTab.url))
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Link copied to clipboard")
                                                }
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = { Text("Clear Browsing Data...", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showClearDataDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Sleek Progress Indicator attached at bottom edge of topBar
                        if (activeTab.pageProgress < 1f && activeTab.url.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { activeTab.pageProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = if (activeTab.isIncognito) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary,
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
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val wv = browserSessionManager.getOrCreateWebView(activeTab, context)
                                if (wv.canGoBack()) wv.goBack()
                            },
                            enabled = activeTab.canGoBack,
                            modifier = Modifier.testTag("browser_nav_back")
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
                            enabled = activeTab.canGoForward,
                            modifier = Modifier.testTag("browser_nav_forward")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (activeTab.canGoForward) Color.White else Color(0xFF555555)
                            )
                        }

                        IconButton(
                            onClick = { goToHome() },
                            modifier = Modifier.testTag("browser_nav_home")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                        }

                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.testTag("browser_nav_history")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                        }

                        IconButton(
                            onClick = { showTabSwitcher = true },
                            modifier = Modifier.testTag("browser_nav_tabs")
                        ) {
                            Icon(Icons.Default.Tab, contentDescription = "Tabs", tint = Color.White)
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
            // Live WebView Viewport
            key(activeTab.id) {
                AndroidView(
                    factory = { ctx ->
                        val wv = browserSessionManager.getOrCreateWebView(activeTab, ctx)
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        browserSessionManager.resumeActiveWebView()
                        wv
                    },
                    update = {
                        browserSessionManager.resumeActiveWebView()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

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

            // Real Browser Start Page (Displayed when active tab URL is empty)
            if (activeTab.url.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeTab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (activeTab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeTab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (activeTab.isIncognito) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (activeTab.isIncognito) "Incognito Private Tab" else "Vault Browser",
                            color = VaultTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (activeTab.isIncognito)
                                "Browsing history is not saved in this tab. Cookies & session data are discarded when closed."
                            else
                                "Fast, private browsing with integrated video sniffer and web storage",
                            color = VaultTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Quick Shortcuts Grid
                        Text(
                            text = "Top Sites",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QUICK_SHORTCUTS.take(4).forEach { shortcut ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { navigateTo(shortcut.url) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(VaultCardBackground)
                                            .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortcut.initial,
                                            color = shortcut.color,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = shortcut.title,
                                        color = VaultTextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QUICK_SHORTCUTS.drop(4).take(4).forEach { shortcut ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { navigateTo(shortcut.url) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(VaultCardBackground)
                                            .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortcut.initial,
                                            color = shortcut.color,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = shortcut.title,
                                        color = VaultTextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Recent History Section (Only shown in regular browsing mode)
                        if (!activeTab.isIncognito && browserHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent History",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = { showHistoryDialog = true }) {
                                    Text("See all", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            browserHistory.take(4).forEach { historyItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { navigateTo(historyItem.url) },
                                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                                    border = BorderStroke(1.dp, VaultCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = VaultTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = historyItem.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = historyItem.url,
                                                color = VaultTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }

            // Floating Snaptube Video Downloader Button
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

    // Granular Clear Browsing Data Dialog
    if (showClearDataDialog) {
        var clearHistoryChecked by remember { mutableStateOf(true) }
        var clearCookiesChecked by remember { mutableStateOf(true) }
        var clearCacheChecked by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text(
                    text = "Clear Browsing Data",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select data types to delete permanently:",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearHistoryChecked = !clearHistoryChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = clearHistoryChecked,
                            onCheckedChange = { clearHistoryChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Browsing History", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Clears persistent visited page records", color = VaultTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearCookiesChecked = !clearCookiesChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = clearCookiesChecked,
                            onCheckedChange = { clearCookiesChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cookies & Site Data", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Signs you out of web accounts", color = VaultTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearCacheChecked = !clearCacheChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = clearCacheChecked,
                            onCheckedChange = { clearCacheChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cached Images & Files", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Frees up device storage space", color = VaultTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearSelectedBrowserData(
                            clearHistory = clearHistoryChecked,
                            clearCookies = clearCookiesChecked,
                            clearCache = clearCacheChecked
                        )
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = cornerStyle.buttonShape,
                    modifier = Modifier.testTag("confirm_clear_browser_data_button")
                ) {
                    Text("Clear Selected", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }

    // SSL / Security Status Dialog
    if (showSslDialog) {
        val isHttps = activeTab.isSecureConnection
        val host = activeTab.sslHost ?: "Current Website"

        AlertDialog(
            onDismissRequest = { showSslDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isHttps) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHttps) "Connection is secure" else "Connection not secure",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = host,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHttps)
                            "Your information (for example, passwords or credit card numbers) is private and encrypted when it is sent to this site."
                        else
                            "You should not enter any sensitive information on this site (for example, passwords or credit cards), because it could be intercepted by attackers.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    if (activeTab.isIncognito) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Incognito mode active: Browsing history will not be saved.", color = Color(0xFFA855F7), fontSize = 11.5.sp)
                        }
                    }
                }
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                TextButton(onClick = { showSslDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun BrowserTabOverviewScreen(
    browserSessionManager: BrowserSessionManager,
    cornerStyle: VaultCornerStyle,
    onClose: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onCloseTab: (Int) -> Unit,
    onCloseAllTabs: () -> Unit
) {
    BackHandler { onClose() }

    val tabs = browserSessionManager.tabs
    val activeTabIndex = browserSessionManager.activeTabIndex
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        topBar = {
            Surface(
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("tab_overview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to page",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Tabs",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tabs.size} ${if (tabs.size == 1) "open tab" else "open tabs"}",
                            color = VaultTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = { onNewTab(false) },
                        modifier = Modifier.testTag("tab_overview_add_tab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Tab options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(VaultCardBackground)
                                .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Regular Tab", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onNewTab(false)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("New Incognito Tab", color = Color(0xFFA855F7)) },
                                leadingIcon = {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFA855F7))
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onNewTab(true)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Close All Tabs", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onCloseAllTabs()
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNewTab(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_tab_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = cornerStyle.buttonShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Tab", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNewTab(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_incognito_tab_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                        shape = cornerStyle.buttonShape
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incognito", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultCardBackground)
                            .border(1.dp, VaultCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tab,
                            contentDescription = null,
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Open Tabs",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open a new tab to begin browsing the web.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onNewTab(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = cornerStyle.buttonShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open New Tab")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                    val isActive = index == activeTabIndex
                    val tabCardBg = if (tab.isIncognito) Color(0xFF1E1428) else VaultCardBackground
                    val activeBorderColor = if (tab.isIncognito) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary
                    val borderColor = if (isActive) activeBorderColor else VaultCardBorder

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTab(index) }
                            .testTag("browser_tab_card_$index"),
                        colors = CardDefaults.cardColors(containerColor = tabCardBg),
                        border = BorderStroke(width = if (isActive) 2.dp else 1.dp, color = borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Header: Icon/Favicon, Title, Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val fav = tab.favicon
                                if (tab.isIncognito) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "Incognito",
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (fav != null) {
                                    Image(
                                        bitmap = fav.asImageBitmap(),
                                        contentDescription = "Favicon",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = VaultTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Text(
                                    text = tab.title.ifEmpty { if (tab.isIncognito) "Incognito" else "New Tab" },
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { onCloseTab(index) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("browser_close_tab_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Tab",
                                        tint = VaultTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preview Body
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VaultBackground)
                                    .border(0.5.dp, VaultCardBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = tab.url.ifEmpty { "Start Page" },
                                        color = VaultTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Badges
                                Row(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (tab.isIncognito) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFA855F7).copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "INCOGNITO",
                                                color = Color(0xFFA855F7),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(activeBorderColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = activeBorderColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserHistoryScreen(
    history: List<BrowserHistoryItem>,
    cornerStyle: VaultCornerStyle,
    onClose: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    BackHandler { onClose() }

    var historySearchQuery by remember { mutableStateOf("") }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val filteredHistory = remember(history, historySearchQuery) {
        if (historySearchQuery.isBlank()) {
            history
        } else {
            history.filter {
                it.title.contains(historySearchQuery, ignoreCase = true) ||
                        it.url.contains(historySearchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        topBar = {
            Surface(
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Browser",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Browsing History",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${history.size} ${if (history.size == 1) "entry" else "entries"}",
                            color = VaultTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear All History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search / Filter Input
            Surface(
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (historySearchQuery.isEmpty()) {
                            Text(
                                text = "Search history...",
                                color = VaultTextSecondary,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )
                    }
                    if (historySearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { historySearchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // History List or Empty State
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(VaultCardBackground)
                                .border(1.dp, VaultCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (historySearchQuery.isNotEmpty()) "No Matching History" else "No Browsing History",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (historySearchQuery.isNotEmpty())
                                "No pages match \"$historySearchQuery\""
                            else
                                "Websites you visit during regular browsing sessions will be listed here.",
                            color = VaultTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                        val formattedDate = remember(item.visitedAt) { dateFormat.format(Date(item.visitedAt)) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenUrl(item.url) }
                                .testTag("history_item_${item.id}"),
                            colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                            border = BorderStroke(1.dp, VaultCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { item.url },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.url,
                                        color = VaultTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formattedDate,
                                        color = VaultTextSecondary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteEntry(item.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("history_delete_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete entry",
                                        tint = VaultTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirm = false },
                title = { Text("Clear All Browsing History?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently delete all entries in your browsing history.", color = VaultTextSecondary, fontSize = 14.sp) },
                shape = cornerStyle.dialogShape,
                containerColor = VaultCardBackground,
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAll()
                            showClearAllConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = cornerStyle.buttonShape,
                        modifier = Modifier.testTag("confirm_clear_all_history_button")
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirm = false }) {
                        Text("Cancel", color = Color(0xFFA0A0A0))
                    }
                }
            )
        }
    }
}
