package com.evenlouder.app

import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.EqLayout
import com.evenlouder.app.core.dsp.ProfileCategory
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.core.session.AudioEngineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspEngineTest {

    @Test
    fun testAudioFlingerSessionSnifferRegexParsing() {
        val sampleDumpsysOutput = """
            AudioPlaybackConfiguration piid:3543 deviceIds:[3] type:android.media.AudioTrack u/pid:10228/26033 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0xA00 tags= bundle=null sessionId:6601 mutedState:none FormatInfo{isSpatialized=false, channelMask=0x3, sampleRate=48000}
            AudioPlaybackConfiguration piid:3551 deviceIds:[] type:android.media.AudioTrack u/pid:10228/26033 state:idle attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0xA00 tags= bundle=null sessionId:6609 mutedState:none
            new player piid:3455 uid/pid:10228/26033 package:com.maxrave.simpmusic type:android.media.AudioTrack attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0xA00 tags= bundle=null session:6513
        """.trimIndent()

        val discoveredSessions = mutableListOf<Pair<Int, Int>>()
        sampleDumpsysOutput.lines().forEach { line ->
            val match = AudioEngineService.DUMPSYS_CONFIG_REGEX.find(line)
            if (match != null && match.groupValues[2] == "started") {
                val uid = match.groupValues[1].toInt()
                val sessionId = match.groupValues[3].toInt()
                discoveredSessions.add(uid to sessionId)
            }
        }

        assertEquals(1, discoveredSessions.size)
        assertEquals(10228, discoveredSessions[0].first)
        assertEquals(6601, discoveredSessions[0].second)
    }

    @Test
    fun testBrowserAudioFlingerActiveTracksParsing() {
        val browserFlingerSample = """
            Global session refs:
              session  cnt     pid    uid  name
                  105    1    2275   1000  android.uid.system
                 7201    1   15599  10218  com.brave.browser
            Output thread 0x70bad82100, name AudioOut_2, tid 3373, type 0 (MIXER):
              Active Tracks:
                1 active track:
                  1, Prio: -1, Client: 10080, Session: 7721, State: ACTIVE, Output: 13
              Tracks:
                896    yes   15599/  10218    7201     940 A  0x000 00000005 00000003  48000
                123 yes 10080/26372 7721 0 0xb40000707758e800 0x70785c4000 00000003 48000
                124 10080 7721 ACTIVE 0x0000 0x00000003 DEFER 2 1920 0 0 0
        """.trimIndent()

        val foundGlobal = mutableListOf<Pair<Int, String>>()
        browserFlingerSample.lines().forEach { line ->
            val globalMatch = AudioEngineService.AUDIOFLINGER_GLOBAL_REF_REGEX.find(line)
            if (globalMatch != null) {
                val sess = globalMatch.groupValues[1].toInt()
                val pkg = globalMatch.groupValues[3]
                foundGlobal.add(sess to pkg)
            }
        }
        assertEquals(2, foundGlobal.size)
        assertEquals(7201, foundGlobal[1].first)
        assertEquals("com.brave.browser", foundGlobal[1].second)

        val foundTrack = mutableListOf<Pair<Int, Int>>()
        browserFlingerSample.lines().forEach { line ->
            val trackMatch = AudioEngineService.AUDIOFLINGER_TRACK_REGEX.find(line)
            if (trackMatch != null) {
                val uid = trackMatch.groupValues[1].toInt()
                val sess = trackMatch.groupValues[2].toInt()
                foundTrack.add(uid to sess)
            }
        }
        assertEquals(2, foundTrack.size)
        assertEquals(10218, foundTrack[0].first)
        assertEquals(7201, foundTrack[0].second)
        assertEquals(26372, foundTrack[1].first)
        assertEquals(7721, foundTrack[1].second)
    }

    @Test
    fun testPlayerEventOptionalPackageParsing() {
        val sampleWithoutPackage = "new player piid:1234 uid/pid:10080/26372 type:android.media.AudioTrack attr:AudioAttributes: usage=USAGE_MEDIA session:9012"
        val match = AudioEngineService.DUMPSYS_EVENT_REGEX.find(sampleWithoutPackage)
        org.junit.Assert.assertNotNull(match)
        assertEquals(10080, match!!.groupValues[1].toInt())
        assertEquals("", match.groupValues[2]) // Package group empty but captured
        assertEquals(9012, match.groupValues[3].toInt())
    }

    @Test
    fun testAndroid15AidlBandLimitCompliance() {
        val totalBands = EqLayout.PRE_EQ_BAND_COUNT
        assertTrue(
            "Total Pre-EQ bands ($totalBands) must strictly not exceed AIDL limit (32)",
            totalBands <= 32
        )
        assertEquals(23, totalBands)
    }

    @Test
    fun testAcousticSafetyProfiles() {
        for (profile in TuningProfile.values()) {
            val cfg = profile.config
            if (cfg.enabled) {
                val minHpf = if (profile.matchesCategory(ProfileCategory.PHONE_SPEAKER)) 120f else 75f
                assertTrue(
                    "${profile.name}: High pass cutoff (${cfg.highPassCutoffHz}) must be >= ${minHpf}Hz",
                    cfg.highPassCutoffHz >= minHpf
                )

                assertTrue(
                    "${profile.name}: Limiter ceiling (${cfg.limiterCeilingDb}) must be < 0.0 dBFS",
                    cfg.limiterCeilingDb < 0.0f
                )

                assertTrue(
                    "${profile.name}: Vocal clarity (${cfg.vocalClarityDb}) must be between -6 and 10 dB",
                    cfg.vocalClarityDb in -6.0f..10.0f
                )

                assertTrue(
                    "${profile.name}: Bass gain (${cfg.bassGainDb}) must be between -10 and 10 dB",
                    cfg.bassGainDb in -10.0f..10.0f
                )

                assertTrue(
                    "${profile.name}: Boxiness cut (${cfg.boxinessCutDb}) must be between -10 and 0 dB",
                    cfg.boxinessCutDb in -10.0f..0.0f
                )

                assertTrue(
                    "${profile.name}: Treble gain (${cfg.trebleGainDb}) must be between -6 and 10 dB",
                    cfg.trebleGainDb in -6.0f..10.0f
                )

                assertTrue(
                    "${profile.name}: Loudness makeup gain (${cfg.loudnessGainDb}) must be <= 6 dB",
                    cfg.loudnessGainDb in 0.0f..6.0f
                )
            }
        }
    }

    @Test
    fun testBassVariationsCalibration() {
        val bassProfiles = listOf(
            TuningProfile.DEEP_BASS,
            TuningProfile.MAXX_BASS,
            TuningProfile.DOLBY_DYNAMIC_BASS,
            TuningProfile.SUB_BASS_DEEP
        )

        for (profile in bassProfiles) {
            assertTrue(
                "${profile.name} must be tagged under BASS category",
                profile.matchesCategory(ProfileCategory.BASS)
            )
            assertTrue(
                "${profile.name} must have a valid acoustic profile with sub-bass filters",
                profile.acousticProfile != null && profile.acousticProfile.filters.isNotEmpty()
            )
        }
    }

    @Test
    fun testInstrumentalModesCalibration() {
        val instrumentalProfiles = listOf(
            TuningProfile.DIRAC_HIFI,
            TuningProfile.DIRAC_VIRTUO_ACOUSTIC,
            TuningProfile.DOLBY_SYMPHONY,
            TuningProfile.LIVE_INSTRUMENTAL
        )

        for (profile in instrumentalProfiles) {
            assertTrue(
                "${profile.name} must be tagged under INSTRUMENTAL category",
                profile.matchesCategory(ProfileCategory.INSTRUMENTAL)
            )
            assertTrue(
                "${profile.name} must be backed by a verified acoustic profile",
                profile.acousticProfile != null
            )
        }
    }

    @Test
    fun testProfileCategoriesIntegrity() {
        val allProfiles = TuningProfile.values()
        assertTrue("Total tuning profiles count must be at least 13 (current: ${allProfiles.size})", allProfiles.size >= 13)

        val dolbyDiracProfiles = allProfiles.filter { it.matchesCategory(ProfileCategory.DOLBY_DIRAC) }
        assertTrue("Dolby & Dirac category must have multiple profiles", dolbyDiracProfiles.size >= 5)

        // Verify existing profile backward compatibility
        val legacyProfiles = listOf("DOLBY_SPATIAL", "MAXX_BASS", "DIRAC_HIFI", "VOCAL_PRO", "ULTRA_BOOST", "BYPASS")
        for (legacy in legacyProfiles) {
            assertTrue("Legacy profile $legacy must be retained", allProfiles.any { it.name == legacy })
        }
    }
}
