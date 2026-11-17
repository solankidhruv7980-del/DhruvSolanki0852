package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ContentTypeMode
import com.example.data.model.FunkTrack
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioCatalog
import com.example.data.model.YppAuditRecord
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "FirebaseAiStudioRepo"
private const val AI_TIMEOUT_MS = 60_000L

data class AnimationPackageResult(
    val hookScript: String,
    val manimCode: String,
    val remotionCode: String,
    val videoApiPayload: String,
    val memeCues: List<String>,
    val seoTitle: String,
    val seoTags: List<String>
)

data class SeoEngagementResult(
    val titles: List<String>,
    val description: String,
    val tags: List<String>,
    val pinnedComment: String,
    val replyTemplates: List<String>
)

@OptIn(PublicPreviewAPI::class)
class FirebaseAiStudioRepository {

    private val batchSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("High-CTR YouTube title under 75 chars"),
                        "hookScript" to Schema.string("3-sentence viral hook and retention payoff script"),
                        "format" to Schema.enumeration(listOf("9:16", "16:9"), "Video aspect format"),
                        "memeCues" to Schema.array(Schema.string("Timestamped meme/SFX cue")),
                        "animationCode" to Schema.string("Short Manim Python or Remotion TSX snippet"),
                        "seoDescription" to Schema.string("SEO YouTube description"),
                        "seoTags" to Schema.array(Schema.string("SEO tag")),
                        "yppScore" to Schema.integer("YPP safety score 0-100"),
                        "estimatedRpmUsd" to Schema.double("Estimated RPM in USD")
                    )
                )
            )
        )
    )

    private val auditSchema = Schema.obj(
        properties = mapOf(
            "safetyScore" to Schema.integer("Overall YPP compliance score 0 to 100"),
            "estimatedRpmUsd" to Schema.double("Projected RPM in USD"),
            "reusedContentRisk" to Schema.enumeration(listOf("LOW", "MEDIUM", "HIGH")),
            "copyrightRisk" to Schema.enumeration(listOf("LOW", "MEDIUM", "HIGH")),
            "advertiserFriendly" to Schema.boolean(),
            "summary" to Schema.string("Detailed YPP audit summary"),
            "recommendations" to Schema.array(Schema.string("Concrete action item"))
        )
    )

    private val animationSchema = Schema.obj(
        properties = mapOf(
            "hookScript" to Schema.string("Opening 15-second retention script with visual beat markers"),
            "manimCode" to Schema.string("Complete runnable Python Manim Scene class"),
            "remotionCode" to Schema.string("Complete React/TypeScript Remotion composition"),
            "videoApiPayload" to Schema.string("JSON prompt payload for Runway / Kling / Sora / Pika"),
            "memeCues" to Schema.array(Schema.string("Timestamped meme & SFX placement")),
            "seoTitle" to Schema.string("High-CTR title"),
            "seoTags" to Schema.array(Schema.string("SEO keyword tag"))
        )
    )

    private val seoSchema = Schema.obj(
        properties = mapOf(
            "titles" to Schema.array(Schema.string("5 high-CTR title options")),
            "description" to Schema.string("SEO-optimized YouTube description"),
            "tags" to Schema.array(Schema.string("High-volume SEO tags")),
            "pinnedComment" to Schema.string("Engagement-boosting pinned comment"),
            "replyTemplates" to Schema.array(Schema.string("Creator reply templates"))
        )
    )

    private val batchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = batchSchema
            },
            systemInstruction = content {
                text(
                    "You are OmniTube Master AI Studio. Generate high-retention, YPP-compliant YouTube Shorts and Long-Form production batches with original commentary, kinetic animation hooks, and safe audio cues."
                )
            }
        )
    }

    private val fastHookModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = content {
                text("You are a concise, high-energy YouTube Shorts hook strategist. Respond quickly with crisp, actionable ideas.")
            }
        )
    }

    private val deepAuditModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = auditSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = content {
                text("You are a strict YouTube Partner Program (YPP) compliance auditor and RPM strategist. Never inflate scores; flag reused content or copyright hazards honestly.")
            }
        )
    }

    private val animationCodeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = animationSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    private val seoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = seoSchema
            }
        )
    }

    private val groundedTrendModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch(), Tool.googleMaps()),
            systemInstruction = content {
                text(
                    "You are OmniTube Trend & Niche Intelligence. Use live Google Search and Maps context when helpful to surface viral YouTube trends, high-RPM sub-niches, creator filming spots, and format hooks."
                )
            }
        )
    }

    private val multimodalModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val videoAnalyzeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = auditSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are OmniTube AI Producer, an elite YouTube automation director specializing in Viral Shorts, Funk Edits, Manim/Remotion animations, CTR optimization, and YPP monetization compliance. Keep advice concrete and structured."
                )
            }
        )
    }

    private val ttsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-tts-preview",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(Voice("Kore"))
            }
        )
    }

    private var activeChat: Chat? = null

    fun resetChat() {
        activeChat = null
    }

    suspend fun generateDailyBatch(
        userId: String,
        frequency: Int,
        niche: String,
        contentMode: ContentTypeMode,
        formatMode: String,
        shortsRatioPercent: Int,
        funkTrack: FunkTrack
    ): List<ProductionBatchItem> {
        val batchCount = frequency.coerceIn(1, 12)
        val slots = buildOptimalDailySlots(batchCount)
        val prompt = """
            Create $batchCount distinct YouTube production items for today's schedule.
            Niche: $niche
            Content Mode: ${contentMode.label} (${contentMode.wire})
            Format Mode: $formatMode (Shorts Ratio: $shortsRatioPercent%)
            Background Track: ${funkTrack.title} (${funkTrack.bpm} BPM, ${funkTrack.artistStyle})
            Time slots to map in order: ${slots.joinToString(", ")}
            Ensure every item is original, advertiser-friendly, YPP safe, and includes meme cues + Manim/Remotion snippet.
        """.trimIndent()

        val text = runWithTimeout {
            batchModel.generateContent(prompt).text
        }
        val root = JSONObject(text)
        val arr = root.optJSONArray("items") ?: JSONArray()
        val results = mutableListOf<ProductionBatchItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val slot = slots.getOrElse(i) { "${(8 + i) % 24}:00 UTC" }
            val fmt = when (formatMode) {
                "SHORTS_9_16" -> "9:16"
                "LONG_16_9" -> "16:9"
                else -> if (obj.optString("format") == "16:9") "16:9" else "9:16"
            }
            results.add(
                ProductionBatchItem(
                    userId = userId,
                    title = obj.optString("title", "Viral Production #${i + 1}").take(160),
                    hookScript = obj.optString("hookScript", "Hook viewers in the first 1.5 seconds.").take(4000),
                    format = fmt,
                    contentMode = contentMode.wire,
                    scheduledSlotLabel = slot,
                    funkTrackId = funkTrack.id,
                    funkTrackTitle = "${funkTrack.title} (${funkTrack.bpm} BPM)",
                    memeCues = obj.optJSONArray("memeCues").toStringList(12),
                    animationCode = obj.optString("animationCode", "# Manim animation script").take(12000),
                    seoDescription = obj.optString("seoDescription", "").take(3000),
                    seoTags = obj.optJSONArray("seoTags").toStringList(15),
                    yppScore = obj.optInt("yppScore", 92).coerceIn(0, 100),
                    estimatedRpmUsd = obj.optDouble("estimatedRpmUsd", 4.5).coerceIn(0.1, 250.0),
                    status = "READY"
                )
            )
        }
        return results
    }

    suspend fun generateQuickHooks(topic: String, niche: String): String {
        val prompt = "Give 4 punchy 1-sentence viral Shorts hooks for '$topic' in niche '$niche'. Keep it super tight."
        return runWithTimeout {
            fastHookModel.generateContent(prompt).text
        }
    }

    suspend fun generateAnimationPackage(
        topic: String,
        format: String,
        contentMode: ContentTypeMode,
        funkTrack: FunkTrack,
        referenceBitmap: Bitmap? = null
    ): AnimationPackageResult {
        val prompt = """
            Generate a complete production package for: "$topic"
            Format: $format | Mode: ${contentMode.label} | Funk Track: ${funkTrack.title} (${funkTrack.bpm} BPM)
            Include:
            1) A sharp 15-second retention script with beat drop timing
            2) Clean runnable Manim Python Scene code
            3) Clean React/TypeScript Remotion composition code
            4) JSON prompt payload formatted for Runway / Kling / Sora / Pika
            5) 4 meme/SFX cue placements synced to ${funkTrack.bpm} BPM
        """.trimIndent()

        val jsonText = runWithTimeout {
            val response = if (referenceBitmap != null) {
                animationCodeModel.generateContent(
                    content {
                        image(referenceBitmap)
                        text("$prompt\nUse the attached reference frame's visual composition and palette.")
                    }
                )
            } else {
                animationCodeModel.generateContent(prompt)
            }
            response.text
        }

        val json = JSONObject(jsonText)
        return AnimationPackageResult(
            hookScript = json.optString("hookScript"),
            manimCode = json.optString("manimCode"),
            remotionCode = json.optString("remotionCode"),
            videoApiPayload = json.optString("videoApiPayload"),
            memeCues = json.optJSONArray("memeCues").toStringList(12),
            seoTitle = json.optString("seoTitle"),
            seoTags = json.optJSONArray("seoTags").toStringList(15)
        )
    }

    suspend fun generateStudioImage(
        prompt: String,
        aspectRatio: AspectRatio,
        imageSize: ImageSize,
        studioQuality: Boolean,
        referenceBitmap: Bitmap? = null
    ): Bitmap {
        val modelName = if (studioQuality) "gemini-3-pro-image" else "gemini-nano-banana-2.1"
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio)
                .setImageSize(imageSize)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = config
        )

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = if (referenceBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(referenceBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    imageModel.generateContent(prompt)
                }
                val part = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                part?.image ?: throw IllegalStateException("Model returned no image data.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Image generation timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Image generation failed.", e)
        }
    }

    suspend fun auditScriptOrConcept(
        userId: String,
        title: String,
        scriptOrPrompt: String,
        niche: String
    ): YppAuditRecord {
        val prompt = """
            Audit this YouTube content concept for YPP monetization safety, Reused Content policy, Copyright risk, Advertiser-Friendliness, and RPM potential.
            Title: $title
            Niche: $niche
            Script/Concept: $scriptOrPrompt
        """.trimIndent()
        val jsonText = runWithTimeout {
            deepAuditModel.generateContent(prompt).text
        }
        return parseAuditJson(userId, title, jsonText)
    }

    suspend fun analyzeVideoForYpp(
        userId: String,
        title: String,
        videoBytes: ByteArray,
        mimeType: String
    ): YppAuditRecord {
        val jsonText = runWithTimeout {
            videoAnalyzeModel.generateContent(
                content {
                    inlineData(videoBytes, mimeType)
                    text(
                        "Analyze this video for hook strength, retention pacing, reused-content compliance, copyright audio/visual risk, advertiser friendliness, and projected RPM."
                    )
                }
            ).text
        }
        return parseAuditJson(userId, title, jsonText)
    }

    suspend fun generateSeoAndEngagement(topic: String, niche: String): SeoEngagementResult {
        val prompt = "Generate 5 high-CTR titles, SEO description, tags, a pinned comment hook, and 3 reply templates for topic '$topic' in niche '$niche'."
        val jsonText = runWithTimeout {
            seoModel.generateContent(prompt).text
        }
        val json = JSONObject(jsonText)
        return SeoEngagementResult(
            titles = json.optJSONArray("titles").toStringList(8),
            description = json.optString("description"),
            tags = json.optJSONArray("tags").toStringList(15),
            pinnedComment = json.optString("pinnedComment"),
            replyTemplates = json.optJSONArray("replyTemplates").toStringList(6)
        )
    }

    suspend fun researchTrendsGrounded(query: String, locationHint: String): String {
        val prompt = buildString {
            append("Research current viral YouTube Shorts & Long-Form opportunities for: $query. ")
            if (locationHint.isNotBlank()) {
                append("Also include trending creator spots or regional angles near: $locationHint. ")
            }
            append("Give 5 actionable video ideas with estimated RPM range, viral hook angle, and YPP safety note.")
        }
        return runWithTimeout {
            groundedTrendModel.generateContent(prompt).text
        }
    }

    suspend fun transcribeAudioWav(wavBytes: ByteArray): String {
        return runWithTimeout {
            multimodalModel.generateContent(
                content {
                    inlineData(wavBytes, "audio/wav")
                    text("Transcribe this voice recording verbatim, then summarize the core video idea in 1 crisp sentence.")
                }
            ).text
        }
    }

    suspend fun synthesizeSpeechAudio(scriptText: String): ByteArray {
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = ttsModel.generateContent(
                    "Read this YouTube script energetically and clearly:\n$scriptText"
                )
                val audioPart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<InlineDataPart>()
                    ?.firstOrNull { it.mimeType.startsWith("audio/") }
                audioPart?.inlineData
                    ?: throw IllegalStateException("TTS model returned no audio stream.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "TTS generation timed out", e)
            throw IllegalStateException("Speech generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "TTS generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Text-to-speech failed.", e)
        }
    }

    fun sendChatMessageStream(userMessage: String): Flow<String> = flow {
        val chat = activeChat ?: chatModel.startChat().also { activeChat = it }
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) emit(piece)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat stream timed out", e)
            throw IllegalStateException("AI response timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat stream failed", e)
            throw IllegalStateException(e.localizedMessage ?: "AI chat failed.", e)
        }
    }

    private suspend fun runWithTimeout(block: suspend () -> String?): String {
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val text = block()
                if (text.isNullOrBlank()) {
                    throw IllegalStateException("Model returned an empty response.")
                }
                text
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            throw IllegalStateException("AI generation timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Firebase AI generation failed.", e)
        }
    }

    private fun parseAuditJson(userId: String, title: String, jsonText: String): YppAuditRecord {
        val json = JSONObject(jsonText)
        return YppAuditRecord(
            userId = userId,
            targetTitle = title.ifBlank { "Uploaded Video Audit" }.take(200),
            safetyScore = json.optInt("safetyScore", 85).coerceIn(0, 100),
            estimatedRpmUsd = json.optDouble("estimatedRpmUsd", 4.0).coerceIn(0.0, 500.0),
            reusedContentRisk = json.optString("reusedContentRisk", "LOW")
                .takeIf { it in setOf("LOW", "MEDIUM", "HIGH") } ?: "LOW",
            copyrightRisk = json.optString("copyrightRisk", "LOW")
                .takeIf { it in setOf("LOW", "MEDIUM", "HIGH") } ?: "LOW",
            advertiserFriendly = json.optBoolean("advertiserFriendly", true),
            summary = json.optString("summary", "Audit completed.").take(2000),
            recommendations = json.optJSONArray("recommendations").toStringList(10)
        )
    }

    private fun JSONArray?.toStringList(max: Int): List<String> {
        if (this == null) return emptyList()
        return (0 until minOf(length(), max)).mapNotNull { idx ->
            optString(idx).takeIf { it.isNotBlank() }
        }
    }

    private fun buildOptimalDailySlots(count: Int): List<String> {
        val startMinutes = 8 * 60
        val endMinutes = 22 * 60
        if (count <= 1) return listOf("14:00 UTC")
        val step = (endMinutes - startMinutes) / (count - 1).coerceAtLeast(1)
        return (0 until count).map { idx ->
            val totalMin = (startMinutes + idx * step).coerceAtMost(23 * 60 + 45)
            val hr = totalMin / 60
            val min = totalMin % 60
            "%02d:%02d UTC".format(hr, min)
        }
    }
}
