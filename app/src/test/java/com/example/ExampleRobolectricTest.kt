package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
  fun `app name matches MailVault branding`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MailVault", appName)
  }

  @Test
  fun `build config exposes verified MailTm base url and toolchain versions`() {
    assertEquals("https://api.mail.tm/", BuildConfig.MAIL_TM_BASE_URL)
    assertTrue(BuildConfig.AGP_VERSION.isNotBlank())
    assertTrue(BuildConfig.KOTLIN_VERSION.isNotBlank())
    assertTrue(BuildConfig.COMPOSE_BOM_VERSION.isNotBlank())
    assertTrue(BuildConfig.GRADLE_VERSION.isNotBlank())
  }
}
