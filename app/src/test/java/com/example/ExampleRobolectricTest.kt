package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnimationPresetRegistry
import com.example.data.ConfigRepository
import com.example.data.ControlCenterPresetId
import com.example.data.MotionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Ultra Motion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ultra Motion", appName)
    }

    @Test
    fun `config repository persists preset selection synchronously across reloads and stock fallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ConfigRepository(context)

        repo.selectPreset(ControlCenterPresetId.STYLE_04_TOP_CURTAIN, commitSync = true)
        val reloaded = ConfigRepository(context)
        assertEquals(ControlCenterPresetId.STYLE_04_TOP_CURTAIN, reloaded.config.value.selectedPresetId)
        assertTrue(reloaded.config.value.isCustomMotionActive)

        repo.selectPreset(ControlCenterPresetId.STOCK_SAMSUNG, commitSync = true)
        val stockReloaded = ConfigRepository(context)
        assertEquals(ControlCenterPresetId.STOCK_SAMSUNG, stockReloaded.config.value.selectedPresetId)
        assertFalse(stockReloaded.config.value.masterEnabled)
        assertFalse(stockReloaded.config.value.isCustomMotionActive)

        repo.updateConfig { it.copy(speedMultiplier = 5.0f, springIntensity = -2.0f) }
        assertEquals(MotionConfig.MAX_SPEED, repo.config.value.speedMultiplier, 0.001f)
        assertEquals(MotionConfig.MIN_SPRING, repo.config.value.springIntensity, 0.001f)

        assertEquals(5, AnimationPresetRegistry.videoPresets.size)
    }
}
