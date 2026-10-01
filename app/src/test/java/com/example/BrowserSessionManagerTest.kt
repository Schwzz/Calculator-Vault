package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.vault.BrowserSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.async

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserSessionManagerTest {

    private lateinit var app: Application
    private lateinit var sessionManager: BrowserSessionManager

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        sessionManager = BrowserSessionManager(app)
    }

    @Test
    fun `initial session has one clean default tab`() {
        assertEquals(1, sessionManager.tabs.size)
        assertEquals(0, sessionManager.activeTabIndex)
        val activeTab = sessionManager.activeTab
        assertEquals("New Tab", activeTab.title)
        assertEquals("", activeTab.url)
        assertFalse(activeTab.isIncognito)
        assertFalse(activeTab.canGoBack)
        assertFalse(activeTab.canGoForward)
        assertFalse(activeTab.hasError)
    }

    @Test
    fun `opening a new tab updates active tab and preserves prior tab state`() {
        val firstTab = sessionManager.activeTab
        firstTab.url = "https://www.youtube.com"
        firstTab.title = "YouTube"
        firstTab.canGoBack = true

        val secondTab = sessionManager.openNewTab("https://www.google.com")
        assertEquals(2, sessionManager.tabs.size)
        assertEquals(1, sessionManager.activeTabIndex)
        assertEquals(secondTab.id, sessionManager.activeTab.id)
        assertEquals("https://www.google.com", sessionManager.activeTab.url)
        assertFalse(sessionManager.activeTab.isIncognito)

        // Switch back to first tab
        sessionManager.selectTab(0)
        assertEquals(0, sessionManager.activeTabIndex)
        assertEquals("https://www.youtube.com", sessionManager.activeTab.url)
        assertEquals("YouTube", sessionManager.activeTab.title)
        assertTrue(sessionManager.activeTab.canGoBack)
    }

    @Test
    fun `opening an incognito tab marks tab as private`() {
        val incognitoTab = sessionManager.openNewTab(isIncognito = true)
        assertEquals(2, sessionManager.tabs.size)
        assertEquals(1, sessionManager.activeTabIndex)
        assertTrue(incognitoTab.isIncognito)
        assertEquals("Incognito Tab", incognitoTab.title)
    }

    @Test
    fun `closing a tab cleanly updates activeTabIndex and keeps valid state`() {
        sessionManager.openNewTab("https://example1.com")
        sessionManager.openNewTab("https://example2.com")
        assertEquals(3, sessionManager.tabs.size)
        assertEquals(2, sessionManager.activeTabIndex)

        // Close active tab
        sessionManager.closeTab(2)
        assertEquals(2, sessionManager.tabs.size)
        assertEquals(1, sessionManager.activeTabIndex)

        // Close remaining tab until empty returns fresh tab
        sessionManager.closeTab(1)
        sessionManager.closeTab(0)
        assertEquals(1, sessionManager.tabs.size)
        assertEquals(0, sessionManager.activeTabIndex)
        assertNotNull(sessionManager.activeTab)
    }

    @Test
    fun `closeAllTabs resets to single clean tab`() {
        sessionManager.openNewTab("https://site1.com")
        sessionManager.openNewTab("https://site2.com")
        assertEquals(3, sessionManager.tabs.size)

        sessionManager.closeAllTabs()
        assertEquals(1, sessionManager.tabs.size)
        assertEquals(0, sessionManager.activeTabIndex)
        assertEquals("", sessionManager.activeTab.url)
    }

    @Test
    fun `clearAllData resets all tabs to clean session`() {
        sessionManager.activeTab.url = "https://example.com"
        sessionManager.openNewTab("https://anotherexample.com")
        assertEquals(2, sessionManager.tabs.size)

        var cleared = false
        sessionManager.clearAllData {
            cleared = true
        }

        assertTrue(cleared)
        assertEquals(1, sessionManager.tabs.size)
        assertEquals(0, sessionManager.activeTabIndex)
        assertEquals("", sessionManager.activeTab.url)
        assertEquals("New Tab", sessionManager.activeTab.title)
    }

    @Test
    fun `default search engine is DuckDuckGo`() {
        val prefs = com.example.data.VaultPreferences(app)
        assertEquals("DuckDuckGo", prefs.searchEngine)
        val searchUrl = prefs.getSearchUrl("cats")
        assertTrue(searchUrl.contains("duckduckgo.com/?q=cats"))
    }

    @Test
    fun `history deduplication ensures single entry for search queries and redirects`() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.VaultDatabase.getDatabase(app)
        val prefs = com.example.data.VaultPreferences(app)
        val repository = com.example.data.VaultRepository(db.vaultDao(), prefs)
        repository.clearAllHistory()

        // 1. Search "cats": initial URL followed by redirect & title update
        repository.recordHistory("https://duckduckgo.com/?q=cats", "https://duckduckgo.com/?q=cats")
        repository.recordHistory("https://duckduckgo.com/?q=cats&ia=web", "cats at DuckDuckGo")
        repository.recordHistory("https://duckduckgo.com/?q=cats&t=h_&ia=web", "cats at DuckDuckGo")

        var historyList = db.vaultDao().getRecentHistoryItems(10)
        assertEquals(1, historyList.size)
        assertEquals("cats at DuckDuckGo", historyList[0].title)

        // 2. Search "dogs"
        repository.recordHistory("https://duckduckgo.com/?q=dogs", "https://duckduckgo.com/?q=dogs")
        repository.recordHistory("https://duckduckgo.com/?q=dogs&ia=web", "dogs at DuckDuckGo")

        historyList = db.vaultDao().getRecentHistoryItems(10)
        assertEquals(2, historyList.size)
        assertEquals("dogs at DuckDuckGo", historyList[0].title)
        assertEquals("cats at DuckDuckGo", historyList[1].title)

        // 3. Search "fish"
        repository.recordHistory("https://duckduckgo.com/?q=fish", "fish at DuckDuckGo")

        historyList = db.vaultDao().getRecentHistoryItems(10)
        assertEquals(3, historyList.size)
        assertEquals("fish at DuckDuckGo", historyList[0].title)

        // 4. Visiting regular page with trailing slash and anchor deduplication
        repository.recordHistory("https://kotlinlang.org", "Kotlin Programming Language")
        repository.recordHistory("https://kotlinlang.org/", "Kotlin Programming Language")
        repository.recordHistory("https://kotlinlang.org/#overview", "Kotlin Programming Language")

        historyList = db.vaultDao().getRecentHistoryItems(10)
        assertEquals(4, historyList.size)
        assertEquals("Kotlin Programming Language", historyList[0].title)

        // Clean up
        repository.clearAllHistory()
    }

    @Test
    fun `concurrent history recording calls are thread-safe and deduplicated`() = kotlinx.coroutines.runBlocking {
        val db = com.example.data.VaultDatabase.getDatabase(app)
        val prefs = com.example.data.VaultPreferences(app)
        val repository = com.example.data.VaultRepository(db.vaultDao(), prefs)
        repository.clearAllHistory()

        // Simulate 5 concurrent coroutines recording for the same search query
        val jobs = (1..5).map { index ->
            async(kotlinx.coroutines.Dispatchers.Default) {
                repository.recordHistory(
                    "https://duckduckgo.com/?q=cats&param=$index",
                    if (index == 5) "cats at DuckDuckGo" else "https://duckduckgo.com/?q=cats"
                )
            }
        }
        jobs.forEach { it.await() }

        val historyList = db.vaultDao().getRecentHistoryItems(10)
        assertEquals(1, historyList.size)
        assertEquals("cats at DuckDuckGo", historyList[0].title)

        repository.clearAllHistory()
    }
}
