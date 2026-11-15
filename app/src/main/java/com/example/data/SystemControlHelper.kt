package com.example.data

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class DeviceEnvironmentInfo(
    val model: String,
    val manufacturer: String,
    val androidRelease: String,
    val sdkInt: Int,
    val oneUiHint: String,
    val currentRefreshHz: Float,
    val supportedRefreshRatesHz: List<Float>,
    val isSamsungDevice: Boolean,
    val rootBinaryDetected: Boolean
)

object SystemControlHelper {

    private val suSearchPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su"
    )

    fun hasSuBinary(): Boolean {
        return suSearchPaths.any { File(it).exists() }
    }

    fun inspectDevice(context: Context): DeviceEnvironmentInfo {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val currentHz = display?.mode?.refreshRate ?: 60f
        val supportedHz = display?.supportedModes
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sortedDescending()
            ?: listOf(currentHz)

        return DeviceEnvironmentInfo(
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            androidRelease = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            oneUiHint = readOneUiVersionProperty(),
            currentRefreshHz = currentHz,
            supportedRefreshRatesHz = supportedHz,
            isSamsungDevice = Build.MANUFACTURER.equals("samsung", ignoreCase = true),
            rootBinaryDetected = hasSuBinary()
        )
    }

    private fun readOneUiVersionProperty(): String {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
            val raw = (getMethod.invoke(null, "ro.build.version.oneui", "") as? String).orEmpty()
            if (raw.isBlank()) {
                "Non-Samsung / Emulator Build"
            } else {
                val numeric = raw.toIntOrNull()
                if (numeric != null && numeric >= 10000) {
                    val major = numeric / 10000
                    val minor = (numeric % 10000) / 100
                    "One UI $major.$minor ($raw)"
                } else {
                    "One UI ($raw)"
                }
            }
        } catch (_: Throwable) {
            "Unavailable"
        }
    }

    sealed interface CommandResult {
        data class Success(val message: String) : CommandResult
        data class Failure(val error: String, val troubleshooting: String) : CommandResult
    }

    /**
     * Checks root access via `su -c id` without shell interpolation, then restarts
     * ONLY `com.android.systemui` (never the whole device). Always gated behind an
     * explicit user confirmation dialog.
     */
    suspend fun restartSystemUiWithRoot(): CommandResult = withContext(Dispatchers.IO) {
        val suExecutable = suSearchPaths.firstOrNull { File(it).canExecute() } ?: "su"

        // Step 1: Verify KernelSU / su permission cleanly before issuing restart
        val checkResult = runFixedSuCommand(suExecutable, "id")
        if (checkResult.isFailure) {
            val err = checkResult.exceptionOrNull()?.message ?: "Root check failed."
            return@withContext CommandResult.Failure(
                error = err,
                troubleshooting = "Your selected preset has been saved. Open KernelSU Manager -> Superuser -> grant root access to Ultra Motion, then tap 'Apply & Soft Restart' again."
            )
        }

        // Step 2: Restart ONLY com.android.systemui via fixed argument array (zero shell injection)
        val killResult = runFixedSuCommand(suExecutable, "killall com.android.systemui")
        if (killResult.isSuccess) {
            return@withContext CommandResult.Success(
                "Preset saved and com.android.systemui soft restart triggered. SystemUI will reload your selected animation within 2–3 seconds."
            )
        }

        // Fallback to pkill -x com.android.systemui on builds without toybox killall
        val pkillResult = runFixedSuCommand(suExecutable, "pkill -x com.android.systemui")
        if (pkillResult.isSuccess) {
            return@withContext CommandResult.Success(
                "Preset saved and com.android.systemui soft restart triggered via pkill. SystemUI will reload your selected animation within 2–3 seconds."
            )
        }

        val errMsg = killResult.exceptionOrNull()?.message
            ?: pkillResult.exceptionOrNull()?.message
            ?: "Unable to signal com.android.systemui."
        CommandResult.Failure(
            error = errMsg,
            troubleshooting = "Your selected preset remains safely saved on disk. Verify KernelSU superuser permission for this app or toggle Ultra Motion off/on in LSPosed Manager if SystemUI did not respond."
        )
    }

    private fun runFixedSuCommand(suPath: String, command: String): Result<String> {
        return try {
            val process = ProcessBuilder(suPath, "-c", command)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return Result.failure(IllegalStateException("Timed out waiting for KernelSU/su prompt."))
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.exitValue() == 0) {
                Result.success(output)
            } else {
                Result.failure(
                    IllegalStateException(
                        output.ifBlank { "Command '$command' exited with code ${process.exitValue()}." }
                    )
                )
            }
        } catch (t: Throwable) {
            Result.failure(
                IllegalStateException("Root binary '$suPath' unavailable: ${t.message ?: "not rooted"}")
            )
        }
    }
}
