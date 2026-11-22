# Acoustic Data & Profile Schema Specification

This document specifies the JSON and Kotlin data schemas used for parametric equalizer profiles, filters, and measurement records in EvenLouder.

---

## 1. Parametric Filter Schema

Each filter node represents a discrete second-order biquad section conforming to W3C Audio EQ Cookbook formulations.

```json
{
  "id": "band_1",
  "type": "PEAKING",
  "frequencyHz": 1000.0,
  "gainDb": -3.2,
  "qFactor": 1.414,
  "enabled": true,
  "slope": 1.0
}
```

### Supported Filter Types:
- `PEAKING`: Parametric bell filter (gain in dB, center frequency $f_0$, quality factor $Q$).
- `LOW_SHELF`: Shelving filter affecting frequencies below $f_0$.
- `HIGH_SHELF`: Shelving filter affecting frequencies above $f_0$.
- `LOW_PASS`: 2nd-order low-pass filter (-12 dB/oct).
- `HIGH_PASS`: 2nd-order high-pass filter (-12 dB/oct).
- `NOTCH`: Band-stop rejection filter at $f_0$.
- `BAND_PASS`: Resonant band-pass filter at $f_0$.

---

## 2. Acoustic Profile Schema

Each profile contains complete metadata, provenance, licensing, and filter configurations:

```json
{
  "id": "sennheiser_hd600_harman",
  "name": "Sennheiser HD 600 (Harman Over-Ear)",
  "description": "Measurement-based acoustic correction for Sennheiser HD 600 measured on GRAS 43AG by Oratory1990.",
  "category": "MEASUREMENT_CORRECTION",
  "targetDeviceType": "HEADPHONES",
  "manufacturer": "Sennheiser",
  "model": "HD 600",
  "preampGainDb": -5.8,
  "filters": [
    { "id": "f1", "type": "LOW_SHELF", "frequencyHz": 105.0, "gainDb": 5.5, "qFactor": 0.71, "enabled": true },
    { "id": "f2", "type": "PEAKING", "frequencyHz": 200.0, "gainDb": -1.8, "qFactor": 1.4, "enabled": true },
    { "id": "f3", "type": "PEAKING", "frequencyHz": 1250.0, "gainDb": -1.5, "qFactor": 2.0, "enabled": true },
    { "id": "f4", "type": "PEAKING", "frequencyHz": 3200.0, "gainDb": -2.7, "qFactor": 3.0, "enabled": true },
    { "id": "f5", "type": "PEAKING", "frequencyHz": 5700.0, "gainDb": -5.2, "qFactor": 4.5, "enabled": true },
    { "id": "f6", "type": "HIGH_SHELF", "frequencyHz": 10000.0, "gainDb": 2.0, "qFactor": 0.71, "enabled": true }
  ],
  "author": "Oratory1990",
  "sourceUrl": "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Sennheiser%20HD%20600",
  "license": "CC BY-SA 4.0",
  "targetCurve": "Harman Over-Ear 2018",
  "measurementRig": "GRAS 43AG / 45CA",
  "algorithmVersion": "AutoEQ v4.4.1",
  "validationStatus": "VALIDATED",
  "createdAt": "2026-11-22T00:00:00Z"
}
```

---

## 3. Categories Enumeration
- `MEASUREMENT_CORRECTION`: Transducer measurement-derived profile.
- `COMMUNITY_PROFILE`: Authored community profile (OPRA/Engineer).
- `PHONE_SPEAKER`: Micro-speaker acoustic tuning with subsonic protection.
- `CREATIVE_SIGNATURE`: Subjective sound coloration recipe.
