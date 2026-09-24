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

        // Switch back to first tab
        sessionManager.selectTab(0)
        assertEquals(0, sessionManager.activeTabIndex)
        assertEquals("https://www.youtube.com", sessionManager.activeTab.url)
        assertEquals("YouTube", sessionManager.activeTab.title)
        assertTrue(sessionManager.activeTab.canGoBack)
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
}
