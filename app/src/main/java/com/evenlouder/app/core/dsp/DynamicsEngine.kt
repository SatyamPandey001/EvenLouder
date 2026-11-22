package com.evenlouder.app.core.dsp

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.sqrt

data class EqPoint(val edgeHz: Float, val gainDb: Float)

/**
 * Single source of truth for the EQ. Engine and DspCurveVisualizer both read this table.
 * 3 stepped sub-bass roll-off bands + 20 fixed log-spaced bands whose gains come from smooth
 * bells (bass 300 Hz, de-box 950 Hz, presence 2.8 kHz) and a treble shelf (~5.5 kHz).
 */
object EqLayout {
    private val FIXED_EDGES = floatArrayOf(
        200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
        2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 13000f, 20000f
    )
    const val PRE_EQ_BAND_COUNT = 23   // 3 + FIXED_EDGES.size (20)
    const val MBC_BAND_COUNT = 3

    private fun bell(fc: Float, f0: Float, sigmaOct: Float, gain: Float): Float {
        val x = log2(fc / f0) / sigmaOct
        return gain * exp(-0.5f * x * x)
    }

    private fun shelf(fc: Float, f0: Float, gain: Float): Float =
        gain / (1f + exp(-2.2f * log2(fc / f0)))

    fun build(config: DspConfig): List<EqPoint> {
        check(FIXED_EDGES.size + 3 == PRE_EQ_BAND_COUNT)
        val hpf = config.highPassCutoffHz.coerceIn(80f, 180f)
        val on = config.eqEnabled
        val bass = if (on) config.bassGainDb.coerceIn(0f, 10f) else 0f
        val box = if (on) config.boxinessCutDb.coerceIn(-10f, 0f) else 0f
        val vocal = if (on) config.vocalClarityDb.coerceIn(-6f, 10f) else 0f
        val treble = if (on) config.trebleGainDb.coerceIn(-6f, 10f) else 0f

        val out = ArrayList<EqPoint>(PRE_EQ_BAND_COUNT)
        out += EqPoint(hpf * 0.5f, -24f)
        out += EqPoint(hpf * 0.75f, -14f)
        out += EqPoint(hpf, -6f)

        var low = hpf
        for (edge in FIXED_EDGES) {
            val fc = sqrt(low * edge)
            val g = bell(fc, 300f, 0.8f, bass) +
                bell(fc, 950f, 0.7f, box) +
                bell(fc, 2800f, 0.9f, vocal) +
                shelf(fc, 5500f, treble)
            out += EqPoint(edge, g.coerceIn(-15f, 12f))
            low = edge
        }
        return out
    }
}

/**
 * Chain: input gain (boost - trim) -> 23-band Pre-EQ -> 3-band MBC -> limiter.
 * Self-healing: if the effect lost control or throws, it is rebuilt once and re-applied.
 * applyConfig returns false only if the session cannot be recovered.
 */
class DynamicsEngine(
    private val audioSessionId: Int,
    private val channelCount: Int = 2
) {
    companion object {
        private const val TAG = "DynamicsEngine"
        private const val MBC_LOW_EDGE = 500f
        private const val MBC_MID_EDGE = 4000f
        private const val MBC_HIGH_EDGE = 20000f
    }

    private var effect: DynamicsProcessing? = null

    var currentConfig: DspConfig = DspConfig()
        private set

    val isReady: Boolean get() = effect != null

    init { create() }

    private fun create(): Boolean = try {
        val builder = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            channelCount,
            true, EqLayout.PRE_EQ_BAND_COUNT,
            true, EqLayout.MBC_BAND_COUNT,
            false, 0,
            true
        )
        effect = DynamicsProcessing(0, audioSessionId, builder.build())
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create effect for session $audioSessionId", e)
        effect = null
        false
    }

    private fun recreate(): Boolean {
        try { effect?.release() } catch (_: Exception) {}
        effect = null
        return create()
    }

    @Synchronized
    fun applyConfig(config: DspConfig): Boolean {
        currentConfig = config
        var fx = effect
        if (fx == null) {
            if (!create()) return false
            fx = effect ?: return false
        }
        if (!fx.hasControl()) {
            Log.w(TAG, "Lost control of session $audioSessionId, rebuilding")
            if (!recreate()) return false
            fx = effect ?: return false
        }
        return try {
            write(fx, config)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Apply failed on session $audioSessionId, rebuilding once", e)
            if (!recreate()) return false
            try { write(effect ?: return false, config); true }
            catch (e2: Exception) { Log.e(TAG, "Apply failed after rebuild", e2); false }
        }
    }

    private fun write(fx: DynamicsProcessing, c: DspConfig) {
        if (!c.enabled) {
            fx.setInputGainAllChannelsTo(0f)
            fx.enabled = false
            return
        }

        val boost = c.volumeBoostDb.coerceIn(0f, DspConfig.MAX_BOOST_DB)
        val eq = EqLayout.build(c)
        val peak = eq.maxOf { it.gainDb }.coerceAtLeast(0f)
        // Headroom trim only matters when EQ boosts; boost-only mode has none.
        val trim = if (c.eqEnabled) (peak * 0.5f).coerceAtMost(4f) else 0f
        fx.setInputGainAllChannelsTo(boost - trim)

        val makeup = if (c.eqEnabled) c.loudnessGainDb.coerceIn(0f, 6f) else 0f
        val extraRatio = boost * 0.15f   // firmer compression as boost rises

        // Pre-EQ
        val preEq = DynamicsProcessing.Eq(true, true, EqLayout.PRE_EQ_BAND_COUNT)
        eq.forEachIndexed { i, p -> preEq.setBand(i, DynamicsProcessing.EqBand(true, p.edgeHz, p.gainDb)) }
        fx.setPreEqAllChannelsTo(preEq)

        // MBC: (enabled, cutoff, attack, release, ratio, threshold, knee, gate, expander, preGain, postGain)
        val mbc = DynamicsProcessing.Mbc(true, true, EqLayout.MBC_BAND_COUNT)
        mbc.setBand(0, DynamicsProcessing.MbcBand(true, MBC_LOW_EDGE, 18f, 120f,
            2.0f + extraRatio, -22f, 8f, -90f, 1f, 0f, makeup * 0.8f))
        mbc.setBand(1, DynamicsProcessing.MbcBand(true, MBC_MID_EDGE, 12f, 130f,
            1.8f + extraRatio, -20f, 8f, -90f, 1f, 0f, makeup))
        mbc.setBand(2, DynamicsProcessing.MbcBand(true, MBC_HIGH_EDGE, 6f, 90f,
            2.0f + extraRatio, -20f, 6f, -90f, 1f, 0f, makeup * 0.7f))
        fx.setMbcAllChannelsTo(mbc)

        // Limiter: (inUse, enabled, linkGroup, attack, release, ratio, threshold, postGain)
        val limiter = DynamicsProcessing.Limiter(true, true, 0, 1f, 80f, 20f,
            c.limiterCeilingDb.coerceIn(-6f, -0.1f), 0f)
        fx.setLimiterAllChannelsTo(limiter)

        fx.enabled = true
    }

    @Synchronized
    fun release() {
        try {
            effect?.let {
                it.enabled = false
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing", e)
        } finally {
            effect = null
        }
    }
}
