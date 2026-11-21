package com.evenlouder.app.core.dsp

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.*
import android.util.Log

/**
 * Hardware-safe Android 15+ DynamicsProcessing AudioFX engine.
 * Calibrated specifically for micro-speakers (protecting against excursion & thermal damage).
 */
class DynamicsEngine(
    private val audioSessionId: Int,
    private val channelCount: Int = 2
) {
    companion object {
        private const val TAG = "DynamicsEngine"
    }

    private var effect: DynamicsProcessing? = null
    var currentConfig: DspConfig = DspConfig()
        private set

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val builder = Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channelCount,
                true, DspConfig.PRE_EQ_BANDS,
                true, DspConfig.MBC_BANDS,
                false, 0, // Post-EQ disabled to avoid uninitialized biquad filter NaNs
                true      // Peak limiter enabled
            )

            val baseConfig = builder.build()
            effect = DynamicsProcessing(0, audioSessionId, baseConfig)
            applyConfig(currentConfig)
            Log.i(TAG, "Initialized DynamicsEngine for session $audioSessionId on $channelCount channels")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DynamicsEngine for session $audioSessionId", e)
            effect = null
        }
    }

    fun applyConfig(config: DspConfig) {
        val fx = effect ?: return
        currentConfig = config

        try {
            if (!config.enabled) {
                // Neutralize DSP to 100% transparent unity gain across all channels and disable sub-stages
                for (ch in 0 until channelCount) {
                    val preEq = fx.getPreEqByChannelIndex(ch)
                    for (b in 0 until DspConfig.PRE_EQ_BANDS) {
                        preEq.getBand(b).apply {
                            gain = 0.0f
                            setEnabled(false)
                        }
                    }
                    preEq.setEnabled(false)
                    fx.setPreEqByChannelIndex(ch, preEq)

                    val mbc = fx.getMbcByChannelIndex(ch)
                    for (b in 0 until DspConfig.MBC_BANDS) {
                        mbc.getBand(b).apply {
                            preGain = 0.0f
                            postGain = 0.0f
                            setEnabled(false)
                        }
                    }
                    mbc.setEnabled(false)
                    fx.setMbcByChannelIndex(ch, mbc)

                    val limiter = fx.getLimiterByChannelIndex(ch)
                    limiter.apply {
                        postGain = 0.0f
                        setEnabled(false)
                    }
                    limiter.setEnabled(false)
                    fx.setLimiterByChannelIndex(ch, limiter)
                }
                fx.enabled = false
                return
            }

            fx.enabled = true
            for (ch in 0 until channelCount) {
                // 1. Pre-EQ: Sub-bass high-pass + 280Hz MaxxBass punch + 950Hz de-box + 2.8kHz vocal + 7.5kHz treble + 13.5kHz air
                val preEq = fx.getPreEqByChannelIndex(ch)
                preEq.setEnabled(true)

                // Band 0: Sub-bass excursion protection filter
                preEq.getBand(0).apply {
                    cutoffFrequency = config.highPassCutoffHz
                    gain = -18f
                    setEnabled(true)
                }
                // Band 1: Upper-bass harmonic punch / MaxxBass resonance (280 Hz)
                preEq.getBand(1).apply {
                    cutoffFrequency = 280f
                    gain = config.bassGainDb.coerceIn(0f, 6f)
                    setEnabled(true)
                }
                // Band 2: Anti-Boxiness / Chassis Resonance Dip (950 Hz)
                preEq.getBand(2).apply {
                    cutoffFrequency = 950f
                    gain = config.boxinessCutDb.coerceIn(-6f, 0f)
                    setEnabled(true)
                }
                // Band 3: Vocal & dialogue presence (2.8 kHz Fletcher-Munson peak)
                preEq.getBand(3).apply {
                    cutoffFrequency = 2800f
                    gain = config.vocalClarityDb.coerceIn(0f, 6f)
                    setEnabled(true)
                }
                // Band 4: Treble crispness & detail (7.5 kHz)
                preEq.getBand(4).apply {
                    cutoffFrequency = 7500f
                    gain = config.trebleGainDb.coerceIn(0f, 6f)
                    setEnabled(true)
                }
                // Band 5: Spatial air & sparkle (13.5 kHz)
                preEq.getBand(5).apply {
                    cutoffFrequency = 13500f
                    gain = (config.trebleGainDb * 0.7f).coerceIn(0f, 4f)
                    setEnabled(true)
                }
                fx.setPreEqByChannelIndex(ch, preEq)

                // 2. Multi-Band Compressor (Mastering-grade Soft-Knee architecture)
                val mbc = fx.getMbcByChannelIndex(ch)
                mbc.setEnabled(true)
                val makeup = config.loudnessGainDb.coerceIn(0f, 6f)

                // Low band (135Hz - 550Hz) - Tight punch control, soft-knee analog warmth
                mbc.getBand(0).apply {
                    cutoffFrequency = 550f
                    threshold = -16f
                    ratio = 3.5f
                    attackTime = 14f
                    releaseTime = 80f
                    kneeWidth = 5.0f // Smooth soft-knee analog warmth
                    noiseGateThreshold = -90.0f // Explicitly disabled
                    expanderRatio = 1.0f
                    preGain = 0.0f
                    postGain = (makeup * 0.85f)
                    setEnabled(true)
                }
                // Mid band (550Hz - 4200Hz) - Dialogue leveling, transparent optical release
                mbc.getBand(1).apply {
                    cutoffFrequency = 4200f
                    threshold = -13f
                    ratio = 2.2f
                    attackTime = 10f
                    releaseTime = 90f
                    kneeWidth = 6.0f // Mastering-grade soft knee eliminates pumping
                    noiseGateThreshold = -90.0f
                    expanderRatio = 1.0f
                    preGain = 0.0f
                    postGain = makeup
                    setEnabled(true)
                }
                // High band (4200Hz - 20000Hz) - High-end air sparkle, fast limiter response
                mbc.getBand(2).apply {
                    cutoffFrequency = 20000f
                    threshold = -12f
                    ratio = 2.0f
                    attackTime = 5f
                    releaseTime = 60f
                    kneeWidth = 4.0f
                    noiseGateThreshold = -90.0f
                    expanderRatio = 1.0f
                    preGain = 0.0f
                    postGain = (makeup * 0.75f)
                    setEnabled(true)
                }
                fx.setMbcByChannelIndex(ch, mbc)

                // 3. Peak Limiter: Strict -0.5 dBFS ceiling to prevent clipping & SmartPA ducking
                val limiter = fx.getLimiterByChannelIndex(ch)
                limiter.setEnabled(true)
                limiter.apply {
                    threshold = config.limiterCeilingDb.coerceIn(-6f, -0.1f)
                    attackTime = 1.0f
                    releaseTime = 50.0f
                    ratio = 12f
                    linkGroup = 0
                    postGain = 0.0f
                    setEnabled(true)
                }
                fx.setLimiterByChannelIndex(ch, limiter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DspConfig to session $audioSessionId", e)
        }
    }

    fun release() {
        try {
            applyConfig(currentConfig.copy(enabled = false))
            effect?.enabled = false
            effect?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing DynamicsEngine", e)
        } finally {
            effect = null
        }
    }
}
