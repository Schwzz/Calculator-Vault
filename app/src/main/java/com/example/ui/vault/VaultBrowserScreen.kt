package com.example.ui.vault

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalVaultCornerStyle
import kotlinx.coroutines.launch

class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    initialTitle: String = "New Tab",
    initialUrl: String = "",
    initialFavicon: Bitmap? = null
) {
    var title by mutableStateOf(initialTitle)
    var url by mutableStateOf(initialUrl)
    var favicon by mutableStateOf<Bitmap?>(initialFavicon)
}

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
    val tabs = remember { mutableStateListOf(BrowserTab()) }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    val activeTab = tabs.getOrNull(activeTabIndex) ?: tabs.first()
    val currentTab = activeTab

    var inputUrl by remember { mutableStateOf(activeTab.url) }
    var isInputFocused by remember { mutableStateOf(false) }
    var currentFavicon by remember { mutableStateOf<Bitmap?>(activeTab.favicon) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableFloatStateOf(1f) }
    var detectedVideoUrl by remember { mutableStateOf<String?>(null) }
    var detectedVideoTitle by remember { mutableStateOf("Web Video") }
    var showResolutionPicker by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cornerStyle = LocalVaultCornerStyle.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Dynamic WebView Loading: Trigger loadUrl inside LaunchedEffect(activeTab.url)
    LaunchedEffect(activeTab.url) {
        val wv = webViewInstance
        if (wv != null && activeTab.url.isNotEmpty() && wv.url != activeTab.url) {
            wv.loadUrl(activeTab.url)
        }
    }

    LaunchedEffect(activeTabIndex) {
        inputUrl = activeTab.url
        currentFavicon = activeTab.favicon
    }

    BackHandler {
        if (customVideoView != null) {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        } else if (showResolutionPicker) {
            showResolutionPicker = false
        } else if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else if (activeTab.url.isNotEmpty()) {
            activeTab.url = ""
            inputUrl = ""
            currentFavicon = null
            detectedVideoUrl = null
        } else {
            onNavigateBack()
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
        keyboardController?.hide()
        focusManager.clearFocus()
        webViewInstance?.let { wv ->
            if (wv.url != targetUrl) {
                wv.loadUrl(targetUrl)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                            modifier = Modifier.size(40.dp).testTag("browser_back_to_vault_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Vault", tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Search & URL Input Field - Decoupled state, IME Search, Favicon support, No text cutoff
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
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                onGo = {
                                    navigateTo(inputUrl)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
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
                                    val fav = currentFavicon
                                    if (fav != null && !isInputFocused && currentTab.url.isNotEmpty()) {
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
                            modifier = Modifier.size(36.dp).testTag("browser_downloads_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Downloads", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { showClearDataConfirm = true },
                            modifier = Modifier.size(36.dp).testTag("browser_clear_data_button")
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear Browser Data",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (pageProgress < 1f && currentTab.url.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { pageProgress },
                            modifier = Modifier.fillMaxWidth().height(2.5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (currentTab.url.isNotEmpty()) {
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
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) Color.White else Color(0xFF555555)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) Color.White else Color(0xFF555555)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentTab.url = ""
                                inputUrl = ""
                                detectedVideoUrl = null
                            }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                        }

                        IconButton(onClick = { webViewInstance?.reload() }) {
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
            // Live WebView - Always kept in layout with Modifier.fillMaxSize()
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Hardware acceleration enabled for high-performance HTML5 video & smooth rendering
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                // Gracefully handle render process termination without crashing
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (!isInputFocused) {
                                    inputUrl = url ?: ""
                                }
                                activeTab.url = url ?: ""
                                if (favicon != null) {
                                    currentFavicon = favicon
                                    activeTab.favicon = favicon
                                }
                                pageProgress = 0.2f
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                pageProgress = 1f
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                activeTab.title = view?.title ?: "Page"
                                if (!isInputFocused) {
                                    inputUrl = url ?: ""
                                }

                                // Automatic video detection script
                                val checkVideoJs = """
                                    (function() {
                                        var v = document.querySelector('video');
                                        if (v && v.src) return v.src;
                                        var sources = document.querySelectorAll('video source');
                                        for (var i=0; i<sources.length; i++) {
                                            if (sources[i].src) return sources[i].src;
                                        }
                                        return '';
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(checkVideoJs) { result ->
                                    val clean = result?.replace("\"", "")?.trim()
                                    if (!clean.isNullOrEmpty() && clean != "null") {
                                        detectedVideoUrl = clean
                                        detectedVideoTitle = view?.title ?: "Web Video"
                                    }
                                }

                                if (url?.contains("youtube.com") == true || url?.contains("vimeo") == true || url?.contains(".mp4") == true) {
                                    detectedVideoUrl = url
                                    detectedVideoTitle = view?.title ?: "Streaming Video"
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""
                                if (reqUrl.endsWith(".mp4", true) || reqUrl.contains(".mp4?") || reqUrl.contains("videoplayback")) {
                                    detectedVideoUrl = reqUrl
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                                super.onProgressChanged(view, newProgress)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrEmpty()) {
                                    activeTab.title = title
                                }
                            }

                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                super.onReceivedIcon(view, icon)
                                if (icon != null) {
                                    currentFavicon = icon
                                    activeTab.favicon = icon
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customVideoView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customViewCallback?.onCustomViewHidden()
                                customVideoView = null
                                customViewCallback = null
                            }
                        }

                        if (activeTab.url.isNotEmpty()) {
                            loadUrl(activeTab.url)
                        }
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                    if (activeTab.url.isNotEmpty() && wv.url != activeTab.url) {
                        wv.loadUrl(activeTab.url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Fullscreen Web Video container (e.g. YouTube / HTML5 video fullscreen)
            if (customVideoView != null) {
                AndroidView(
                    factory = { customVideoView!! },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            // Minimal, elegant private browser start page (displayed when active tab URL is empty)
            if (activeTab.url.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultBackground)
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
                visible = detectedVideoUrl != null && activeTab.url.isNotEmpty(),
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
                        text = detectedVideoTitle,
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
                                        title = detectedVideoTitle,
                                        url = detectedVideoUrl ?: "https://example.com/stream.mp4",
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
                                        activeTabIndex = index
                                        inputUrl = tab.url
                                        currentFavicon = tab.favicon
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
                                                val removingCurrent = index == activeTabIndex
                                                tabs.removeAt(index)
                                                if (removingCurrent) {
                                                    activeTabIndex = (activeTabIndex - 1).coerceAtLeast(0)
                                                    inputUrl = tabs[activeTabIndex].url
                                                    currentFavicon = tabs[activeTabIndex].favicon
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Tab", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val newTab = BrowserTab()
                            tabs.add(newTab)
                            activeTabIndex = tabs.lastIndex
                            inputUrl = ""
                            currentFavicon = null
                            showTabSwitcher = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("new_tab_button"),
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
                        try {
                            // 1. WebStorage delete all data
                            WebStorage.getInstance().deleteAllData()

                            // 2. CookieManager remove all cookies
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()

                            // 3. Clear active WebView history and cache
                            webViewInstance?.let { wv ->
                                wv.clearHistory()
                                wv.clearCache(true)
                                wv.clearFormData()
                                wv.clearSslPreferences()
                            }

                            // 4. Reset active tabs and URLs to clean state
                            tabs.clear()
                            val cleanTab = BrowserTab()
                            tabs.add(cleanTab)
                            activeTabIndex = 0
                            inputUrl = ""
                            currentFavicon = null
                            detectedVideoUrl = null
                            canGoBack = false
                            canGoForward = false

                            // Show confirmation snackbar
                            scope.launch {
                                snackbarHostState.showSnackbar("Browser cache & history cleared")
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cleared browser data")
                            }
                        } finally {
                            showClearDataConfirm = false
                        }
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

