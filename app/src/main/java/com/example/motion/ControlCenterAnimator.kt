package com.example.motion

import android.view.Choreographer
import android.view.ViewGroup
import com.example.data.MotionConfig
import java.lang.ref.WeakReference

/**
 * Choreographer-synchronized animation coordinator for hooked SystemUI panel containers.
 * Includes a built-in safety watchdog to prevent stuck/invisible panels or crash loops.
 */
class ControlCenterAnimator(
    private val onSafetyTrip: (reason: String) -> Unit,
    private val onFrameTelemetry: (frameMs: Float) -> Unit = {}
) : Choreographer.FrameCallback {

    private val solver = SpringPhysicsSolver()
    private var rootRef: WeakReference<ViewGroup>? = null
    private var cachedTiers: List<ControlCenterTierMapper.TieredViewRef> = emptyList()
    private var config: MotionConfig = MotionConfig.REFERENCE_VIDEO_DEFAULTS

    private var frameCallbackScheduled = false
    private var lastFrameNanos = 0L
    private var lastExpansionFraction = 0f
    private var lastExpansionEventNanos = 0L
    private var invisibleFullOpenStartNanos = 0L
    private var isTripped = false

    fun attachRoot(root: ViewGroup) {
        val current = rootRef?.get()
        if (current !== root) {
            rootRef = WeakReference(root)
            cachedTiers = emptyList()
        }
    }

    fun updateConfig(newConfig: MotionConfig) {
        config = newConfig
        solver.updateConfig(newConfig)
        if (!newConfig.isCustomMotionActive) {
            stopAndReset()
        }
    }

    fun onExpansionChanged(fraction: Float, tracking: Boolean) {
        if (isTripped || !config.isCustomMotionActive) return

        val root = rootRef?.get() ?: return
        val nowNanos = System.nanoTime()
        val dtSec = if (lastExpansionEventNanos > 0L) {
            ((nowNanos - lastExpansionEventNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        } else {
            0.016f
        }
        val estVelocity = (fraction - lastExpansionFraction) / dtSec
        val wasNearClosed = lastExpansionFraction <= 0.03f
        val wasNearOpen = lastExpansionFraction >= 0.97f

        lastExpansionFraction = fraction
        lastExpansionEventNanos = nowNanos

        if (cachedTiers.isEmpty() && root.childCount > 0 && root.height > 0) {
            cachedTiers = ControlCenterTierMapper.mapHierarchy(root)
        }

        if (tracking) {
            if ((fraction > 0.02f && config.openingEnabled) || (fraction < 0.98f && config.closingEnabled)) {
                solver.setInteractiveDrag(fraction, estVelocity)
                applySnapshot(root, solver.snapshot())
                ensureFrameLoopStopped()
            }
        } else {
            when {
                fraction >= 0.97f && !wasNearOpen -> {
                    if (config.openingEnabled) {
                        solver.animateTo(open = true, initialVelocityPerSec = estVelocity.coerceIn(0f, 6f))
                        ensureFrameLoopRunning()
                    } else {
                        solver.snapToState(open = true)
                        applySnapshot(root, solver.snapshot())
                    }
                }
                fraction <= 0.03f && !wasNearClosed -> {
                    if (config.closingEnabled) {
                        solver.animateTo(open = false, initialVelocityPerSec = estVelocity.coerceIn(-8f, 0f))
                        ensureFrameLoopRunning()
                    } else {
                        solver.snapToState(open = false)
                        applySnapshot(root, solver.snapshot())
                    }
                }
                fraction in 0.04f..0.96f -> {
                    // Mid-fling or interrupted gesture target detection
                    val movingOpen = estVelocity >= 0f
                    if ((movingOpen && config.openingEnabled) || (!movingOpen && config.closingEnabled)) {
                        solver.animateTo(open = movingOpen, initialVelocityPerSec = estVelocity.coerceIn(-8f, 8f))
                        ensureFrameLoopRunning()
                    }
                }
            }
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameCallbackScheduled = false
        if (isTripped || !config.isCustomMotionActive) {
            stopAndReset()
            return
        }

        val root = rootRef?.get()
        if (root == null || !root.isAttachedToWindow) {
            lastFrameNanos = 0L
            return
        }

        val dtSec = if (lastFrameNanos > 0L) {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.034f)
        } else {
            0.0083f
        }
        lastFrameNanos = frameTimeNanos

        try {
            val snapshot = solver.step(dtSec)
            applySnapshot(root, snapshot)
            onFrameTelemetry(dtSec * 1000f)

            // Stuck-panel watchdog: if SystemUI reports fully expanded but tier alpha stays <0.1 for >600ms
            val avgAlpha = snapshot.tiers.map { it.alpha }.average().toFloat()
            if (lastExpansionFraction >= 0.99f && avgAlpha < 0.1f) {
                if (invisibleFullOpenStartNanos == 0L) {
                    invisibleFullOpenStartNanos = frameTimeNanos
                } else if (frameTimeNanos - invisibleFullOpenStartNanos > STUCK_WATCHDOG_NANOS) {
                    tripBreaker("Watchdog detected invisible expanded panel (>600ms). Restored original animations.")
                    return
                }
            } else {
                invisibleFullOpenStartNanos = 0L
            }

            if (snapshot.isAnimating) {
                ensureFrameLoopRunning()
            } else {
                lastFrameNanos = 0L
            }
        } catch (t: Throwable) {
            tripBreaker("Frame animation exception: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    fun stopAndReset() {
        ensureFrameLoopStopped()
        lastFrameNanos = 0L
        invisibleFullOpenStartNanos = 0L
        ControlCenterTierMapper.resetAllViews(rootRef?.get())
    }

    private fun tripBreaker(reason: String) {
        isTripped = true
        stopAndReset()
        onSafetyTrip(reason)
    }

    private fun applySnapshot(root: ViewGroup, snapshot: SpringPhysicsSolver.FrameSnapshot) {
        if (cachedTiers.isEmpty() && root.childCount > 0 && root.height > 0) {
            cachedTiers = ControlCenterTierMapper.mapHierarchy(root)
        }
        val density = root.resources.displayMetrics.density
        ControlCenterTierMapper.applyTransforms(cachedTiers, snapshot, density)
    }

    private fun ensureFrameLoopRunning() {
        if (!frameCallbackScheduled) {
            frameCallbackScheduled = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun ensureFrameLoopStopped() {
        if (frameCallbackScheduled) {
            frameCallbackScheduled = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    companion object {
        private const val STUCK_WATCHDOG_NANOS = 600_000_000L
    }
}
