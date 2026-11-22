# Digital Signal Processing Mathematics & Filter Implementation Reference

This document details the mathematical foundations of the EvenLouder Audio Tuning Engine, derived directly from Robert Bristow-Johnson's *Cookbook formulae for audio equalizer biquad filter coefficients* (W3C standard reference).

---

## 1. The Biquad Digital Filter Transfer Function

A digital biquad filter is a second-order recursive linear filter with two poles and two zeros. Its transfer function in the $Z$-domain is expressed as:

$$H(z) = \frac{Y(z)}{X(z)} = \frac{b_0 + b_1 z^{-1} + b_2 z^{-2}}{a_0 + a_1 z^{-1} + a_2 z^{-2}}$$

Normalized with respect to $a_0$ (setting $a_0 = 1$):

$$H(z) = \frac{\frac{b_0}{a_0} + \frac{b_1}{a_0} z^{-1} + \frac{b_2}{a_0} z^{-2}}{1 + \frac{a_1}{a_0} z^{-1} + \frac{a_2}{a_0} z^{-2}} = \frac{b_0' + b_1' z^{-1} + b_2' z^{-2}}{1 + a_1' z^{-1} + a_2' z^{-2}}$$

In time-domain Direct Form II (or Transposed Direct Form II for optimum numerical precision in 32-bit floating point), the difference equation is:

$$y[n] = b_0' x[n] + b_1' x[n-1] + b_2' x[n-2] - a_1' y[n-1] - a_2' y[n-2]$$

---

## 2. Intermediate Variables and Definitions

Given:
- $F_s$: Sampling frequency in Hertz (e.g., $44100\text{ Hz}$ or $48000\text{ Hz}$)
- $f_0$: Center frequency or cutoff frequency in Hertz ($0 < f_0 < F_s / 2$)
- $dBgain$: Gain in decibels ($dB$)
- $Q$: Quality factor ($Q > 0$, typically $0.1 \le Q \le 10$)
- $BW$: Bandwidth in octaves (related to $Q$)
- $S$: Shelf slope parameter (for shelving filters)

### Pre-warping and Angular Frequency:
$$\omega_0 = 2 \pi \frac{f_0}{F_s}$$
$$\sin(\omega_0) = \sin\left(2 \pi \frac{f_0}{F_s}\right)$$
$$\cos(\omega_0) = \cos\left(2 \pi \frac{f_0}{F_s}\right)$$

### Amplitude Factor:
$$A = 10^{\frac{dBgain}{40}} = \sqrt{10^{\frac{dBgain}{20}}}$$
Note that for peaking and shelving filters, $A^2 = 10^{\frac{dBgain}{20}}$, which represents the linear voltage gain.

### Alpha Calculation:
When specified by $Q$:
$$\alpha = \frac{\sin(\omega_0)}{2 Q}$$

When specified by Bandwidth $BW$ in octaves:
$$\alpha = \sin(\omega_0) \cdot \sinh\left( \frac{\ln(2)}{2} \cdot BW \cdot \frac{\omega_0}{\sin(\omega_0)} \right)$$

When specified by Shelf Slope $S$:
$$\alpha = \frac{\sin(\omega_0)}{2} \sqrt{\left(A + \frac{1}{A}\right) \left(\frac{1}{S} - 1\right) + 2}$$
For a maximally flat (Butterworth) shelf slope ($S = 1$):
$$\alpha = \frac{\sin(\omega_0)}{2} \sqrt{2} = \frac{\sqrt{2}}{2} \sin(\omega_0)$$

---

## 3. Filter Coefficient Formulations

### 3.1 Peaking EQ Filter (Parametric Bell)
Used for corrective equalization at a specific center frequency $f_0$:

$$b_0 = 1 + \alpha \cdot A$$
$$b_1 = -2 \cos(\omega_0)$$
$$b_2 = 1 - \alpha \cdot A$$
$$a_0 = 1 + \frac{\alpha}{A}$$
$$a_1 = -2 \cos(\omega_0)$$
$$a_2 = 1 - \frac{\alpha}{A}$$

### 3.2 Low-Shelf Filter
Boosts or cuts all frequencies below $f_0$ while preserving frequencies above $f_0$:

$$b_0 = A \left[ (A + 1) - (A - 1) \cos(\omega_0) + 2 \sqrt{A} \alpha \right]$$
$$b_1 = 2 A \left[ (A - 1) - (A + 1) \cos(\omega_0) \right]$$
$$b_2 = A \left[ (A + 1) - (A - 1) \cos(\omega_0) - 2 \sqrt{A} \alpha \right]$$
$$a_0 = (A + 1) + (A - 1) \cos(\omega_0) + 2 \sqrt{A} \alpha$$
$$a_1 = -2 \left[ (A - 1) + (A + 1) \cos(\omega_0) \right]$$
$$a_2 = (A + 1) + (A - 1) \cos(\omega_0) - 2 \sqrt{A} \alpha$$

### 3.3 High-Shelf Filter
Boosts or cuts all frequencies above $f_0$ while preserving frequencies below $f_0$:

$$b_0 = A \left[ (A + 1) + (A - 1) \cos(\omega_0) + 2 \sqrt{A} \alpha \right]$$
$$b_1 = -2 A \left[ (A - 1) + (A + 1) \cos(\omega_0) \right]$$
$$b_2 = A \left[ (A + 1) + (A - 1) \cos(\omega_0) - 2 \sqrt{A} \alpha \right]$$
$$a_0 = (A + 1) - (A - 1) \cos(\omega_0) + 2 \sqrt{A} \alpha$$
$$a_1 = 2 \left[ (A - 1) - (A + 1) \cos(\omega_0) \right]$$
$$a_2 = (A + 1) - (A - 1) \cos(\omega_0) - 2 \sqrt{A} \alpha$$

### 3.4 Low-Pass Filter (LPF)
Attenuates high frequencies above $f_0$ at $-12\text{ dB/octave}$:

$$b_0 = \frac{1 - \cos(\omega_0)}{2}$$
$$b_1 = 1 - \cos(\omega_0)$$
$$b_2 = \frac{1 - \cos(\omega_0)}{2}$$
$$a_0 = 1 + \alpha$$
$$a_1 = -2 \cos(\omega_0)$$
$$a_2 = 1 - \alpha$$

### 3.5 High-Pass Filter (HPF)
Attenuates low frequencies below $f_0$ at $-12\text{ dB/octave}$:

$$b_0 = \frac{1 + \cos(\omega_0)}{2}$$
$$b_1 = -(1 + \cos(\omega_0))$$
$$b_2 = \frac{1 + \cos(\omega_0)}{2}$$
$$a_0 = 1 + \alpha$$
$$a_1 = -2 \cos(\omega_0)$$
$$a_2 = 1 - \alpha$$

### 3.6 Notch Filter (Band-Stop)
Sharp attenuation at resonance frequency $f_0$:

$$b_0 = 1$$
$$b_1 = -2 \cos(\omega_0)$$
$$b_2 = 1$$
$$a_0 = 1 + \alpha$$
$$a_1 = -2 \cos(\omega_0)$$
$$a_2 = 1 - \alpha$$

### 3.7 Band-Pass Filter (Constant 0 dB Peak Gain)
$$b_0 = \alpha$$
$$b_1 = 0$$
$$b_2 = -\alpha$$
$$a_0 = 1 + \alpha$$
$$a_1 = -2 \cos(\omega_0)$$
$$a_2 = 1 - \alpha$$

---

## 4. Complex Frequency Response Calculation

To compute the magnitude and phase response of an individual biquad at any arbitrary frequency $f$, evaluate $H(z)$ on the unit circle $z = e^{j\omega}$ where $\omega = 2 \pi \frac{f}{F_s}$:

$$e^{-j\omega} = \cos(\omega) - j \sin(\omega)$$
$$e^{-j 2\omega} = \cos(2\omega) - j \sin(2\omega)$$

The numerator polynomial $N(e^{j\omega})$ is:
$$N_{\text{re}} = b_0 + b_1 \cos(\omega) + b_2 \cos(2\omega)$$
$$N_{\text{im}} = -b_1 \sin(\omega) - b_2 \sin(2\omega)$$

The denominator polynomial $D(e^{j\omega})$ is:
$$D_{\text{re}} = 1 + a_1 \cos(\omega) + a_2 \cos(2\omega)$$
$$D_{\text{im}} = -a_1 \sin(\omega) - a_2 \sin(2\omega)$$

The magnitude squared is:
$$|H(e^{j\omega})|^2 = \frac{N_{\text{re}}^2 + N_{\text{im}}^2}{D_{\text{re}}^2 + D_{\text{im}}^2}$$

The gain in decibels is:
$$\text{Gain}_{\text{dB}}(f) = 10 \log_{10}(|H(e^{j\omega})|^2) = 20 \log_{10}|H(e^{j\omega})|$$

The phase response in radians is:
$$\phi(f) = \text{atan2}(N_{\text{im}}, N_{\text{re}}) - \text{atan2}(D_{\text{im}}, D_{\text{re}})$$

---

## 5. Cascaded Biquad Chains and Total Response

In a parametric equalizer profile containing $M$ cascaded biquad filters:
$$H_{\text{total}}(z) = \prod_{k=1}^M H_k(z)$$

Because decibels are logarithmic:
$$\text{TotalGain}_{\text{dB}}(f) = \sum_{k=1}^M \text{Gain}_{k,\text{dB}}(f)$$

---

## 6. Stability Verification and Pole Constraints

A discrete-time linear time-invariant system is strictly bounded-input bounded-output (BIBO) stable if and only if all poles of its transfer function lie strictly inside the unit circle of the complex plane:

$$|p_i| < 1 \quad \forall i$$

For a normalized biquad denominator $1 + a_1 z^{-1} + a_2 z^{-2} = 0$, multiplying by $z^2$ yields the characteristic polynomial:
$$z^2 + a_1 z + a_2 = 0$$

The roots are:
$$p_{1,2} = \frac{-a_1 \pm \sqrt{a_1^2 - 4 a_2}}{2}$$

By Jury's stability test / Schur-Cohn criteria for second-order polynomials, the roots lie strictly inside the unit circle if and only if:
1. $|a_2| < 1$
2. $|a_1| < 1 + a_2$
3. All coefficients are real and finite (neither NaN nor $\pm\infty$).

Any filter violating these three conditions is unstable and rejected by the validation engine.

---

## 7. Headroom, Preamp Compensation, and Clipping Prevention

If the cascaded filter chain has a combined peak gain:
$$G_{\text{peak}} = \max_{20 \le f \le \frac{F_s}{2}} \text{TotalGain}_{\text{dB}}(f)$$

When $G_{\text{peak}} > 0\text{ dB}$, a full-scale digital input signal ($0\text{ dBFS}$) would exceed $0\text{ dBFS}$ at the output, inducing severe digital clipping / arithmetic overflow.

To prevent digital clipping deterministically:
$$\text{Preamp}_{\text{required}} = -G_{\text{peak}} - \text{margin}$$
where $\text{margin} \ge 0.2\text{ dB}$ accounts for inter-sample peaks.
