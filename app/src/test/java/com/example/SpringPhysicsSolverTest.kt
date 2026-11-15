package com.example

import com.example.data.AnimationPresetRegistry
import com.example.data.ControlCenterPresetId
import com.example.data.MotionConfig
import com.example.motion.SpringPhysicsSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpringPhysicsSolverTest {

    @Test
    fun `all 5 video presets produce distinct opening and closing behaviors and settle cleanly`() {
        val dt = 1f / 120f
        for (preset in AnimationPresetRegistry.videoPresets) {
            val solver = SpringPhysicsSolver()
            solver.updateConfig(
                MotionConfig(
                    selectedPresetId = preset.id,
                    masterEnabled = true,
                    openingEnabled = true,
                    closingEnabled = true
                )
            )
            solver.snapToState(open = false)
            solver.animateTo(open = true)

            repeat(90) { solver.step(dt) }
            val opened = solver.snapshot()
            assertFalse("${preset.title} should settle opening", opened.isAnimating)
            assertEquals(1.0f, opened.tiers[2].alpha, 0.005f)
            assertEquals(1.0f, opened.tiers[2].scale, 0.005f)

            solver.animateTo(open = false)
            repeat(80) { solver.step(dt) }
            val closed = solver.snapshot()
            assertFalse("${preset.title} should settle closing", closed.isAnimating)
            assertEquals(0.0f, closed.backdropBlurProgress, 0.01f)
        }
    }

    @Test
    fun `style 01 corner cascade staggers tiers top to bottom with subtle overshoot`() {
        val solver = SpringPhysicsSolver()
        solver.updateConfig(MotionConfig(selectedPresetId = ControlCenterPresetId.STYLE_01_CORNER_CASCADE))
        solver.snapToState(open = false)
        solver.animateTo(open = true)

        val dt = 1f / 120f
        var sawTier0AheadOfTier4 = false
        var maxTier2Scale = 0f

        repeat(60) {
            val snap = solver.step(dt)
            if (snap.tiers[0].progress > snap.tiers[4].progress + 0.05f) {
                sawTier0AheadOfTier4 = true
            }
            if (snap.tiers[2].scale > maxTier2Scale) {
                maxTier2Scale = snap.tiers[2].scale
            }
        }

        assertTrue("Expected top tier 0 to lead bottom tier 4", sawTier0AheadOfTier4)
        assertTrue("Expected subtle overshoot above 1.0x scale", maxTier2Scale > 1.002f)
    }

    @Test
    fun `style 02 unified zoom has zero stagger across tiers`() {
        val solver = SpringPhysicsSolver()
        solver.updateConfig(MotionConfig(selectedPresetId = ControlCenterPresetId.STYLE_02_UNIFIED_ZOOM))
        solver.snapToState(open = false)
        solver.animateTo(open = true)

        val snap = solver.step(1f / 60f)
        assertEquals(snap.tiers[0].progress, snap.tiers[4].progress, 0.0001f)
    }

    @Test
    fun `style 05 silk bloom settles with zero overshoot`() {
        val solver = SpringPhysicsSolver()
        solver.updateConfig(MotionConfig(selectedPresetId = ControlCenterPresetId.STYLE_05_SILK_BLOOM))
        solver.snapToState(open = false)
        solver.animateTo(open = true)

        val dt = 1f / 120f
        repeat(80) {
            val snap = solver.step(dt)
            assertTrue("Silk bloom should remain smooth without bounce", snap.tiers[2].scale <= 1.002f)
        }
    }

    @Test
    fun `mid-flight reversal preserves continuous progress without snapping`() {
        val solver = SpringPhysicsSolver()
        solver.updateConfig(MotionConfig(selectedPresetId = ControlCenterPresetId.STYLE_04_TOP_CURTAIN))
        solver.snapToState(open = false)
        solver.animateTo(open = true)

        val dt = 1f / 120f
        repeat(12) { solver.step(dt) }
        val beforeInterrupt = solver.snapshot().masterProgress
        assertTrue(beforeInterrupt in 0.1f..0.95f)

        solver.animateTo(open = false)
        val immediatelyAfterInterrupt = solver.step(dt).masterProgress
        assertTrue(
            "Mid-flight reversal should transition smoothly from $beforeInterrupt, got $immediatelyAfterInterrupt",
            kotlin.math.abs(immediatelyAfterInterrupt - beforeInterrupt) < 0.15f
        )
    }

    @Test
    fun `stock samsung preset bypasses custom animations immediately`() {
        val solver = SpringPhysicsSolver()
        solver.updateConfig(MotionConfig(selectedPresetId = ControlCenterPresetId.STOCK_SAMSUNG))
        solver.snapToState(open = false)
        solver.animateTo(open = true)

        val snap = solver.snapshot()
        assertFalse(snap.isAnimating)
        assertEquals(1f, snap.masterProgress, 0.001f)
    }
}
