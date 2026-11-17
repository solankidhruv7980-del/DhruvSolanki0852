package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ContentTypeMode(val wire: String, val label: String, val subtitle: String) {
    AI_ANIMATION("AI_ANIMATION", "AI Animation", "Manim, Remotion & AI scenes"),
    REAL_FOOTAGE("REAL_FOOTAGE", "Real Footage", "B-roll & camera clip remixes"),
    HYBRID("HYBRID", "Hybrid Mode", "Kinetic overlays on real clips");

    companion object {
        fun fromWire(value: String?): ContentTypeMode =
            entries.firstOrNull { it.wire == value } ?: HYBRID
    }
}

enum class FormatMode(val wire: String, val label: String) {
    DUAL("DUAL", "Dual Format (9:16 + 16:9)"),
    SHORTS_9_16("SHORTS_9_16", "Shorts Only (9:16)"),
    LONG_16_9("LONG_16_9", "Long-Form Only (16:9)");

    companion object {
        fun fromWire(value: String?): FormatMode =
            entries.firstOrNull { it.wire == value } ?: DUAL
    }
}

@IgnoreExtraProperties
data class StudioUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudioConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val isMasterAutopilotOn: Boolean = true,
    val dailyVideoFrequency: Int = 5,
    val contentType: String = ContentTypeMode.HYBRID.wire,
    val formatMode: String = FormatMode.DUAL.wire,
    val shortsRatioPercent: Int = 80,
    val primaryNiche: String = "Viral Shorts & Funk Edits / Multi-Niche",
    val activeFunkTrackId: String = "funk_neon_drift",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductionBatchItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val hookScript: String = "",
    val format: String = "9:16",
    val contentMode: String = ContentTypeMode.HYBRID.wire,
    val scheduledSlotLabel: String = "12:00 UTC",
    val funkTrackId: String = "funk_neon_drift",
    val funkTrackTitle: String = "Neon Drift Phonk (132 BPM)",
    val memeCues: List<String> = emptyList(),
    val animationCode: String = "",
    val seoDescription: String = "",
    val seoTags: List<String> = emptyList(),
    val yppScore: Int = 92,
    val estimatedRpmUsd: Double = 4.2,
    val status: String = "QUEUED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class YppAuditRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetTitle: String = "",
    val safetyScore: Int = 90,
    val estimatedRpmUsd: Double = 4.5,
    val reusedContentRisk: String = "LOW",
    val copyrightRisk: String = "LOW",
    val advertiserFriendly: Boolean = true,
    val summary: String = "",
    val recommendations: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelConnection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val channelHandle: String = "",
    val channelId: String = "",
    val oauthClientId: String = "",
    val grantedScopes: List<String> = listOf(
        "https://www.googleapis.com/auth/youtube.upload",
        "https://www.googleapis.com/auth/youtube.readonly",
        "https://www.googleapis.com/auth/youtube.force-ssl"
    ),
    val isLinked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class FunkTrack(
    val id: String,
    val title: String,
    val artistStyle: String,
    val bpm: Int,
    val rootFreqHz: Float,
    val bassPattern: List<Int>,
    val vibeTag: String,
    val yppSafeLabel: String = "YPP Cleared Synth"
)

data class MemeSfxPreset(
    val id: String,
    val name: String,
    val category: String,
    val timingTip: String,
    val toneType: String
)

data class SetupTutorialModule(
    val id: String,
    val number: Int,
    val title: String,
    val durationLabel: String,
    val summary: String,
    val steps: List<String>,
    val voiceScript: String
)

object StudioCatalog {
    val funkTracks = listOf(
        FunkTrack(
            id = "funk_neon_drift",
            title = "Neon Drift Phonk",
            artistStyle = "Cowbell + Sub-808",
            bpm = 132,
            rootFreqHz = 55f,
            bassPattern = listOf(0, 0, 3, 5, 0, 7, 5, 3),
            vibeTag = "High Retention"
        ),
        FunkTrack(
            id = "funk_rio_voltage",
            title = "Rio Voltage Funk",
            artistStyle = "Brazilian Bass Bounce",
            bpm = 138,
            rootFreqHz = 61.74f,
            bassPattern = listOf(0, 7, 0, 10, 0, 7, 5, 3),
            vibeTag = "Viral Shorts"
        ),
        FunkTrack(
            id = "funk_tokyo_hyper",
            title = "Tokyo Hyper Synth",
            artistStyle = "Cyber Slap Bass",
            bpm = 126,
            rootFreqHz = 65.41f,
            bassPattern = listOf(0, 2, 3, 7, 10, 7, 5, 2),
            vibeTag = "Tech & Motion"
        ),
        FunkTrack(
            id = "funk_midnight_sigma",
            title = "Midnight Turbo Edit",
            artistStyle = "Distorted Analog Funk",
            bpm = 142,
            rootFreqHz = 49f,
            bassPattern = listOf(0, 0, 7, 0, 8, 7, 5, 0),
            vibeTag = "Hook Drop"
        ),
        FunkTrack(
            id = "funk_clean_lo_fi_groove",
            title = "Studio Cashflow Groove",
            artistStyle = "Crisp Nu-Funk",
            bpm = 114,
            rootFreqHz = 73.42f,
            bassPattern = listOf(0, 4, 7, 9, 7, 4, 2, 0),
            vibeTag = "High RPM Safe"
        )
    )

    val memeSfxPresets = listOf(
        MemeSfxPreset("sfx_vine_boom", "Sub Impact Boom", "Impact", "Drop at 0.8s hook twist", "boom"),
        MemeSfxPreset("sfx_glitch_cut", "Digital Glitch Snap", "Transition", "Between caption bursts", "glitch"),
        MemeSfxPreset("sfx_whoosh", "Kinetic Whip Whoosh", "Motion", "Before metric reveal", "whoosh"),
        MemeSfxPreset("sfx_chime", "YPP Coin Ping", "Reward", "On payoff CTA frame", "chime"),
        MemeSfxPreset("sfx_record_stop", "Beat Freeze Cut", "Comedy", "Before punchline pause", "freeze")
    )

    val nicheOptions = listOf(
        "Viral Shorts & Funk Edits / Multi-Niche",
        "AI Automation & Code Visuals",
        "High-RPM Finance & Creator Economy",
        "Motivation & Kinetic Edits",
        "Gaming Highlights & Phonk Sync"
    )

    val setupModules = listOf(
        SetupTutorialModule(
            id = "mod_1",
            number = 1,
            title = "Connecting YouTube Data API, OAuth 2.0 & Channel Permissions",
            durationLabel = "01:45",
            summary = "Link your YouTube channel securely using OAuth 2.0 Client IDs and scoped permissions.",
            steps = listOf(
                "Open Google Cloud Console and enable YouTube Data API v3.",
                "Create an Android / Web OAuth 2.0 Client ID with your app package & SHA-1.",
                "Paste your Channel Handle, Channel ID, and Client ID in the OAuth Dashboard below.",
                "Verify required scopes: youtube.upload, youtube.readonly, youtube.force-ssl."
            ),
            voiceScript = "Module 1: Connecting YouTube Data API and OAuth 2.0. Enable YouTube Data API v3 in Google Cloud, configure your OAuth 2.0 client ID, verify upload and readonly scopes, and save your linked channel profile inside OmniTube Studio."
        ),
        SetupTutorialModule(
            id = "mod_2",
            number = 2,
            title = "Master ON/OFF Switch & Daily Frequency (1 to 10+ Videos/Day)",
            durationLabel = "02:10",
            summary = "Control daily upload velocity with algorithm-safe time spacing and Dual Format ratios.",
            steps = listOf(
                "Go to Tab 1 (Command) and inspect the Master Auto-Pilot kill switch.",
                "Drag the Daily Video Frequency slider from 1 up to 10+ videos per day.",
                "Select Content Mode: AI Animation, Real Footage, or Hybrid Mode.",
                "Tap Generate Daily Batch to auto-schedule non-spam peak time slots."
            ),
            voiceScript = "Module 2: Mastering the ON OFF kill switch and daily frequency. Use the frequency slider to choose anywhere from 1 to 10 plus videos per day. OmniTube spaces every upload across optimal peak windows to protect your algorithm reach."
        ),
        SetupTutorialModule(
            id = "mod_3",
            number = 3,
            title = "AI Animation Code Engine, Meme Library & Funk Music Swapper",
            durationLabel = "02:30",
            summary = "Generate Manim & Remotion code, render kinetic previews, and hot-swap YPP-safe funk tracks.",
            steps = listOf(
                "Open Tab 2 (Video Engine) and select 9:16 Shorts or 16:9 Widescreen.",
                "Generate viral hook scripts, Manim Python / Remotion code, and Runway/Kling prompts.",
                "Preview beats live on the Kinetic Canvas and toggle Meme SFX cues.",
                "Open the Funk Track Swapper to audition BPM grooves and swap tracks across queued batches."
            ),
            voiceScript = "Module 3: AI Animation Code, Memes, and the Funk Music Swapper. Generate executable Manim and Remotion animation code, preview kinetic captions in real time, and swap high-energy funk tracks across your entire queue with one tap."
        ),
        SetupTutorialModule(
            id = "mod_4",
            number = 4,
            title = "Gemini Trend Research, YPP Monetization Auditor & Analytics",
            durationLabel = "02:05",
            summary = "Scrape viral trends, audit videos before publishing, and maximize CTR and RPM.",
            steps = listOf(
                "Open Tab 3 (Monetization & Intel) to run Gemini viral trend scraping.",
                "Audit any script or picked device video for Reused Content & Copyright risk.",
                "Generate 5 high-CTR titles, SEO descriptions, tags, and pinned comment loops.",
                "Track retention curves and projected RPM velocity on the analytics telemetry deck."
            ),
            voiceScript = "Module 4: Gemini Trend Research and Monetization Auditing. Always run the YPP Compliance Auditor before publishing to verify originality, copyright safety, and high RPM keyword placement."
        )
    )
}
