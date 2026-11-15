package com.example.xposed

import de.robv.android.xposed.XposedHelpers

/**
 * Safely probes SystemUI's ClassLoader at runtime without throwing on absent
 * or refactored One UI 9 classes.
 */
object SystemUiProbe {

    data class ProbeReport(
        val discovered: Map<String, Class<*>>,
        val missing: List<String>
    )

    fun inspect(classLoader: ClassLoader): ProbeReport {
        val discovered = linkedMapOf<String, Class<*>>()
        val missing = mutableListOf<String>()

        for (descriptor in HookVerificationRegistry.baselineHooks) {
            val clazz = XposedHelpers.findClassIfExists(descriptor.targetClass, classLoader)
            if (clazz != null) {
                discovered[descriptor.targetClass] = clazz
            } else {
                missing.add(descriptor.targetClass)
            }
        }
        return ProbeReport(discovered = discovered, missing = missing)
    }
}
