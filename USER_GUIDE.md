# EvenLouder — User Guide & Manual

Welcome to **EvenLouder**, a hardware-aware audio tuning and loudness optimization engine for **Android 15+ (API 35+)**.

This guide covers setup, profile selection, manual acoustic tuning, and tips for getting the best sound from your smartphone micro-speakers.

---

## 📋 Table of Contents

1. [Requirements](#requirements)
2. [Installation](#installation)
3. [First Launch & Permissions](#first-launch--permissions)
4. [Understanding the Dashboard](#understanding-the-dashboard)
5. [Acoustic Profiles Explained](#acoustic-profiles-explained)
6. [Manual DSP Tuning Controls](#manual-dsp-tuning-controls)
7. [Reference Audio Test Player](#reference-audio-test-player)
8. [Connecting External Media Players](#connecting-external-media-players)
9. [Notification & Lock Screen Controls](#notification--lock-screen-controls)
10. [Troubleshooting & FAQ](#troubleshooting--faq)

---

## 1. Requirements

- **Device:** Smartphone or tablet running **Android 15 (API 35)** or higher.
- **Hardware:** Works with all device speakers (mono, stereo, or multi-driver speaker arrays).
- **Permissions:**
  - *Post Notifications* (required to run the background audio engine).
  - *Notification Listener Access* (optional, enables automatic session hooking for Spotify, YouTube, etc.).

---

## 2. Installation

1. Go to the [EvenLouder GitHub Releases](https://github.com/SatyamPandey001/EvenLouder/releases) page.
2. Download `app-release.apk`.
3. Open the downloaded file on your Android device and tap **Install**.
   *(If prompted, allow installation of unknown apps for your browser/file manager).*

---

## 3. First Launch & Permissions

Upon launching EvenLouder for the first time:

1. **Notification Access Prompt:**
   - Tap **Allow** when prompted for notification permissions.
   - EvenLouder runs a lightweight foreground service so its audio processing remains uninterrupted while switching between apps.
2. **Media Session Listener (Recommended):**
   - In the top bar or sticky dock, tap **Allow Session Access**.
   - Enable EvenLouder in your device's *Device & App Notifications* settings.
   - This allows EvenLouder to detect whenever YouTube, Spotify, or your media player begins playing and immediately hook into its audio stream.

---

## 4. Understanding the Dashboard

The EvenLouder user interface is organized into five key sections:

```
┌───────────────────────────────────────────────┐
│ [EvenLouder Header]            [Status Pill]  │
├───────────────────────────────────────────────┤
│ [Session Dock] Currently hooked player info   │
├───────────────────────────────────────────────┤
│ [Frequency Response Visualizer] Live Curve    │
├───────────────────────────────────────────────┤
│ [Profile Selector Tabs] All/Bass/Instrumental │
│  - Horizontal scrollable profile cards        │
├───────────────────────────────────────────────┤
│ [Acoustic Sliders] Precision tuning controls  │
├───────────────────────────────────────────────┤
│ [Built-in Reference Player] Play / Stop test  │
└───────────────────────────────────────────────┘
```

- **Frequency Curve Visualizer:** Shows the exact combined frequency response curve produced by your active Pre-EQ and Multi-Band Compressor settings.
- **Master Engine Toggle:** Located at the top right. Instantly engage or bypass all audio processing.

---

## 5. Acoustic Profiles Explained

EvenLouder provides 12 specialized acoustic profiles tailored for micro-speakers:

### Spatial & Hi-Fi
- **Cinema 3D Spatial:** Wide soundstage with dialogue enhancement. Excels at movies, TV series, Netflix, and YouTube videos.
- **Studio Hi-Fi Master:** Reference audiophile tuning. Flattens micro-speaker peaks to deliver transparent, neutral sound reproduction.
- **Dynamic Low-End Bass:** Extends bass response safely while keeping mids clear and free of mud.
- **Tight Transient Bass:** Fast attack curve for punchy drums and clear basslines.

### Bass Modes
- **Club Bass Punch:** Delivers powerful 280 Hz punch. Ideal for Pop, EDM, and Hip-Hop.
- **Deep Sub-Harmonics:** Aggressive low-end extension with multi-band compression for heavy beats.
- **Warm Studio Bass:** Smooth low-mid warmth reminiscent of analog studio monitors.

### Instrumental & Classical
- **Acoustic & Classical:** Accentuates string vibrations, piano harmonics, and delicate acoustic instruments.
- **Concert Symphony Hall:** Simulates an open concert hall with extended high-frequency air.
- **Live Stage & Rock:** Forward electric guitar edge and crisp cymbal sizzle.

### Speech & Loudness
- **Dialogue & Voice Max:** Maximum intelligibility for podcasts, audiobooks, audio messages, and phone calls.
- **Ultra Loudroom:** Maximum possible output volume without digital clipping or hardware throttling.

### Comparison
- **Hardware Bypass:** Turns off all DSP processing so you can immediately compare your tuned sound with the phone's stock sound.

---

## 6. Manual DSP Tuning Controls

Expand the **Acoustic Tuning Sliders** to manually fine-tune your sound:

1. **Sub-Bass Cutoff (HPF):** *Range: 100 Hz – 200 Hz*
   - Rolls off frequencies below this threshold. Set to ~135–140 Hz for most phones to eliminate distortion and free up amplifier power.
2. **Harmonic Bass Punch:** *Range: 0.0 dB – 6.0 dB*
   - Boosts upper-bass harmonics (280 Hz) to create full-bodied bass.
3. **Chassis Resonance Cut:** *Range: -6.0 dB – 0.0 dB*
   - Eliminates the hollow, "tinny" nasal tone (~950 Hz) caused by phone enclosure vibrations.
4. **Vocal Clarity (Fletcher-Munson):** *Range: 0.0 dB – 6.0 dB*
   - Boosts the human ear's high-sensitivity region (~2.8 kHz) for clear dialogue.
5. **Treble Sparkle & Air:** *Range: 0.0 dB – 6.0 dB*
   - Adds crisp high-frequency detail (7.5 kHz).
6. **Loudness Multi-Band Compressor:** *Range: 0.0 dB – 6.0 dB*
   - Smooth makeup gain that raises quiet sections without pushing loud peaks past the limit.

---

## 7. Reference Audio Test Player

To preview and evaluate tuning profiles without leaving the app:

1. Locate the **Reference Acoustic Player** card at the bottom of the screen.
2. Tap **Play Test Track**.
3. A high-resolution acoustic guitar sample will loop cleanly.
4. Switch between different presets or adjust the sliders to hear the acoustic changes instantly.

---

## 8. Connecting External Media Players

EvenLouder connects to external audio playback automatically through two mechanisms:

1. **Broadcast Session Interception:**
   - Standard players (e.g. Spotify, Poweramp, VLC, Musicolet) broadcast session intents. EvenLouder immediately attaches a `DynamicsProcessing` instance to their audio stream.
2. **Active Playback State Monitoring:**
   - Android's `AudioPlaybackCallback` continuously checks which app is playing audio (e.g., YouTube or Google Chrome) and attaches the DSP engine.

The **Active Audio Session Dock** at the top of the dashboard displays the active app name and session ID.

---

## 9. Notification & Lock Screen Controls

When EvenLouder is active:
- A persistent notification is displayed in your notification tray.
- **Quick Controls:**
  - **Power Button:** Turn DSP engine on or off with a single tap.
  - **Profile Switcher:** Cycle through your favorite tuning presets without opening the app.
- Expand the notification card for detailed information on the hooked media app.

---

## 10. Troubleshooting & FAQ

#### Why doesn't the sound get louder when I raise the system volume past maximum?
EvenLouder operates within digital limits ($\le 0\text{ dBFS}$) and hardware constraints. It achieves perceived loudness through frequency shaping and dynamic range compression. Unlike dangerous "hack" boosters, it does not damage your speakers or trigger hardware SmartPA thermal limiters.

#### The app doesn't seem to affect YouTube / Chrome:
Make sure **Notification Listener Access** is enabled in EvenLouder's settings. Some browser apps do not broadcast standard audio sessions, so the listener permission is needed to detect their active media playback.

#### Does EvenLouder consume excess battery?
No. EvenLouder uses Android's native hardware-accelerated `DynamicsProcessing` audio effects HAL. It does not process raw PCM audio in user space, resulting in near-zero CPU and battery usage.

#### Does EvenLouder collect any data?
No. EvenLouder does not have `android.permission.INTERNET` in its manifest. It has zero network access, zero analytics, and zero tracking.
