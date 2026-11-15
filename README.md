# One UI 9 — Ultra Motion Control Center (LSPosed Module)

Native Android LSPosed module and AMOLED companion manager recreating all **5 distinct Control Center opening and closing animations** identified in the 60fps reference video on **Samsung Galaxy S23 (SM-S911B, Android 17 / One UI 9, KernelSU + LSPosed)**.

---

## 1. Complete Reference Video Analysis (60fps, 1080×1920, 19.52s)

Frame-by-frame inspection of all 1,171 frames revealed **5 distinct animation segments**:

| Preset | Video Timestamp | Opening Behavior | Closing Behavior |
|---|---|---|---|
| **Animation 01 — Corner Radial Cascade** | `00:00.0 – 00:04.2` (`21:18`) | Top-right diagonal cascade (`+22dp` X bias, `-28` to `-72dp` Y, `0.85×` scale, `18ms`/tier stagger) settling with subtle `~1.5%` spring bounce (`~350ms`). | Bottom tiles lead upward (`-8ms` lead, `-52` to `-68dp` Y, `0.88×` scale); card alpha clears at `54%` before blur finishes unveiling (`~205ms`). |
| **Animation 02 — Unified Elastic Zoom** | `00:04.4 – 00:09.4` (`21:15`) | Zero-stagger synchronized glass sheet pop (`0.78× → 1.00×`, `k=620`, `ζ=0.72`, `~210ms`). | Symmetric backward zoom-shrink into blur (`1.00× → 0.85×`, `~180ms`). |
| **Animation 03 — Sequential Waterfall Pop** | `00:09.9 – 00:13.8` (`21:16 · #1`) | Strong top-left to bottom-right sequential tile cascade (`+28ms`/tier, `-36` to `-92dp` Y, `~290ms`). | Ultra-fast upward snap (`~145ms`) where card alpha clears within `~65ms` while blur trails smoothly. |
| **Animation 04 — Top-Drop Spring Curtain** | `00:14.2 – 00:16.5` (`21:16 · #2`) | Rapid `50ms` blur lock followed by aligned full-panel drop from top edge (`-112dp → 0dp`) with bouncy spring settle (`~270ms`). | Whole panel glides straight upward off top bezel (`-136dp` Y, `~160ms`). |
| **Animation 05 — Silk Glass Bloom** | `00:16.5 – 00:19.5` (`21:16 · #3`) | Critically damped in-place bloom (`0.89× → 1.00×`, `-14dp` Y, `ζ=0.96` zero bounce, `~260ms`). | Gentle in-place scale shrink (`1.00× → 0.92×`) and translucent dissolve (`~175ms`). |
| **Stock Samsung Animation** | Native | Disables all custom transforms and restores stock One UI 9 motion. | Native Samsung One UI 9 close. |

---

## 2. Persistent Settings & Apply + Soft Restart

- **Multi-Layer Persistence:** Selected preset & physics tuning are synchronously flushed via `.commit()` to **LSPosed New XSharedPreferences** (`xposedsharedprefs=true`, `MODE_WORLD_READABLE` with safe `MODE_PRIVATE` fallback) AND mirrored into **Device-Protected Storage** for Direct Boot `ConfigBridgeProvider` (`directBootAware="true"`) so selections survive SystemUI restarts, reboots, and locked-device startup.
- **Apply & Soft Restart:**
  1. Synchronously commits the selected preset.
  2. Shows an explicit confirmation dialog explaining that the status bar, navigation interface, and Control Center will briefly disappear for 2–3 seconds.
  3. Verifies KernelSU `su` access (`su -c id`) and restarts **only** `com.android.systemui` (`killall com.android.systemui` with `pkill -x com.android.systemui` fallback, fixed argument arrays, zero shell injection, 4-second anti-loop cooldown). Never reboots the entire device.

---

## 3. SystemUI Compatibility & Hook Verification

Because Samsung's One UI 9 `SystemUI.apk` was not supplied for static decompilation in this environment, hooks are split into **Verified AOSP APIs** and **Candidate One UI Classes** probed safely via `XposedHelpers.findClassIfExists`:

- **Verified AOSP APIs:** `QSFragment`, `QSImpl`, `NotificationPanelViewController`, `QuickSettingsControllerImpl`
- **Candidate One UI Classes (Verify against your `SystemUI.apk`):** `SecQSImpl`, `SecPanelExpansionStateNotifier`, `SecQsTransitionAnimator`

If any candidate hook reports **Class Absent** in **Hooks & Logs** on your SM-S911B, extract `/system_ext/priv-app/SystemUI/SystemUI.apk` (or provide it) so exact One UI 9 internal method signatures can be verified without guessing.

---

## 4. Installation, Safety & Recovery

1. Install `app-debug.apk` from GitHub Actions artifacts (or build via `gradle :app:assembleDebug`).
2. Enable **Ultra Motion** in **LSPosed Manager** with scope **System UI** (`com.android.systemui`) + **Ultra Motion**.
3. Open **Ultra Motion**, select any of the 5 presets (or preview them first), and tap **Apply & Soft Restart**.
4. **Recovery:** Select **Stock Samsung Animation** + **Apply & Soft Restart**, or disable the module in LSPosed Manager. An automatic Safety Circuit Breaker also resets all `View` transforms if any hook error or invisible expanded panel (`> 600ms`) occurs.
