package com.evenlouder.app

import com.evenlouder.app.core.dsp.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BiquadDspTest {

    private val sampleRates = listOf(44100.0, 48000.0, 96000.0)

    @Test
    fun testPeakingFilterFlatAtZeroGain() {
        for (sr in sampleRates) {
            val coeffs = BiquadCalculator.calculate(
                type = FilterType.PEAKING,
                frequencyHz = 1000.0,
                gainDb = 0.0,
                qFactor = 1.414,
                sampleRateHz = sr
            )
            assertTrue("Coefficients must be finite", coeffs.isFinite)
            assertTrue("Filter must be stable", coeffs.isStable)

            // Test response across the audible spectrum
            for (f in listOf(20.0, 100.0, 1000.0, 5000.0, 15000.0)) {
                val db = coeffs.responseDbAt(f, sr)
                assertEquals("0 dB peaking filter must yield 0 dB response at $f Hz (Fs=$sr)", 0.0, db, 0.001)
            }
        }
    }

    @Test
    fun testPeakingFilterExactCenterFrequencyBoostAndCut() {
        for (sr in sampleRates) {
            val boostCoeffs = BiquadCalculator.calculate(
                type = FilterType.PEAKING,
                frequencyHz = 1000.0,
                gainDb = 6.0,
                qFactor = 2.0,
                sampleRateHz = sr
            )
            assertTrue(boostCoeffs.isStable)
            val centerBoostDb = boostCoeffs.responseDbAt(1000.0, sr)
            assertEquals("Center frequency boost must match configured 6 dB (Fs=$sr)", 6.0, centerBoostDb, 0.02)

            val cutCoeffs = BiquadCalculator.calculate(
                type = FilterType.PEAKING,
                frequencyHz = 2500.0,
                gainDb = -4.5,
                qFactor = 1.8,
                sampleRateHz = sr
            )
            assertTrue(cutCoeffs.isStable)
            val centerCutDb = cutCoeffs.responseDbAt(2500.0, sr)
            assertEquals("Center frequency cut must match configured -4.5 dB (Fs=$sr)", -4.5, centerCutDb, 0.02)
        }
    }

    @Test
    fun testLowShelfLimits() {
        val sr = 48000.0
        val shelfCoeffs = BiquadCalculator.calculate(
            type = FilterType.LOW_SHELF,
            frequencyHz = 120.0,
            gainDb = 5.0,
            qFactor = 0.71,
            sampleRateHz = sr
        )
        assertTrue(shelfCoeffs.isStable)

        // Sub-bass (f << f0) should approach +5.0 dB
        val subBassDb = shelfCoeffs.responseDbAt(25.0, sr)
        assertEquals("Sub-bass response must approach +5 dB shelf gain", 5.0, subBassDb, 0.2)

        // High frequency (f >> f0) should approach 0 dB
        val highDb = shelfCoeffs.responseDbAt(10000.0, sr)
        assertEquals("High frequency response must approach 0 dB", 0.0, highDb, 0.05)
    }

    @Test
    fun testHighShelfLimits() {
        val sr = 48000.0
        val shelfCoeffs = BiquadCalculator.calculate(
            type = FilterType.HIGH_SHELF,
            frequencyHz = 8000.0,
            gainDb = 4.0,
            qFactor = 0.71,
            sampleRateHz = sr
        )
        assertTrue(shelfCoeffs.isStable)

        // Low frequency (f << f0) should approach 0 dB
        val lowDb = shelfCoeffs.responseDbAt(100.0, sr)
        assertEquals("Low frequency response must approach 0 dB", 0.0, lowDb, 0.05)

        // High frequency near Nyquist should approach +4 dB
        val highDb = shelfCoeffs.responseDbAt(16000.0, sr)
        assertEquals("High frequency response must approach +4 dB shelf gain", 4.0, highDb, 0.2)
    }

    @Test
    fun testHighPassSubsonicRollOff() {
        val sr = 48000.0
        val hpf = BiquadCalculator.calculate(
            type = FilterType.HIGH_PASS,
            frequencyHz = 140.0,
            gainDb = 0.0,
            qFactor = 0.707, // Butterworth
            sampleRateHz = sr
        )
        assertTrue(hpf.isStable)

        // At cutoff f0, Butterworth HPF is -3 dB
        val cutoffDb = hpf.responseDbAt(140.0, sr)
        assertEquals("Butterworth HPF at cutoff frequency must be -3 dB", -3.01, cutoffDb, 0.1)

        // At 35 Hz (two octaves down), 2nd order HPF is approximately -24 dB
        val subDb = hpf.responseDbAt(35.0, sr)
        assertTrue("Subsonic frequency (35 Hz) must be strongly attenuated (<= -20 dB)", subDb <= -20.0)

        // Well above cutoff (e.g. 2000 Hz), attenuation should be ~0 dB
        val passDb = hpf.responseDbAt(2000.0, sr)
        assertEquals("Passband must be 0 dB", 0.0, passDb, 0.05)
    }

    @Test
    fun testNotchFilterRejection() {
        val sr = 48000.0
        val notch = BiquadCalculator.calculate(
            type = FilterType.NOTCH,
            frequencyHz = 1000.0,
            gainDb = 0.0,
            qFactor = 5.0,
            sampleRateHz = sr
        )
        assertTrue(notch.isStable)

        // Center frequency must have massive attenuation (> 40 dB cut)
        val notchCenterDb = notch.responseDbAt(1000.0, sr)
        assertTrue("Notch center must attenuate by at least 40 dB", notchCenterDb < -40.0)

        // Outside notch band, response is 0 dB
        assertEquals(0.0, notch.responseDbAt(200.0, sr), 0.1)
        assertEquals(0.0, notch.responseDbAt(5000.0, sr), 0.1)
    }

    @Test
    fun testFilterStabilityBoundaryConditions() {
        // Test high Q
        val highQ = BiquadCalculator.calculate(FilterType.PEAKING, 1000.0, 6.0, 15.0, 48000.0)
        assertTrue("High Q filter must remain stable", highQ.isStable)

        // Test extreme low frequency
        val lowFreq = BiquadCalculator.calculate(FilterType.PEAKING, 20.0, 4.0, 1.0, 48000.0)
        assertTrue("Low frequency filter must remain stable", lowFreq.isStable)

        // Test high frequency near Nyquist
        val nearNyquist = BiquadCalculator.calculate(FilterType.PEAKING, 23500.0, 3.0, 1.0, 48000.0)
        assertTrue("Near-Nyquist filter must remain stable", nearNyquist.isStable)
    }

    @Test
    fun testCascadedBiquadResponseAdditivity() {
        val sr = 48000.0
        val filter1 = BiquadCalculator.calculate(FilterType.PEAKING, 1000.0, 3.0, 1.4, sr)
        val filter2 = BiquadCalculator.calculate(FilterType.PEAKING, 1000.0, 4.0, 1.4, sr)

        val cascadeGain = BiquadCalculator.evaluateCascadeDb(listOf(filter1, filter2), 1000.0, sr)
        assertEquals("Cascaded identical filters must sum dB gains (+3 dB + +4 dB = +7 dB)", 7.0, cascadeGain, 0.05)
    }
}
