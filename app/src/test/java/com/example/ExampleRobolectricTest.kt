package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ViralMoment
import com.example.data.network.YouTubeApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ViralClip", appName)
  }

  @Test
  fun `youtube url parsing extracts video id`() {
    val apiService = YouTubeApiService()
    assertEquals("0e3GPea1Tyg", apiService.extractVideoId("https://www.youtube.com/watch?v=0e3GPea1Tyg"))
    assertEquals("kcnwnc2Fh60", apiService.extractVideoId("https://youtu.be/kcnwnc2Fh60"))
    assertEquals("bHIhgxav9LY", apiService.extractVideoId("https://www.youtube.com/shorts/bHIhgxav9LY"))
  }

  @Test
  fun `viral moment formats duration and time range`() {
    val moment = ViralMoment(
      title = "Incredible Reveal",
      hookScore = 9.8,
      startTimeSeconds = 15,
      endTimeSeconds = 45,
      reasoning = "High tension hook"
    )
    assertEquals(30, moment.durationSeconds)
    assertEquals("00:15 - 00:45 (30s)", moment.formattedTimeRange())
  }
}
