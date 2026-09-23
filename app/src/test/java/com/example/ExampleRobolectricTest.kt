package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.robolectric.Robolectric
import org.junit.Assert.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("API Playground", appName)
  }

  @Test
  fun `launch main activity successfully`() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = activityController.get()
    assertNotNull(activity)
  }
}
