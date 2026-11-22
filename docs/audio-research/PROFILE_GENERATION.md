# Profile Generation, Measurement Ingestion, and Acoustic Methodology

This document outlines the architecture for generating, verifying, and applying audio profiles in EvenLouder, replacing non-deterministic AI guesses with empirical acoustic science.

---

## 1. The Three Explicit Profile Categories

### Category A: Measurement-Based Acoustic Correction
- **Objective**: Correct physical frequency response deviations of specific headphone and earphone transducers toward a standardized acoustic target.
- **Data Source**: Calibrated coupler measurements (GRAS 43AG / 45CA, IEC 60318-4) processed via AutoEQ.
- **Target Curves**:
  - Harman Over-Ear (2018): Target with preferred bass elevation and smoothed pinna gain for circumaural and supra-aural headphones.
  - Harman In-Ear (2019 v2): Target calibrated for ear-canal acoustic impedance with ear tips.
  - Diffuse Field (ISO 11904-1): Equalized for omnidirectional spatial room balance.
- **Error Calculation**:
  $$\text{Deviation}(f) = \text{Target}(f) - \text{RawMeasured}(f)$$
  The correction equalizer approximates $\text{Deviation}(f)$ using $N$ optimized parametric biquad filters ($5 \le N \le 10$).
- **Rule**: If no verified measurement exists for a model, the application displays "No verified measurement available" rather than inventing numbers.

### Category B: Verified Community / Engineer Profiles
- **Objective**: Provide authored, peer-reviewed equalization profiles from acoustic engineers (such as Oratory1990) and the OPRA repository.
- **Attributes**: Preserves original author attribution, measurement rig notes, target curve description, filter configuration, and CC BY-SA 4.0 license metadata.
- **Rule**: Never rename or rebrand an engineer's profile as an in-house proprietary tuning.

### Category C: Creative Sound Signatures
- **Objective**: Subjective sound coloration (Bass Boost, Vocal Focus, Treble Air, Warm Tone, Cinematic, Podcast) applied deterministically.
- **Rule**: Must be clearly labeled as subjective creative presets, NOT scientific calibrations. Never claim that a creative preset corrects transducer hardware.

---

## 2. Headphone Correction Pipeline

```
[Raw Measurement dB(f)] ---> [Target Curve dB(f)]
              |                       |
              v                       v
      [Subtract: Deviation(f) = Target(f) - Measurement(f)]
                              |
                              v
      [Smoothing: 1/3-octave / Psychoacoustic interpolation]
                              |
                              v
      [Parametric Biquad Optimization (AutoEQ algorithm)]
          - Low Shelf for Sub-Bass
          - Peaking Biquads for Ear-Canal / Concha resonances
          - High Shelf for Air / Treble
                              |
                              v
      [Evaluate Combined Response H_total(f)]
                              |
                              v
      [Calculate Headroom Peak & Preamp Compensation]
                              |
                              v
      [Profile Validation Engine (Stability, Bounds, Attribution)]
                              |
                              v
      [Acoustic Profile Catalog (JSON)]
```

---

## 3. Mobile Phone Micro-Speaker Acoustic Considerations

Mobile phone internal speakers have strict physical constraints that make headphone equalization curves dangerous if applied directly:
1. **Displacement Limits ($X_{\text{max}}$)**: A 15x11 mm micro-speaker cannot reproduce frequencies below 150-200 Hz without bottoming out and mechanical distortion.
2. **Subsonic Protection**: A sharp high-pass filter ($120\text{ Hz} - 160\text{ Hz}$) is mandatory to prevent thermal runaway and voice coil excursion damage.
3. **Boxiness and Chassis Resonance**: Phone bodies resonate around $800\text{ Hz} - 1.2\text{ kHz}$. A calibrated dip here significantly cleans up speech intelligibility.
4. **Presence Elevation**: Gentle boost between $2.5\text{ kHz} - 4.5\text{ kHz}$ enhances clarity for speech and podcast dialogue without exceeding thermal limits.
5. **Multiband Limiting**: Hardware-safe limiter ceiling prevents digital and DAC clipping when volume boosting is applied.
