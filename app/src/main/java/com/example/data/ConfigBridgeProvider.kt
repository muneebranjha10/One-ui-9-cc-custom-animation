package com.example.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

/**
 * Direct-Boot-aware IPC bridge allowing the hooked SystemUI process to read the
 * active preset and report runtime diagnostics even before first unlock after reboot.
 */
class ConfigBridgeProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val repo = ConfigRepository(ctx)
        val cfg = repo.config.value
        val cursor = MatrixCursor(
            arrayOf(
                MotionConfig.KEY_SELECTED_PRESET_ID,
                MotionConfig.KEY_MASTER_ENABLED,
                MotionConfig.KEY_OPENING_ENABLED,
                MotionConfig.KEY_CLOSING_ENABLED,
                MotionConfig.KEY_SPEED_MULTIPLIER,
                MotionConfig.KEY_SPRING_INTENSITY,
                MotionConfig.KEY_SAFE_RECOVERY_TRIPPED,
                MotionConfig.KEY_CONFIG_VERSION
            )
        )
        cursor.addRow(
            arrayOf<Any>(
                cfg.selectedPresetId.storageKey,
                if (cfg.masterEnabled) 1 else 0,
                if (cfg.openingEnabled) 1 else 0,
                if (cfg.closingEnabled) 1 else 0,
                cfg.clampedSpeed,
                cfg.clampedSpring,
                if (cfg.safeRecoveryTripped) 1 else 0,
                cfg.configVersion
            )
        )
        return cursor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = context ?: return Bundle.EMPTY
        val repo = ConfigRepository(ctx)
        return when (method) {
            METHOD_GET_CONFIG -> {
                val cfg = repo.config.value
                Bundle().apply {
                    putString(MotionConfig.KEY_SELECTED_PRESET_ID, cfg.selectedPresetId.storageKey)
                    putBoolean(MotionConfig.KEY_MASTER_ENABLED, cfg.masterEnabled)
                    putBoolean(MotionConfig.KEY_OPENING_ENABLED, cfg.openingEnabled)
                    putBoolean(MotionConfig.KEY_CLOSING_ENABLED, cfg.closingEnabled)
                    putFloat(MotionConfig.KEY_SPEED_MULTIPLIER, cfg.clampedSpeed)
                    putFloat(MotionConfig.KEY_SPRING_INTENSITY, cfg.clampedSpring)
                    putBoolean(MotionConfig.KEY_SAFE_RECOVERY_TRIPPED, cfg.safeRecoveryTripped)
                    putLong(MotionConfig.KEY_CONFIG_VERSION, cfg.configVersion)
                }
            }
            METHOD_REPORT_DIAGNOSTICS -> {
                if (extras != null) {
                    val discovered = extras.getStringArrayList(EXTRA_DISCOVERED) ?: emptyList()
                    val missing = extras.getStringArrayList(EXTRA_MISSING) ?: emptyList()
                    repo.recordSystemUiDiagnostics(
                        discoveredClasses = discovered,
                        missingClasses = missing,
                        activeHookCount = extras.getInt(EXTRA_HOOK_COUNT, 0),
                        refreshHz = extras.getFloat(EXTRA_REFRESH_HZ, 120f),
                        frameMs = extras.getFloat(EXTRA_FRAME_MS, 0f),
                        breakerTripped = extras.getBoolean(EXTRA_BREAKER_TRIPPED, false),
                        breakerReason = extras.getString(EXTRA_BREAKER_REASON, "") ?: ""
                    )
                }
                Bundle().apply { putBoolean("ok", true) }
            }
            else -> Bundle.EMPTY
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val METHOD_GET_CONFIG = "get_config"
        const val METHOD_REPORT_DIAGNOSTICS = "report_diagnostics"
        const val EXTRA_DISCOVERED = "discovered"
        const val EXTRA_MISSING = "missing"
        const val EXTRA_HOOK_COUNT = "hook_count"
        const val EXTRA_REFRESH_HZ = "refresh_hz"
        const val EXTRA_FRAME_MS = "frame_ms"
        const val EXTRA_BREAKER_TRIPPED = "breaker_tripped"
        const val EXTRA_BREAKER_REASON = "breaker_reason"
    }
}
