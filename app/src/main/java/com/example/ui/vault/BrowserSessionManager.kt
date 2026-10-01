package com.example.ui.vault

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.UUID

class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    initialTitle: String = "New Tab",
    initialUrl: String = "",
    initialFavicon: Bitmap? = null,
    val isIncognito: Boolean = false
) {
    var title by mutableStateOf(initialTitle)
    var url by mutableStateOf(initialUrl)
    var favicon by mutableStateOf<Bitmap?>(initialFavicon)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var pageProgress by mutableFloatStateOf(1f)
    var hasError by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isDesktopSite by mutableStateOf(false)
    var isSecureConnection by mutableStateOf(false)
    var sslHost by mutableStateOf<String?>(null)
    var detectedVideoUrl by mutableStateOf<String?>(null)
    var detectedVideoTitle by mutableStateOf("Web Video")
    var customVideoView by mutableStateOf<View?>(null)
    var customViewCallback by mutableStateOf<WebChromeClient.CustomViewCallback?>(null)
    var webViewVersion by mutableIntStateOf(0)
    var lastRecordedUrl: String? = null
    var lastRecordedTitle: String? = null
    var lastRecordTime: Long = 0L
}

/**
 * Manages normal & incognito browser sessions, tabs, and leak-free WebView lifecycle
 * across navigation within Calculator Vault.
 */
class BrowserSessionManager(private val application: Application) {

    companion object {
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }

    private val contextWrapper = MutableContextWrapper(application.applicationContext)
    private val webViews = mutableMapOf<String, WebView>()

    init {
        try {
            android.system.Os.setenv("MESA_DEBUG", "0", true)
            android.system.Os.setenv("EGL_LOG_LEVEL", "fatal", true)
        } catch (_: Exception) {}
    }

    val tabs = mutableStateListOf(BrowserTab())
    var activeTabIndex by mutableIntStateOf(0)

    var onPageVisited: ((url: String, title: String, isIncognito: Boolean) -> Unit)? = null

    val activeTab: BrowserTab
        get() = tabs.getOrElse(activeTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))) {
            if (tabs.isEmpty()) {
                val newTab = BrowserTab()
                tabs.add(newTab)
                newTab
            } else {
                tabs.first()
            }
        }

    fun selectTab(index: Int) {
        if (index in tabs.indices) {
            val oldTabId = activeTab.id
            if (activeTabIndex != index) {
                webViews[oldTabId]?.onPause()
            }
            activeTabIndex = index
            webViews[activeTab.id]?.onResume()
        }
    }

    fun openNewTab(url: String = "", isIncognito: Boolean = false): BrowserTab {
        val newTab = BrowserTab(
            initialTitle = if (url.isEmpty()) if (isIncognito) "Incognito Tab" else "New Tab" else "Loading...",
            initialUrl = url,
            isIncognito = isIncognito
        )
        tabs.add(newTab)
        activeTabIndex = tabs.lastIndex
        return newTab
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tabToClose = tabs[index]
        val isClosingActive = index == activeTabIndex

        // Clean up and destroy the tab's WebView
        webViews.remove(tabToClose.id)?.let { wv ->
            try {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                if (tabToClose.isIncognito) {
                    wv.clearFormData()
                }
                wv.destroy()
            } catch (_: Exception) {}
        }

        tabs.removeAt(index)

        if (tabs.isEmpty()) {
            val freshTab = BrowserTab()
            tabs.add(freshTab)
            activeTabIndex = 0
        } else if (isClosingActive) {
            activeTabIndex = (index - 1).coerceAtLeast(0).coerceAtMost(tabs.lastIndex)
        } else if (activeTabIndex > index) {
            activeTabIndex--
        }
    }

    fun closeAllTabs() {
        webViews.values.forEach { wv ->
            try {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.destroy()
            } catch (_: Exception) {}
        }
        webViews.clear()
        tabs.clear()
        tabs.add(BrowserTab())
        activeTabIndex = 0
    }

    fun toggleDesktopSite(tab: BrowserTab) {
        tab.isDesktopSite = !tab.isDesktopSite
        val wv = webViews[tab.id] ?: return
        val defaultUa = WebSettings.getDefaultUserAgent(contextWrapper)
        wv.settings.userAgentString = if (tab.isDesktopSite) DESKTOP_USER_AGENT else defaultUa
        wv.reload()
    }

    fun recreateTabWebView(tab: BrowserTab, context: Context): WebView {
        webViews.remove(tab.id)?.let { oldWv ->
            try {
                (oldWv.parent as? ViewGroup)?.removeView(oldWv)
                oldWv.stopLoading()
                oldWv.destroy()
            } catch (_: Exception) {}
        }
        tab.webViewVersion++
        return getOrCreateWebView(tab, context)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(tab: BrowserTab, context: Context): WebView {
        contextWrapper.baseContext = context

        val existing = webViews[tab.id]
        if (existing != null) {
            return existing
        }

        // Limit concurrent WebViews in memory to prevent renderer process memory pressure
        if (webViews.size >= 4) {
            val candidateId = webViews.keys.firstOrNull { it != tab.id && it != activeTab.id }
            if (candidateId != null) {
                webViews.remove(candidateId)?.let { oldWv ->
                    try {
                        (oldWv.parent as? ViewGroup)?.removeView(oldWv)
                        oldWv.stopLoading()
                        oldWv.destroy()
                    } catch (_: Exception) {}
                }
            }
        }

        val wv = WebView(contextWrapper).apply {
            // Setup Cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, false)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mediaPlaybackRequiresUserGesture = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                val defaultUa = WebSettings.getDefaultUserAgent(contextWrapper)
                userAgentString = if (tab.isDesktopSite) DESKTOP_USER_AGENT else defaultUa
                cacheMode = if (tab.isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            }

            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    webViews.values.forEach { deadWv ->
                        try {
                            (deadWv.parent as? ViewGroup)?.removeView(deadWv)
                            deadWv.destroy()
                        } catch (_: Exception) {}
                    }
                    webViews.clear()
                    tab.webViewVersion++
                    tab.isLoading = false
                    tab.hasError = true
                    val didCrash = detail?.didCrash() == true
                    tab.errorMessage = if (didCrash) {
                        "The webpage renderer stopped unexpectedly. Tap 'Try Again' to reload."
                    } else {
                        "The webpage was closed by the system. Tap 'Try Again' to reload."
                    }
                    return true
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val reqUrl = request?.url?.toString() ?: return false
                    if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://") || reqUrl.startsWith("data:") || reqUrl.startsWith("blob:") || reqUrl.startsWith("about:")) {
                        return false
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        contextWrapper.startActivity(intent)
                    } catch (_: Exception) {}
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    tab.isLoading = true
                    tab.hasError = false
                    tab.errorMessage = null
                    if (!url.isNullOrEmpty() && url != "about:blank") {
                        tab.url = url
                        val isHttps = url.startsWith("https://", ignoreCase = true)
                        tab.isSecureConnection = isHttps
                        tab.sslHost = try {
                            Uri.parse(url).host
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        tab.isSecureConnection = false
                        tab.sslHost = null
                    }
                    if (favicon != null) {
                        tab.favicon = favicon
                    }
                    tab.pageProgress = 0.15f
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    val effectiveUrl = url ?: view?.url ?: ""
                    if (effectiveUrl.isNotEmpty() && effectiveUrl != "about:blank") {
                        tab.url = effectiveUrl
                        val isHttps = effectiveUrl.startsWith("https://", ignoreCase = true)
                        tab.isSecureConnection = isHttps
                        tab.sslHost = try {
                            Uri.parse(effectiveUrl).host
                        } catch (_: Exception) {
                            null
                        }
                    }
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    val effectiveUrl = url ?: view?.url ?: ""
                    if (effectiveUrl.isNotEmpty() && effectiveUrl != "about:blank") {
                        tab.url = effectiveUrl
                    }
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    tab.isLoading = false
                    tab.pageProgress = 1f
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                    val pageTitle = view?.title
                    if (!pageTitle.isNullOrBlank() && !pageTitle.startsWith("http")) {
                        tab.title = pageTitle
                    }

                    // Flush cookies to persistent disk storage for regular browsing sessions
                    if (!tab.isIncognito) {
                        CookieManager.getInstance().flush()
                    }

                    // Record to persistent history (only for regular browsing, never incognito)
                    val finishedUrl = (url ?: view?.url ?: "").trim()
                    if (!tab.isIncognito && finishedUrl.isNotEmpty() && finishedUrl != "about:blank" && !finishedUrl.startsWith("data:") && !finishedUrl.startsWith("javascript:")) {
                        val titleToRecord = if (!pageTitle.isNullOrBlank() && !pageTitle.startsWith("http://") && !pageTitle.startsWith("https://")) pageTitle.trim() else finishedUrl
                        val lastUrl = tab.lastRecordedUrl
                        val lastTitle = tab.lastRecordedTitle
                        val now = System.currentTimeMillis()
                        val isExactSame = (finishedUrl.equals(lastUrl, ignoreCase = true) && titleToRecord == lastTitle)
                        val isSameSearch = isSameSearchQuery(finishedUrl, lastUrl)

                        if (!isExactSame) {
                            val isTitleUpgrade = finishedUrl.equals(lastUrl, ignoreCase = true) &&
                                    !titleToRecord.startsWith("http") &&
                                    (lastTitle == null || lastTitle.startsWith("http") || lastTitle.isBlank())
                            val isSignificantUrlChange = !finishedUrl.equals(lastUrl, ignoreCase = true) && !isSameSearch
                            val isElapsedEnough = (now - tab.lastRecordTime >= 2000L)

                            if (isSignificantUrlChange || isTitleUpgrade || (isSameSearch && isTitleUpgrade) || (isSameSearch && isElapsedEnough && titleToRecord != lastTitle)) {
                                tab.lastRecordedUrl = finishedUrl
                                tab.lastRecordedTitle = titleToRecord
                                tab.lastRecordTime = now
                                onPageVisited?.invoke(finishedUrl, titleToRecord, false)
                            }
                        }
                    }

                    // Automatic video detection script (preserves downloader sniffer)
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
                            tab.detectedVideoUrl = clean
                            tab.detectedVideoTitle = view?.title ?: "Web Video"
                        }
                    }

                    if (url?.contains("youtube.com") == true || url?.contains("vimeo") == true || url?.contains(".mp4") == true) {
                        tab.detectedVideoUrl = url
                        tab.detectedVideoTitle = view?.title ?: "Streaming Video"
                    }

                    // Safely disable WebGL contexts to prevent GLES2 zero-count warnings and renderer memory leaks
                    val disableWebGlJs = """
                        (function() {
                            try {
                                if (window.HTMLCanvasElement && !window.__webglDisabled) {
                                    window.__webglDisabled = true;
                                    var origGetContext = HTMLCanvasElement.prototype.getContext;
                                    HTMLCanvasElement.prototype.getContext = function(type) {
                                        if (type === 'webgl' || type === 'experimental-webgl' || type === 'webgl2') {
                                            return null;
                                        }
                                        return origGetContext.apply(this, arguments);
                                    };
                                }
                            } catch(e) {}
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(disableWebGlJs, null)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    tab.isSecureConnection = false
                    super.onReceivedSslError(view, handler, error)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: ""
                    if (reqUrl.endsWith(".mp4", true) || reqUrl.contains(".mp4?") || reqUrl.contains("videoplayback")) {
                        tab.detectedVideoUrl = reqUrl
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        tab.isLoading = false
                        val description = error?.description?.toString() ?: "Unable to connect"
                        tab.hasError = true
                        tab.errorMessage = when (error?.errorCode) {
                            ERROR_HOST_LOOKUP, ERROR_CONNECT -> "Unable to connect to the website. Please check your network connection."
                            ERROR_TIMEOUT -> "The connection timed out. The server may be unreachable."
                            ERROR_UNSUPPORTED_SCHEME -> "This address protocol is unsupported."
                            ERROR_FAILED_SSL_HANDSHAKE -> "Secure connection could not be established."
                            else -> "Could not load this page ($description)."
                        }
                        tab.pageProgress = 1f
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    tab.pageProgress = newProgress / 100f
                    if (newProgress >= 100) {
                        tab.isLoading = false
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank() && !title.startsWith("http")) {
                        tab.title = title
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    if (icon != null) {
                        tab.favicon = icon
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    tab.customVideoView = view
                    tab.customViewCallback = callback
                }

                override fun onHideCustomView() {
                    tab.customViewCallback?.onCustomViewHidden()
                    tab.customVideoView = null
                    tab.customViewCallback = null
                }
            }

            if (tab.url.isNotEmpty()) {
                loadUrl(tab.url)
            }
        }

        webViews[tab.id] = wv
        return wv
    }

    private fun isSameSearchQuery(url1: String?, url2: String?): Boolean {
        if (url1.isNullOrEmpty() || url2.isNullOrEmpty()) return false
        return try {
            val uri1 = Uri.parse(url1)
            val uri2 = Uri.parse(url2)
            val h1 = uri1.host?.removePrefix("www.")?.lowercase() ?: ""
            val h2 = uri2.host?.removePrefix("www.")?.lowercase() ?: ""
            if (h1.isNotEmpty() && h1 == h2 && (h1.contains("duckduckgo.com") || h1.contains("google.") || h1.contains("search.brave.com") || h1.contains("bing.com"))) {
                val q1 = uri1.getQueryParameter("q") ?: uri1.getQueryParameter("query")
                val q2 = uri2.getQueryParameter("q") ?: uri2.getQueryParameter("query")
                q1 != null && q1.equals(q2, ignoreCase = true)
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun detachWebView(tab: BrowserTab) {
        val wv = webViews[tab.id]
        if (wv != null) {
            try {
                (wv.parent as? ViewGroup)?.removeView(wv)
            } catch (_: Exception) {}
        }
        contextWrapper.baseContext = application.applicationContext
    }

    fun pauseActiveWebView() {
        webViews[activeTab.id]?.onPause()
    }

    fun resumeActiveWebView() {
        webViews[activeTab.id]?.onResume()
    }

    fun clearCookiesOnly(onComplete: () -> Unit = {}) {
        try {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                onComplete()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun clearCacheOnly(onComplete: () -> Unit = {}) {
        try {
            webViews.values.firstOrNull()?.clearCache(true) ?: WebView(contextWrapper).clearCache(true)
        } catch (_: Exception) {}
        onComplete()
    }

    fun clearSelectedData(clearCookies: Boolean, clearCache: Boolean, onComplete: () -> Unit = {}) {
        if (clearCache) {
            try {
                webViews.values.firstOrNull()?.clearCache(true) ?: WebView(contextWrapper).clearCache(true)
                WebStorage.getInstance().deleteAllData()
            } catch (_: Exception) {}
        }

        if (clearCookies) {
            clearCookiesOnly {
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        try {
            // 1. WebStorage delete all data
            WebStorage.getInstance().deleteAllData()

            // 2. CookieManager remove all cookies
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()

            // 3. Clear cache once globally
            webViews.values.firstOrNull()?.clearCache(true) ?: WebView(contextWrapper).clearCache(true)

            // 4. Clear and destroy all active WebViews
            webViews.values.forEach { wv ->
                try {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.stopLoading()
                    wv.clearHistory()
                    wv.clearFormData()
                    wv.clearSslPreferences()
                    wv.destroy()
                } catch (_: Exception) {}
            }
            webViews.clear()

            // 5. Reset tabs to clean single start page
            tabs.clear()
            tabs.add(BrowserTab())
            activeTabIndex = 0
        } catch (_: Exception) {
        } finally {
            onComplete()
        }
    }

    fun destroyAll() {
        webViews.values.forEach { wv ->
            try {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            } catch (_: Exception) {}
        }
        webViews.clear()
        contextWrapper.baseContext = application.applicationContext
    }
}
