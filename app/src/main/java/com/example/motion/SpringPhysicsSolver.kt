package com.example.motion

import com.example.data.AnimationPreset
import com.example.data.ControlCenterPresetId
import com.example.data.MotionConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Multi-preset frame-time-based spring physics solver reproducing all 5 distinct
 * Control Center opening/closing styles identified in the 60fps reference video.
 */
class SpringPhysicsSolver {

    enum class Direction {
        IDLE,
        OPENING,
        CLOSING,
        DRAGGING
    }

    data class TierTransform(
        val progress: Float = 0f,
        val translationXDp: Float = 0f,
        val translationYDp: Float = 0f,
        val scale: Float = 1f,
        val alpha: Float = 0f,
        val pivotX: Float = 0.5f,
        val pivotY: Float = 0f
    )

    data class FrameSnapshot(
        val presetId: ControlCenterPresetId,
        val direction: Direction,
        val isAnimating: Boolean,
        val masterProgress: Float,
        val backdropBlurProgress: Float,
        val backdropDimAlpha: Float,
        val wallpaperScale: Float,
        val tiers: List<TierTransform>
    )

    private val tierPos = FloatArray(TIER_COUNT)
    private val tierVel = FloatArray(TIER_COUNT)
    private val tierDelayRemainingSec = FloatArray(TIER_COUNT)
    private val tierTransforms = Array(TIER_COUNT) { TierTransform() }

    private var backdropPos = 0f
    private var backdropVel = 0f
    private var direction: Direction = Direction.IDLE
    private var targetOpen: Boolean = false
    private var activeConfig: MotionConfig = MotionConfig.REFERENCE_VIDEO_DEFAULTS
    private var activePreset: AnimationPreset = activeConfig.activePreset

    val isRunning: Boolean
        get() = direction == Direction.OPENING || direction == Direction.CLOSING

    val currentDirection: Direction
        get() = direction

    val currentMasterProgress: Float
        get() {
            var sum = 0f
            for (i in 0 until TIER_COUNT) sum += tierPos[i]
            return (sum / TIER_COUNT).coerceIn(0f, 1.15f)
        }

    fun updateConfig(config: MotionConfig) {
        activeConfig = config
        activePreset = config.activePreset
    }

    fun snapToState(open: Boolean) {
        val target = if (open) 1f else 0f
        targetOpen = open
        direction = Direction.IDLE
        backdropPos = target
        backdropVel = 0f
        for (i in 0 until TIER_COUNT) {
            tierPos[i] = target
            tierVel[i] = 0f
            tierDelayRemainingSec[i] = 0f
        }
        recomputeTransforms()
    }

    fun setInteractiveDrag(rawProgress: Float, velocityPerSec: Float) {
        val clamped = rawProgress.coerceIn(0f, 1f)
        val prevMaster = currentMasterProgress
        direction = Direction.DRAGGING
        targetOpen = clamped >= prevMaster

        val lagScale = (activePreset.openTierStaggerMs / 18f).coerceIn(0f, 1.4f)
        for (i in 0 until TIER_COUNT) {
            val lagOffset = i * 0.028f * lagScale
            val mapped = ((clamped - lagOffset) / (1f - lagOffset * 0.7f)).coerceIn(0f, 1f)
            tierPos[i] = mapped
            tierVel[i] = velocityPerSec
            tierDelayRemainingSec[i] = 0f
        }
        backdropPos = min(1f, clamped * 1.35f)
        backdropVel = velocityPerSec
        recomputeTransforms()
    }

    fun animateTo(open: Boolean, initialVelocityPerSec: Float = 0f) {
        val wasRunningOpposite = isRunning && (targetOpen != open)
        val wasDragging = direction == Direction.DRAGGING
        targetOpen = open
        direction = if (open) Direction.OPENING else Direction.CLOSING

        val shouldBypass = !activeConfig.isCustomMotionActive ||
            (open && !activeConfig.openingEnabled) ||
            (!open && !activeConfig.closingEnabled)

        if (shouldBypass) {
            snapToState(open)
            return
        }

        val speed = activeConfig.clampedSpeed
        for (i in 0 until TIER_COUNT) {
            if (wasDragging) {
                tierVel[i] = initialVelocityPerSec
                tierDelayRemainingSec[i] = 0f
            } else if (wasRunningOpposite) {
                tierVel[i] = tierVel[i] * 0.5f + initialVelocityPerSec
                tierDelayRemainingSec[i] = 0f
            } else {
                tierVel[i] += initialVelocityPerSec
                val staggerMs = if (open) {
                    i * activePreset.openTierStaggerMs
                } else if (activePreset.closeBottomLead) {
                    (TIER_COUNT - 1 - i) * activePreset.closeTierStaggerMs
                } else {
                    i * activePreset.closeTierStaggerMs
                }
                tierDelayRemainingSec[i] = (staggerMs / 1000f) / speed
            }
        }
        backdropVel = if (wasDragging) initialVelocityPerSec else backdropVel
    }

    fun step(dtSeconds: Float): FrameSnapshot {
        if (direction == Direction.IDLE || direction == Direction.DRAGGING) {
            return snapshot()
        }

        val dt = dtSeconds.coerceIn(0.0005f, 0.034f)
        val speed = activeConfig.clampedSpeed
        val springFactor = activeConfig.clampedSpring
        val target = if (targetOpen) 1f else 0f

        val (stiffness, dampingRatio) = if (targetOpen) {
            val k = activePreset.openStiffness * speed * speed
            val zeta = (1.0f - (1.0f - activePreset.openDampingRatio) * springFactor)
                .coerceIn(0.45f, 1.0f)
            k to zeta
        } else {
            val k = activePreset.closeStiffness * speed * speed
            k to activePreset.closeDampingRatio
        }

        val omega0 = sqrt(stiffness)
        val dampingCoeff = 2f * dampingRatio * omega0

        val subSteps = max(1, ((dt / MAX_SUB_STEP_SEC) + 0.999f).toInt())
        val subDt = dt / subSteps

        var allSettled = true
        for (i in 0 until TIER_COUNT) {
            val remainingDelay = tierDelayRemainingSec[i]
            var activeDt = dt
            if (remainingDelay > 0f) {
                if (remainingDelay >= dt) {
                    tierDelayRemainingSec[i] = remainingDelay - dt
                    allSettled = false
                    continue
                } else {
                    activeDt = dt - remainingDelay
                    tierDelayRemainingSec[i] = 0f
                }
            }

            val tierSubSteps = max(1, ((activeDt / MAX_SUB_STEP_SEC) + 0.999f).toInt())
            val tierSubDt = activeDt / tierSubSteps
            var x = tierPos[i]
            var v = tierVel[i]

            for (s in 0 until tierSubSteps) {
                val force = -stiffness * (x - target) - dampingCoeff * v
                v += force * tierSubDt
                x += v * tierSubDt
                if (!targetOpen && x <= 0f) {
                    x = 0f
                    v = 0f
                    break
                }
            }

            if (abs(x - target) < SETTLE_POS_EPSILON && abs(v) < SETTLE_VEL_EPSILON) {
                tierPos[i] = target
                tierVel[i] = 0f
            } else {
                tierPos[i] = x
                tierVel[i] = v
                allSettled = false
            }
        }

        val blurTarget = if (targetOpen) 1f else 0f
        val blurRate = if (targetOpen) activePreset.openBlurRate * speed else activePreset.closeBlurRate * speed
        for (s in 0 until subSteps) {
            val delta = blurTarget - backdropPos
            backdropPos += delta * min(1f, blurRate * subDt)
        }
        if (abs(backdropPos - blurTarget) < 0.003f) {
            backdropPos = blurTarget
            backdropVel = 0f
        } else {
            allSettled = false
        }

        if (allSettled) {
            direction = Direction.IDLE
        }

        recomputeTransforms()
        return snapshot()
    }

    fun snapshot(): FrameSnapshot {
        val blur = backdropPos.coerceIn(0f, 1f)
        return FrameSnapshot(
            presetId = activePreset.id,
            direction = direction,
            isAnimating = isRunning,
            masterProgress = currentMasterProgress,
            backdropBlurProgress = blur,
            backdropDimAlpha = blur * 0.44f,
            wallpaperScale = 1.0f - (0.04f * blur),
            tiers = tierTransforms.toList()
        )
    }

    private fun recomputeTransforms() {
        val isClosingMode = direction == Direction.CLOSING ||
            (direction == Direction.IDLE && !targetOpen) ||
            (direction == Direction.DRAGGING && !targetOpen)

        for (i in 0 until TIER_COUNT) {
            val p = tierPos[i]
            if (!isClosingMode) {
                val startTranslationY = activePreset.openStartTranslationYDp + i * activePreset.openPerTierYStepDp
                val startTranslationX = activePreset.openDiagonalXShiftDp * ((TIER_COUNT - 1 - i) / 4f)
                val startScale = if (i == 0) {
                    max(activePreset.openStartScale, 0.90f)
                } else {
                    activePreset.openStartScale
                }
                val ty = startTranslationY * (1f - p)
                val tx = startTranslationX * (1f - p)
                val scale = startScale + (1f - startScale) * p
                val alpha = (p / activePreset.openAlphaCutoff.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                tierTransforms[i] = TierTransform(
                    progress = p,
                    translationXDp = tx,
                    translationYDp = ty,
                    scale = scale,
                    alpha = alpha,
                    pivotX = activePreset.openPivotX,
                    pivotY = activePreset.openPivotY
                )
            } else {
                val u = (1f - p).coerceIn(0f, 1f)
                val accelU = u * u * 0.35f + u * 0.65f
                val tierWeight = if (activePreset.closeBottomLead) (TIER_COUNT - 1 - i) * 4f else 0f
                val maxLift = activePreset.closeEndTranslationYDp - tierWeight
                val ty = maxLift * accelU
                val tx = activePreset.closeDiagonalXShiftDp * accelU
                val scale = 1f - (1f - activePreset.closeEndScale) * u
                val alpha = (1f - (u / activePreset.closeAlphaCutoff.coerceAtLeast(0.1f))).coerceIn(0f, 1f)
                tierTransforms[i] = TierTransform(
                    progress = p,
                    translationXDp = tx,
                    translationYDp = ty,
                    scale = scale,
                    alpha = alpha,
                    pivotX = activePreset.openPivotX,
                    pivotY = activePreset.openPivotY
                )
            }
        }
    }

    companion object {
        const val TIER_COUNT = 5
        private const val MAX_SUB_STEP_SEC = 1f / 240f
        private const val SETTLE_POS_EPSILON = 0.0012f
        private const val SETTLE_VEL_EPSILON = 0.015f
    }
}
