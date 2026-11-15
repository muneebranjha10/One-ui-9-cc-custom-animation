package com.example.data

enum class ControlCenterPresetId(val storageKey: String) {
    STYLE_01_CORNER_CASCADE("style_01_corner_cascade"),
    STYLE_02_UNIFIED_ZOOM("style_02_unified_zoom"),
    STYLE_03_WATERFALL_POP("style_03_waterfall_pop"),
    STYLE_04_TOP_CURTAIN("style_04_top_curtain"),
    STYLE_05_SILK_BLOOM("style_05_silk_bloom"),
    STOCK_SAMSUNG("stock_samsung");

    companion object {
        fun fromKey(key: String?): ControlCenterPresetId {
            return entries.firstOrNull { it.storageKey == key } ?: STYLE_01_CORNER_CASCADE
        }
    }
}

/**
 * Modular physics & visual profile for each distinct Control Center animation
 * observed in the 60fps reference video.
 */
data class AnimationPreset(
    val id: ControlCenterPresetId,
    val title: String,
    val subtitle: String,
    val videoTimestamp: String,
    val openingSummary: String,
    val closingSummary: String,
    val openDurationMs: Int,
    val closeDurationMs: Int,
    // Opening parameters
    val openStiffness: Float,
    val openDampingRatio: Float,
    val openTierStaggerMs: Int,
    val openStartScale: Float,
    val openStartTranslationYDp: Float,
    val openPerTierYStepDp: Float,
    val openDiagonalXShiftDp: Float,
    val openPivotX: Float,
    val openPivotY: Float,
    val openBlurRate: Float,
    val openAlphaCutoff: Float,
    // Closing parameters
    val closeStiffness: Float,
    val closeDampingRatio: Float,
    val closeTierStaggerMs: Int,
    val closeBottomLead: Boolean,
    val closeEndScale: Float,
    val closeEndTranslationYDp: Float,
    val closeDiagonalXShiftDp: Float,
    val closeBlurRate: Float,
    val closeAlphaCutoff: Float
)

object AnimationPresetRegistry {

    val videoPresets: List<AnimationPreset> = listOf(
        AnimationPreset(
            id = ControlCenterPresetId.STYLE_01_CORNER_CASCADE,
            title = "Animation 01 — Corner Radial Cascade",
            subtitle = "Top-right diagonal wave with 18ms tier stagger & subtle 1.5% spring settle; tight upward fade close.",
            videoTimestamp = "00:00.0 – 00:04.2 (21:18)",
            openingSummary = "Tiles emerge from top-right (-28 to -72dp Y, +18dp X bias, 0.85x scale) and settle with a subtle 1.5% spring bounce.",
            closingSummary = "Bottom tiles lead upward (-52 to -68dp Y, 0.88x scale); card alpha clears at 54% before blur finishes unveiling.",
            openDurationMs = 350,
            closeDurationMs = 205,
            openStiffness = 460f,
            openDampingRatio = 0.66f,
            openTierStaggerMs = 18,
            openStartScale = 0.85f,
            openStartTranslationYDp = -28f,
            openPerTierYStepDp = -11f,
            openDiagonalXShiftDp = 22f,
            openPivotX = 0.72f,
            openPivotY = 0f,
            openBlurRate = 26f,
            openAlphaCutoff = 0.65f,
            closeStiffness = 860f,
            closeDampingRatio = 1.0f,
            closeTierStaggerMs = 8,
            closeBottomLead = true,
            closeEndScale = 0.88f,
            closeEndTranslationYDp = -56f,
            closeDiagonalXShiftDp = 0f,
            closeBlurRate = 14f,
            closeAlphaCutoff = 0.54f
        ),
        AnimationPreset(
            id = ControlCenterPresetId.STYLE_02_UNIFIED_ZOOM,
            title = "Animation 02 — Unified Elastic Zoom",
            subtitle = "Zero-stagger synchronized sheet pop from 0.78x scale; symmetric backward zoom-shrink on close.",
            videoTimestamp = "00:04.4 – 00:09.4 (21:15)",
            openingSummary = "All Control Center tiers appear simultaneously as one cohesive glass sheet scaling from 0.78x -> 1.00x.",
            closingSummary = "Uniformly zooms backward into blur (1.00x -> 0.85x) with balanced opacity & blur fade in ~180ms.",
            openDurationMs = 210,
            closeDurationMs = 180,
            openStiffness = 620f,
            openDampingRatio = 0.72f,
            openTierStaggerMs = 0,
            openStartScale = 0.78f,
            openStartTranslationYDp = -18f,
            openPerTierYStepDp = 0f,
            openDiagonalXShiftDp = 0f,
            openPivotX = 0.5f,
            openPivotY = 0.18f,
            openBlurRate = 28f,
            openAlphaCutoff = 0.55f,
            closeStiffness = 760f,
            closeDampingRatio = 1.0f,
            closeTierStaggerMs = 0,
            closeBottomLead = false,
            closeEndScale = 0.85f,
            closeEndTranslationYDp = -16f,
            closeDiagonalXShiftDp = 0f,
            closeBlurRate = 18f,
            closeAlphaCutoff = 0.68f
        ),
        AnimationPreset(
            id = ControlCenterPresetId.STYLE_03_WATERFALL_POP,
            title = "Animation 03 — Sequential Waterfall Pop",
            subtitle = "Pronounced row-by-row & left-to-right tile stagger (-80dp drop) with ultra-snappy 140ms snap close.",
            videoTimestamp = "00:09.9 – 00:13.8 (21:16 · #1)",
            openingSummary = "Top-left connectivity cards lock in first while toggle grid, sliders, and bottom cards cascade sequentially (+28ms/tier).",
            closingSummary = "Rapid 140ms upward snap where card opacity drops within ~65ms while blur trails smoothly behind.",
            openDurationMs = 290,
            closeDurationMs = 145,
            openStiffness = 510f,
            openDampingRatio = 0.70f,
            openTierStaggerMs = 28,
            openStartScale = 0.82f,
            openStartTranslationYDp = -36f,
            openPerTierYStepDp = -14f,
            openDiagonalXShiftDp = -16f,
            openPivotX = 0.35f,
            openPivotY = 0f,
            openBlurRate = 30f,
            openAlphaCutoff = 0.60f,
            closeStiffness = 1040f,
            closeDampingRatio = 1.0f,
            closeTierStaggerMs = 6,
            closeBottomLead = true,
            closeEndScale = 0.90f,
            closeEndTranslationYDp = -74f,
            closeDiagonalXShiftDp = 0f,
            closeBlurRate = 19f,
            closeAlphaCutoff = 0.45f
        ),
        AnimationPreset(
            id = ControlCenterPresetId.STYLE_04_TOP_CURTAIN,
            title = "Animation 04 — Top-Drop Spring Curtain",
            subtitle = "Fast 50ms blur lock followed by unified vertical curtain drop (-120dp) & straight top slide-out.",
            videoTimestamp = "00:14.2 – 00:16.5 (21:16 · #2)",
            openingSummary = "Backdrop blurs almost instantly, then the entire aligned panel drops from the top edge (-120dp) with bouncy spring settle.",
            closingSummary = "Panel glides straight upward off the top bezel (-140dp Y) while blur clears behind it.",
            openDurationMs = 270,
            closeDurationMs = 160,
            openStiffness = 440f,
            openDampingRatio = 0.64f,
            openTierStaggerMs = 4,
            openStartScale = 0.94f,
            openStartTranslationYDp = -112f,
            openPerTierYStepDp = -3f,
            openDiagonalXShiftDp = 0f,
            openPivotX = 0.5f,
            openPivotY = 0f,
            openBlurRate = 36f,
            openAlphaCutoff = 0.50f,
            closeStiffness = 920f,
            closeDampingRatio = 1.0f,
            closeTierStaggerMs = 0,
            closeBottomLead = false,
            closeEndScale = 0.95f,
            closeEndTranslationYDp = -136f,
            closeDiagonalXShiftDp = 0f,
            closeBlurRate = 17f,
            closeAlphaCutoff = 0.72f
        ),
        AnimationPreset(
            id = ControlCenterPresetId.STYLE_05_SILK_BLOOM,
            title = "Animation 05 — Silk Glass Bloom",
            subtitle = "Critically damped zero-overshoot bloom in place (0.89x -> 1.00x) and gentle dissolve on close.",
            videoTimestamp = "00:16.5 – 00:19.5 (21:16 · #3)",
            openingSummary = "Cards stay near final vertical position (-14dp) and bloom smoothly outward with zero bounce (ζ = 0.96).",
            closingSummary = "Soft, unhurried in-place scale shrink (1.00x -> 0.92x) and translucent fade with zero upward jump.",
            openDurationMs = 260,
            closeDurationMs = 175,
            openStiffness = 380f,
            openDampingRatio = 0.96f,
            openTierStaggerMs = 6,
            openStartScale = 0.89f,
            openStartTranslationYDp = -14f,
            openPerTierYStepDp = -2f,
            openDiagonalXShiftDp = 0f,
            openPivotX = 0.5f,
            openPivotY = 0.5f,
            openBlurRate = 22f,
            openAlphaCutoff = 0.75f,
            closeStiffness = 620f,
            closeDampingRatio = 1.0f,
            closeTierStaggerMs = 0,
            closeBottomLead = false,
            closeEndScale = 0.92f,
            closeEndTranslationYDp = -10f,
            closeDiagonalXShiftDp = 0f,
            closeBlurRate = 16f,
            closeAlphaCutoff = 0.78f
        )
    )

    val stockSamsungPreset = AnimationPreset(
        id = ControlCenterPresetId.STOCK_SAMSUNG,
        title = "Stock Samsung Animation",
        subtitle = "Disables all custom LSPosed transforms and restores native One UI 9 Control Center animation.",
        videoTimestamp = "Native One UI 9",
        openingSummary = "Bypassed — Samsung SystemUI handles opening natively.",
        closingSummary = "Bypassed — Samsung SystemUI handles closing natively.",
        openDurationMs = 0,
        closeDurationMs = 0,
        openStiffness = 500f,
        openDampingRatio = 1.0f,
        openTierStaggerMs = 0,
        openStartScale = 1f,
        openStartTranslationYDp = 0f,
        openPerTierYStepDp = 0f,
        openDiagonalXShiftDp = 0f,
        openPivotX = 0.5f,
        openPivotY = 0.5f,
        openBlurRate = 25f,
        openAlphaCutoff = 1f,
        closeStiffness = 500f,
        closeDampingRatio = 1.0f,
        closeTierStaggerMs = 0,
        closeBottomLead = false,
        closeEndScale = 1f,
        closeEndTranslationYDp = 0f,
        closeDiagonalXShiftDp = 0f,
        closeBlurRate = 25f,
        closeAlphaCutoff = 1f
    )

    fun findById(id: ControlCenterPresetId): AnimationPreset {
        return if (id == ControlCenterPresetId.STOCK_SAMSUNG) {
            stockSamsungPreset
        } else {
            videoPresets.firstOrNull { it.id == id } ?: videoPresets.first()
        }
    }
}
