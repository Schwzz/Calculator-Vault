package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VaultPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculator", appName)
  }

  @Test
  fun `default security settings are enabled by default on fresh install`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = VaultPreferences(context)
    assertTrue("Auto-reset on exit must default to true", prefs.resetOnExit)
    assertTrue("Hide recents preview must default to true", prefs.hideRecentsPreview)
    assertTrue("Block screenshots must default to true", prefs.blockScreenshots)
  }
}
