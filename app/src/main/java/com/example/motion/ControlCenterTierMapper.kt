package com.example.motion

import android.view.View
import android.view.ViewGroup
import kotlin.math.min

/**
 * Classifies live SystemUI Quick Settings / Control Center child views into the 5
 * spatial tiers and applies per-preset translationX/Y, pivot, scale, and alpha cleanly.
 */
object ControlCenterTierMapper {

    data class TieredViewRef(
        val view: View,
        val tierIndex: Int
    )

    fun mapHierarchy(root: ViewGroup): List<TieredViewRef> {
        val candidates = mutableListOf<View>()
        collectMeaningfulCards(root, depth = 0, out = candidates)
        if (candidates.isEmpty()) return emptyList()

        val rootHeight = root.height.coerceAtLeast(1).toFloat()
        return candidates.mapNotNull { view ->
            if (view.visibility == View.GONE || view.height <= 0) return@mapNotNull null
            val topY = relativeTop(view, root).toFloat()
            val normalizedY = (topY / rootHeight).coerceIn(0f, 1f)
            val tier = when {
                normalizedY < 0.14f -> 0
                normalizedY < 0.34f -> 1
                normalizedY < 0.58f -> 2
                normalizedY < 0.74f -> 3
                else -> 4
            }
            TieredViewRef(view = view, tierIndex = tier)
        }
    }

    fun applyTransforms(
        refs: List<TieredViewRef>,
        snapshot: SpringPhysicsSolver.FrameSnapshot,
        density: Float
    ) {
        for (ref in refs) {
            val v = ref.view
            val tier = snapshot.tiers[min(ref.tierIndex, snapshot.tiers.lastIndex)]
            v.pivotX = v.width * tier.pivotX
            v.pivotY = v.height * tier.pivotY
            v.translationX = tier.translationXDp * density
            v.translationY = tier.translationYDp * density
            v.scaleX = tier.scale
            v.scaleY = tier.scale
            v.alpha = tier.alpha
        }
    }

    fun resetAllViews(root: ViewGroup?) {
        root ?: return
        resetViewRecursive(root)
    }

    private fun resetViewRecursive(view: View) {
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                resetViewRecursive(view.getChildAt(i))
            }
        }
    }

    private fun collectMeaningfulCards(group: ViewGroup, depth: Int, out: MutableList<View>) {
        if (depth >= 3) {
            out.add(group)
            return
        }
        val childCount = group.childCount
        if (childCount == 0) return

        if (childCount in 2..12 && depth >= 1) {
            for (i in 0 until childCount) {
                val child = group.getChildAt(i)
                if (child.visibility != View.GONE) {
                    out.add(child)
                }
            }
            return
        }

        for (i in 0 until childCount) {
            val child = group.getChildAt(i)
            if (child is ViewGroup && child.visibility != View.GONE) {
                collectMeaningfulCards(child, depth + 1, out)
            } else if (child.visibility != View.GONE) {
                out.add(child)
            }
        }
    }

    private fun relativeTop(view: View, ancestor: ViewGroup): Int {
        var top = view.top
        var parent = view.parent
        while (parent is View && parent !== ancestor) {
            top += parent.top
            parent = parent.parent
        }
        return top
    }
}
