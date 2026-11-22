# DSP Testing & Verification Protocol

This document defines testing standards for digital signal processing, filter stability, profile validation, and profile differentiation.

---

## 1. Mathematical Unit Test Specifications

1. **Flat Response at 0 dB**:
   - For any filter type at $0\text{ dB}$ gain, the calculated magnitude response across all frequencies must equal $0.0\text{ dB} \pm 0.001\text{ dB}$.
2. **Peaking EQ Peak Test**:
   - For a peaking filter configured with $(f_0, G, Q)$, the evaluated gain at $f = f_0$ must equal $G\text{ dB} \pm 0.01\text{ dB}$.
   - The response at $f \ll f_0$ and $f \gg f_0$ must approach $0\text{ dB}$.
3. **Shelving Filter Limits**:
   - For a low-shelf with $(f_0, G)$, the response as $f \to 0$ must approach $G\text{ dB} \pm 0.05\text{ dB}$, and as $f \to \infty$ must approach $0\text{ dB}$.
   - For a high-shelf with $(f_0, G)$, the response as $f \to 0$ must approach $0\text{ dB}$, and as $f \to F_s/2$ must approach $G\text{ dB} \pm 0.05\text{ dB}$.
4. **Stability Criterion**:
   - Filter poles $p_{1,2}$ must strictly satisfy $|p_i| < 1.0$.
   - Any pole with magnitude $\ge 1.0$ indicates an oscillator or unstable exponential growth and must fail validation.
5. **Sample Rate Invariance**:
   - The filter mathematics must correctly scale angular frequencies across standard rates ($44.1\text{ kHz}, 48\text{ kHz}, 96\text{ kHz}$).
6. **Headroom & Clipping Prevention**:
   - Evaluated peak boost across $20\text{ Hz} \le f \le F_s/2$ must define minimum negative preamp gain to ensure combined throughput stays $\le 0\text{ dBFS}$.

---

## 2. Profile Differentiation Test (Acoustic Distance)

To prevent identical-sounding presets masquerading under different names:
- Given two profiles $A$ and $B$, evaluate their frequency response across a 100-point logarithmically spaced grid from $20\text{ Hz}$ to $20\text{ kHz}$.
- Calculate the root-mean-square (RMS) spectral difference:
  $$\text{RMS}_{\text{diff}} = \sqrt{\frac{1}{N} \sum_{i=1}^N \left( \text{Gain}_A(f_i) - \text{Gain}_B(f_i) \right)^2 }$$
- If $\text{RMS}_{\text{diff}} < 0.75\text{ dB}$ and $\max |\text{Gain}_A - \text{Gain}_B| < 1.5\text{ dB}$, the profiles are classified as duplicates / perceptually indistinct and flagged.
