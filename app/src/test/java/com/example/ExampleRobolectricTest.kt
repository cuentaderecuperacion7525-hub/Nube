package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.ImmichClientFactory
import org.junit.Assert.assertEquals
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
        assertEquals("ImmichVault", appName)
    }

    @Test
    fun `normalizeRootUrl strips trailing slash and api suffix`() {
        assertEquals(
            "https://demo.immich.app",
            ImmichClientFactory.normalizeRootUrl("demo.immich.app/api/"),
        )
        assertEquals(
            "http://192.168.1.50:2283",
            ImmichClientFactory.normalizeRootUrl("http://192.168.1.50:2283/"),
        )
        assertEquals(
            "https://demo.immich.app/api/",
            ImmichClientFactory.normalizeApiBaseUrl("https://demo.immich.app"),
        )
    }
}
