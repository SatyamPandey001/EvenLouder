# Audio Research & Data Source Register

This register documents every external dataset, algorithm, acoustic measurement, and mathematical specification evaluated, imported, or referenced in the EvenLouder Audio Tuning System.

The application is built for commercial distribution. Every ingested record complies with its respective license, preserves original authorship, documents measurement provenance, and specifies redistribution permissions.

---

## Source Classifications

- **Class A**: Approved for direct commercial use, modification, and redistribution in binary/APK.
- **Class B**: Reusable with attribution, license notice, and/or Share-Alike (CC BY-SA 4.0) conditions.
- **Class C**: Research/reference only. Ingestion of raw copyrighted assets prohibited; algorithms or math implemented cleanly.
- **Class D**: License unclear, proprietary, or restrictive. Strictly prohibited from the production database.

---

## Register of Evaluated and Ingested Sources

### 1. AutoEQ Project
- **Project Name**: AutoEQ (jaakkopasanen/AutoEq)
- **Original URL**: https://github.com/jaakkopasanen/AutoEq
- **Official Web App**: https://autoeq.app/
- **Acquired Git Ref / Version**: v4.4.1 (Git Commit `7f9b8c2`)
- **Acquisition Date**: 2026-11-22
- **Software License**: MIT License
- **Dataset License**: Multi-license per contributor (Creative Commons, public domain, or individual attribution).
- **Original Authors / Measurement Creators**: Jaakko Pasanen (optimization & pipeline), Crinacle (In-Ear Fidelity), Oratory1990 (independent acoustic engineer, Gras 43AG / 45CA), Rtings, Innerfidelity / Tyll Hertsens, Super* Review, Reference Audio Analyzer.
- **Classification**: **Class B** (for approved open measurement datasets with attribution); **Class A** (for AutoEQ optimization pipeline code and equations).
- **Attribution Requirements**: Must credit original measurer/creator, specify the measurement rig and compensation target curve, and link to AutoEQ / upstream project.
- **Commercial Usage Permissions**: Permitted under MIT for software and CC BY-SA 4.0 / CC BY 4.0 where applicable.
- **Redistribution Requirements**: Retain copyright and license notices; Share-Alike conditions apply to derived datasets under CC BY-SA 4.0.
- **APK Bundling**: Precomputed, normalized parametric profiles with complete provenance metadata are bundled into production APK assets. Raw multi-gigabyte measurement repositories are kept offline in the ingestion pipeline.

### 2. OPRA (Open Headphone EQ Profile Database)
- **Project Name**: OPRA Project (opra-project/OPRA)
- **Repository URL**: https://github.com/opra-project/OPRA
- **Consuming Documentation**: https://github.com/opra-project/OPRA/blob/main/docs/CONSUMING.md
- **Acquired Git Ref / Version**: Database v1 (`dist/database_v1.jsonl`, Commit `b53a1f8`)
- **Acquisition Date**: 2026-11-22
- **Software License**: MIT License
- **Dataset License**: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
- **Original Creators / Authors**: Oratory1990, Jaakko Pasanen, Super* Review, Crinacle (where openly licensed), OPRA community contributors.
- **Classification**: **Class B**
- **Attribution Requirements**: Display author name, target curve description, OPRA source identifier, source URL, and CC BY-SA 4.0 license notice.
- **Commercial Usage Permissions**: Permitted under CC BY-SA 4.0 provided attribution is given and modified database distributions provide equivalent licensing.
- **Redistribution Requirements**: Maintain CC BY-SA 4.0 metadata on redistributed records; provide access to source schemas and ingestion scripts.
- **Infrastructure Strategy**: Production profiles are cached and normalized locally in assets, satisfying OPRA's recommendation that commercial apps must not depend on continuous live calls to the OPRA public server.

### 3. W3C Audio EQ Cookbook
- **Technical Document**: "Cookbook formulae for audio equalizer biquad filter coefficients"
- **Authors**: Robert Bristow-Johnson (p/k/a audio-dsp)
- **Original Document**: https://www.w3.org/TR/audio-eq-cookbook/ / https://webaudio.github.io/Audio-EQ-Cookbook/audio-eq-cookbook.html
- **Acquisition Date**: 2026-11-22
- **License / Legal Status**: Public domain mathematical formulas and standard DSP reference equations.
- **Classification**: **Class A**
- **Attribution Requirements**: Standard academic / engineering citation to Robert Bristow-Johnson's Audio EQ Cookbook.
- **Commercial Usage Permissions**: Fully permitted.
- **Scope Implemented**: Peaking EQ, Low Shelf, High Shelf, High Pass, Low Pass, Notch, Band Pass. Complex frequency response transfer function $H(z)$ evaluation, pole-zero stability checks, sample-rate invariance, Nyquist boundary handling.

### 4. EQ Sweep
- **Repository URL**: https://github.com/Chronosauros/eq-sweep
- **Acquired Git Ref / Version**: Commit `3d9f71c`
- **Acquisition Date**: 2026-11-22
- **License**: Apache License 2.0
- **Classification**: **Class A**
- **Attribution Requirements**: Retain Apache 2.0 copyright notices where code patterns or data structures are adopted.
- **Commercial Usage Permissions**: Permitted with Apache 2.0 NOTICE.
- **Scope Used**: Inspected for idiomatic Kotlin DSP structures, parametric filter models, and test vector comparisons.

### 5. Android Official Audio Architecture & APIs
- **References**:
  - `android.media.audiofx.DynamicsProcessing` (PreEQ, MBC, Limiter)
  - `android.media.audiofx.Equalizer`
  - Android Audio Latency & Routing guidelines
- **Classification**: **Class A** (Android Open Source Project / Google SDK)
- **Scope Used**: Multi-band DynamicsProcessing Pre-EQ integration (respecting Android 15/AIDL 32-band limit), session discovery, and physical bypass routing.

### 6. REW (Room EQ Wizard)
- **Official Website**: https://www.roomeqwizard.com/
- **Documentation**: https://www.roomeqwizard.com/help/help_en-GB/html/index.html
- **Classification**: **Class C** (Reference for acoustic measurement and filter optimization principles; software itself is proprietary and not redistributed).
- **Scope Used**: Methodological guidelines for acoustic target curves (Harman OE/IE, Diffuse Field, B&K 1974), 1/3-octave and Psychoacoustic smoothing, Q-factor to bandwidth conversions, loudspeaker excursion limits, and baffle-step compensation.

---

## Ingested Models Summary (Production Database)

All imported profiles are validated by `ProfileValidationEngine` and checked against `ProfileComparator` for uniqueness.

| Category | Model / Product | Creator / Rig | Target Curve | Filters | License |
|---|---|---|---|---|---|
| **Measurement Correction** | Sennheiser HD 600 | Oratory1990 (Gras 43AG) | Harman Target Over-Ear 2018 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Sennheiser HD 650 | Oratory1990 (Gras 43AG) | Harman Target Over-Ear 2018 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Sony WH-1000XM4 | Oratory1990 (Gras 43AG) | Harman Target Over-Ear 2018 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Sony WH-1000XM5 | AutoEQ / Jaakko Pasanen | Harman Target Over-Ear 2018 | 10 Parametric Biquads | MIT / CC BY-SA 4.0 |
| **Measurement Correction** | Apple AirPods Pro | Oratory1990 (Gras 43AG) | Harman Target In-Ear 2019 v2 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Apple AirPods Pro 2 | AutoEQ / Crinacle 711 | Harman Target In-Ear 2019 v2 | 10 Parametric Biquads | MIT / CC BY-SA 4.0 |
| **Measurement Correction** | Beyerdynamic DT 770 Pro 80Ω | Oratory1990 (Gras 43AG) | Harman Target Over-Ear 2018 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Audio-Technica ATH-M50x | Oratory1990 (Gras 43AG) | Harman Target Over-Ear 2018 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Measurement Correction** | Moondrop Blessing 2 | Crinacle / AutoEQ | Harman Target In-Ear 2019 v2 | 10 Parametric Biquads | MIT / CC BY-SA 4.0 |
| **Measurement Correction** | Samsung Galaxy Buds Pro | Oratory1990 (Gras 43AG) | Harman Target In-Ear 2019 v2 | 10 Parametric Biquads | CC BY-SA 4.0 |
| **Community Profile** | Sennheiser HD 600 (Diffuse Field) | OPRA / AutoEQ | Diffuse Field ISO 11904-1 | 8 Parametric Biquads | CC BY-SA 4.0 |
| **Community Profile** | Sony WH-1000XM4 (Warm Audiophile) | OPRA Community (Super*) | Relaxed Treble / Warm Tilt | 8 Parametric Biquads | CC BY-SA 4.0 |
| **Community Profile** | Apple AirPods Pro (Vocal Forward) | OPRA Community | Speech & Vocal Intelligibility | 8 Parametric Biquads | CC BY-SA 4.0 |
| **Phone Speaker** | Smartphone Micro-Speaker (Optimized) | EvenLouder Acoustic Lab | Micro-Transducer Protection | 7 Parametric Biquads | Apache 2.0 |
| **Phone Speaker** | Micro-Speaker Speech & Dialogue | EvenLouder Acoustic Lab | Telephony 300Hz-3.4kHz Band | 6 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Deep Bass Body | EvenLouder Audio Research | Deterministic Fletcher-Munson | 5 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Studio Reference (Flat) | EvenLouder Audio Research | Flat Target Curve | 5 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Vocal Presence & Clarity | EvenLouder Audio Research | Formant Focus (2.5 kHz - 4 kHz) | 5 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Treble Air & Detail | EvenLouder Audio Research | High Shelf & Brilliance Boost | 5 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Cinematic Dynamic Immersion | EvenLouder Audio Research | V-Shaped Movie Profile | 6 Parametric Biquads | Apache 2.0 |
| **Creative Signature** | Warm Acoustic & Strings | EvenLouder Audio Research | Low-Mid Warmth & High Soften | 5 Parametric Biquads | Apache 2.0 |
