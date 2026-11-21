# EvenLouder - Architecture & Audio Engineering Spec

## 1. The Problem Space & Hardware Constraints

Smartphone micro-speakers have physical limits:
* **Nominal Power:** 0.5W to 3W.
* **Resonance Frequency ($f_0$):** Typically 600 Hz – 900 Hz. They physically cannot produce acoustic energy below ~200 Hz.
* **SmartPA Protection:** Modern SoCs use dedicated Smart Power Amplifiers (Cirrus Logic CS35L41, NXP TFA98xx, Qualcomm WCD/Aqstic) with integrated DSPs running real-time I/V sensing. 
* **The "Booster" Failure:** Cranked software digital gain (>0 dBFS) produces harmonic distortion (THD > 20%) and trips the SmartPA excursion limiter, resulting in aggressive dynamic ducking, muddiness, and premature thermal throttling.

---

## 2. Android 15 Platform Constraints

1. **Session 0 is Dead:** Global audio hook (`audioSessionId = 0`) is restricted/blocked on Android 14/15. System-wide processing requires tracking active player sessions.
2. **AIDL HAL Band Clamp:** Android 15 transitions audio effects to AIDL (`android.hardware.audio.effect`). Hardware-offloaded effects strictly clamp to a maximum of **32 bands**. Configurations exceeding 32 bands silently fail or distort.
3. **CTA-2075 Loudness Standard:** Android 15 introduces `LoudnessCodecController` (API 35). While useful for decoder-stage stream normalization, our app operates at the post-decode DSP effect layer to tune physical speaker output across all players.

---

## 3. Signal Flow Architecture

```
[Media App AudioTrack]
         │
         ▼ (Captured via Session Listener)
[DynamicsProcessing Engine]
  ├─ Stage 1: Input Gain (0.0 dB unity)
  ├─ Stage 2: Pre-EQ (High-Pass + Fletcher-Munson Boost)
  ├─ Stage 3: Multi-Band Compressor (Low, Mid, High)
  ├─ Stage 4: Post-EQ (Resonance Smoothing)
  └─ Stage 5: Brickwall Limiter (-0.5 dBFS Ceiling)
         │
         ▼
[AudioFlinger Mixer] ──► [AIDL Audio HAL] ──► [SmartPA + I/V Sense] ──► Speaker
```

---

## 4. Exact DSP Algorithm Calibration

### A. Pre-EQ Stage (6 Bands)
* **Band 0 (High-Pass):** Cutoff: 140 Hz, Gain: -18 dB. Drops sub-bass that wastes ~60% of amplifier power and causes unneeded cone excursion.
* **Band 1 (Lower-Mid Body):** Cutoff: 300 Hz, Gain: +1.5 dB, Q: 1.0. Adds perceived fullness above the hardware cutoff.
* **Band 2 (Mid Clear):** Cutoff: 1000 Hz, Gain: 0.0 dB (flat).
* **Band 3 (Presence / Vocal Boost):** Cutoff: 2800 Hz, Gain: +3.0 dB, Q: 1.2. Targets Fletcher-Munson high-sensitivity zone for clear dialog and perceived volume without increasing peak voltage.
* **Band 4 (Air / Detail):** Cutoff: 7000 Hz, Gain: +1.5 dB, Q: 1.0. Compensates for miniature speaker high-frequency dropoff.
* **Band 5 (Ultra-High Shelf):** Cutoff: 14000 Hz, Gain: 0.0 dB.

### B. Multi-Band Compressor Stage (3 Bands)
Splits the reproducible spectrum into three controlled compression regions:

| Band | Frequency Range | Threshold | Ratio | Attack | Release | Makeup Gain |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Low** | 140 Hz – 600 Hz | -16 dB | 3.5:1 | 12 ms | 80 ms | +2.0 dB |
| **Mid** | 600 Hz – 4500 Hz | -14 dB | 2.5:1 | 8 ms | 60 ms | +1.5 dB |
| **High** | 4500 Hz – 20 kHz | -12 dB | 2.0:1 | 5 ms | 50 ms | +1.0 dB |

*Result:* Quiet dialogue and instruments are brought forward; loud peaks are gently compressed before hitting the hardware amp.

### C. Peak Limiter Stage
* **Threshold:** -0.5 dBFS (prevents digital clipping at inter-sample boundaries).
* **Attack Time:** 1.0 ms (instant catch).
* **Release Time:** 50.0 ms.
* **Ratio:** 10:1 (hard wall).
* **Post Gain:** 0.0 dB.

---

## 5. Audio Session Interception Architecture

```
[Media Playback Apps (Spotify, YouTube, VLC)]
       │ (Broadcasts & Audio Focus)
       ▼
[AudioSessionTracker]
  ├─ MediaSessionManager (tracks active tokens)
  ├─ NotificationListenerService (intercepts media notifications)
  └─ BroadcastReceiver (ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
       │
       ▼
[AudioEngineService (Foreground Service: mediaPlayback)]
  └─ Maps AudioSessionID -> DynamicsProcessing instance
```

* **Fallback / Legacy:** If an app does not broadcast session intents and hides its session ID, a toggleable Shizuku / ADB permission (`android.permission.DUMP`) parses `dumpsys media.audio_flinger` to attach directly to untracked streams.
