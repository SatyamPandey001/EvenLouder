# EvenLouder - Implementation Plan & Progress Tracker

## Status: COMPLETE (Ready for On-Device Testing)
* **Target:** Android 15+ (minSdk 35, targetSdk 35, compileSdk 35)
* **Audio Engine:** `android.media.audiofx.DynamicsProcessing` (AIDL compliant, 13 bands total $\le 32$ bands)
* **APK Output:** `app/build/outputs/apk/debug/app-debug.apk` (Built & Verified)

---

## The 6 Execution Chunks Summary

```
[Chunk 1: Scaffolding & SDK]              ──► DONE (Android 15 Platform 35 SDK + AGP 8.7.2 + Kotlin 2.0.21)
[Chunk 2: DSP Core Engine]                 ──► DONE (DynamicsEngine.kt: High-pass + Vocal + MBC + Limiter)
[Chunk 3: Built-In Test Audio Generator]   ──► DONE (AudioTestPlayer.kt: Voice chord + 70Hz stress + Noise)
[Chunk 4: Session Interceptor & Service]   ──► DONE (AudioEngineService.kt + AudioSessionReceiver.kt)
[Chunk 5: Precision Functional UI]         ──► DONE (DashboardScreen.kt + MainActivity.kt)
[Chunk 6: Build, Assemble & Verify APK]    ──► DONE (Unit tests passed, assembleDebug clean exit)
```

---

## Detailed Task Checklist

### Chunk 1: Scaffolding & Android 15 Toolchain
- [x] Install `platforms;android-35` via Android SDK manager.
- [x] Create `settings.gradle.kts` and root `build.gradle.kts`.
- [x] Create `gradle.properties` (AndroidX, JVM args, Jetpack Compose compiler config).
- [x] Configure `app/build.gradle.kts` with compileSdk 35, minSdk 35, Compose BOM, Material3.
- [x] Create `app/src/main/AndroidManifest.xml` with permissions and service declarations.

### Chunk 2: Core Audio DSP Engine (`com.evenlouder.app.core.dsp`)
- [x] `DspParameters.kt`: Data structures for Pre-EQ (high-pass + presence), 3-band MBC, and Limiter.
- [x] `DynamicsEngine.kt`:
  - Enforce strictly $\le 32$ total bands for Android 15 AIDL HAL compliance (13 active bands configured).
  - Construct `DynamicsProcessing.Config` (Pre-EQ: 6 bands, MBC: 3 bands, Post-EQ: 4 bands, Limiter: 1 band).
  - Implemented dynamic runtime parameter updates without audio glitching.
- [x] `AcousticTuningProfiles.kt`: Presets:
  - *Balanced Clarity* (sub-bass protection with balanced vocal presence).
  - *Vocal & Dialogue* (160 Hz cut + max dialogue clarity).
  - *Max Safe Loudness* (controlled MBC makeup gain + strict -0.5 dBFS brickwall ceiling).
  - *Hardware Bypass* (clean A/B comparison against stock sound).
- [x] `DspEngineTest.kt`: Unit tests verifying band counts $\le 32$, frequency bounds, and limiter ceiling.

### Chunk 3: Built-in Audio Test Generator (`com.evenlouder.app.core.testplayer`)
- [x] `AudioTestPlayer.kt`: Self-contained PCM audio generator using `AudioTrack` with accessible `audioSessionId`:
  - *Speech / Dialogue Chord* (300 Hz, 900 Hz, 2800 Hz) to audit vocal presence boost.
  - *Excursion Stress Tone* (70 Hz sub-bass + 2.5 kHz clarity tone) proving the high-pass excursion filter.
  - *Acoustic Dynamic Noise* (pseudo-random roll-off noise) auditing the 3-band compressor and limiter ceiling.

### Chunk 4: Session Interception & Background Service (`com.evenlouder.app.core.session`)
- [x] `AudioEngineService.kt`:
  - Foreground service with `android:foregroundServiceType="mediaPlayback"`.
  - Maintains `ConcurrentHashMap<Int, DynamicsProcessing>` managing active sessions.
  - Notification channel with ongoing status and active session counts.
- [x] `AudioSessionReceiver.kt`: BroadcastReceiver intercepting `ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` and `ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION`.

### Chunk 5: Precision Functional UI (`com.evenlouder.app.ui`)
- [x] `MainActivity.kt`: Compose host with permission handlers (`POST_NOTIFICATIONS`) and reactive service binding.
- [x] `theme/Theme.kt`: Minimal high-contrast dark theme (Neon Cyan `#00E5FF` / Neon Amber `#FFB300`).
- [x] `screens/DashboardScreen.kt`:
  - Master DSP Enable / Bypass switch (instant A/B testing).
  - Acoustic Profile chips (Balanced, Vocal Focus, Max Loudness).
  - Loudness Boost Slider (0 dB to +6 dB).
  - Vocal Presence Slider (0 dB to +6 dB at 2.8 kHz).
  - Sub-Bass Cutoff Selector (120 Hz, 140 Hz, 160 Hz).
  - Built-in Audio Test Player card with immediate play/stop and tone selection.
  - Attached Audio Sessions card showing active session IDs.

### Chunk 6: Build, Assemble & Final Verification
- [x] Verified unit tests via `./gradlew testDebugUnitTest` (all passed).
- [x] Assembled debug APK via `./gradlew assembleDebug` (clean exit code 0).
- [x] Verified generated APK: [app-debug.apk](file:///C:/Users/GAURI/Projects/EvenLouder/app/build/outputs/apk/debug/app-debug.apk).

---

## Next Action: On-Device Testing via USB Debugging
When you connect your physical Android 15 device with USB debugging enabled:
1. Verify device attached: `adb devices`
2. Install APK: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
3. Launch app: `adb shell am start -n com.evenlouder.app/.ui.MainActivity`
4. Tap **"PLAY TEST AUDIO ON SPEAKER"** and toggle **"DSP ENGINE ACTIVE"** vs **"HARDWARE BYPASS"** to hear the immediate acoustic difference!
