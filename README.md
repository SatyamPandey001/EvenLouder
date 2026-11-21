# EvenLouder

An engineered, hardware-aware speaker tuning and loudness optimization app specifically targeting **Android 15+ (API 35+)**.

EvenLouder bypasses the flaws of typical "volume boosters" (which merely distort digital audio and trigger harsh hardware SmartPA limiters) by utilizing Android's AIDL-compliant `DynamicsProcessing` engine to optimize micro-speaker output safely and cleanly.

---

## Core Objectives

1. **Acoustic Correction:** Roll off non-reproducible sub-bass (<140 Hz) to free up amplifier power and voice coil excursion.
2. **Perceived Loudness:** Apply Fletcher-Munson curve compensation (1.8 kHz - 3.5 kHz presence boost) and psychoacoustic missing-fundamental harmonics.
3. **Dynamic Range Management:** 3-band Multi-Band Compression (MBC) to lift quiet details without clipping.
4. **Hardware Protection:** Hard brickwall limiter capped at -0.5 dBFS to prevent digital clipping and avoid triggering the phone's hardware SmartPA limiter/thermal throttling.
5. **Session Interception:** Dynamic audio session attachment via `MediaSessionManager` and `NotificationListenerService` without relying on deprecated Session 0.

---

## Technical Baseline

* **Target OS:** Android 15+ (API 35+)
* **Min SDK:** 35
* **Language:** Kotlin
* **UI:** Jetpack Compose (functional, meter-focused, zero bloat)
* **Audio Engine:** `android.media.audiofx.DynamicsProcessing` (AIDL compliant, $\le 32$ total bands)
* **Background Service:** Foreground Service with type `mediaPlayback`

---

## Quick Reference Docs

* [ARCHITECTURE.md](file:///C:/Users/GAURI/Projects/EvenLouder/ARCHITECTURE.md) - Audio pipeline specs, SmartPA constraints, AIDL limits, and algorithm parameters.
* [IMPLEMENTATION_PLAN.md](file:///C:/Users/GAURI/Projects/EvenLouder/IMPLEMENTATION_PLAN.md) - Step-by-step build roadmap, file tree, and session handoff state.
