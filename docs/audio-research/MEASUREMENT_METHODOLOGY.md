# Acoustic Measurement Methodology & Target Curve Science

This document describes measurement protocols, artificial ear simulators, and compensation curves used for headphone and speaker tuning.

---

## 1. Acoustic Measurement Fixtures

1. **IEC 60318-4 (formerly IEC 711) Occluded Ear Simulator**:
   - Industry-standard acoustic coupler simulating the human ear canal impedance up to 10 kHz.
   - Contains half-inch microphone inside an acoustic impedance network.
   - Standard resonance appears around 13-14 kHz (coupler half-wave resonance artifact), which must not be equalized with sharp boost or cut.
2. **GRAS 43AG / 45CA Head and Torso Simulator (HATS)**:
   - Includes anatomically accurate pinnae (KB5000/KB5001 anthropometric pinna), ear canals, head, and torso.
   - Captures head-related transfer functions (HRTF) and anatomical pinna reflections.
3. **MiniDSP EARS**:
   - Simplified test jig for hobbyist / comparative measurements. Non-compliant with IEC 60318-4, so measurements require specific calibration files.

---

## 2. Standardized Target Curves

1. **Harman Target (Harman International / Dr. Sean Olive)**:
   - Derived from double-blind listening tests across trained and untrained listeners.
   - Over-Ear (2018): Features a ~4-5 dB shelf boost below 105 Hz and a natural ear-canal pinna gain around 3 kHz (+8 to +10 dB), rolling off smoothly past 10 kHz.
   - In-Ear (2019): Higher bass boost (~7-9 dB) due to lack of whole-body bone conduction in earphones.
2. **Diffuse Field (ISO 11904-1)**:
   - Response of a flat loudspeaker system in a reverberant diffuse room.
   - Tends to sound bright or thin to modern listeners because it lacks room boundary bass reinforcement.
3. **Free Field**:
   - Direct acoustic sound in an anechoic chamber from 0 degrees azimuth. Too bright for headphone playback due to lack of room reflections.

---

## 3. Smartphone Speaker Physical Acoustics

Unlike headphones, mobile micro-speakers:
- Suffer severe excursion limits ($X_{\text{max}} \approx 0.35\text{ mm}$).
- Frequencies below fundamental resonance $f_s$ ($150-180\text{ Hz}$) only create distortion and heat.
- Require high-pass subsonic filter to preserve transducer lifespan.
- Enclosure resonance around $900-1100\text{ Hz}$ sounds "tinny" or "boxy" and is attenuated by 3-5 dB.
