package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.AnalyticsTelemetryCharts
import com.example.ui.components.AsyncStateFeedback
import com.example.ui.components.RackModuleCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
import com.example.ui.theme.PolicyRed
import com.example.ui.theme.RackHairline
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.YppEmerald
import com.example.ui.viewmodel.StudioSnapshotBundle
import com.example.ui.viewmodel.StudioTransientState
import com.example.ui.viewmodel.StudioViewModel

private const val MAX_INLINE_VIDEO_BYTES = 18 * 1024 * 1024

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntelYppTabScreen(
    bundle: StudioSnapshotBundle,
    transient: StudioTransientState,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var auditTitle by remember { mutableStateOf("AI Funk Retention Breakdown #1") }
    var auditScript by remember {
        mutableStateOf("Original 3D kinetic typography synced with synthesized 132 BPM phonk groove, custom voiceover hook, and zero third-party clips.")
    }
    var trendQuery by remember { mutableStateOf("Viral Shorts Funk Edits and High-RPM AI Explainers") }
    var locationHint by remember { mutableStateOf("") }
    var seoTopic by remember { mutableStateOf("How Top Faceless Channels Automate 5 Shorts Daily") }
    var chatInput by remember { mutableStateOf("") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    Toast.makeText(context, "Could not read selected video.", Toast.LENGTH_SHORT).show()
                } else if (bytes.size > MAX_INLINE_VIDEO_BYTES) {
                    Toast.makeText(
                        context,
                        "Video exceeds 18MB inline upload limit. Pick a shorter clip.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                    viewModel.analyzePickedVideoBytes("Device Clip Audit", bytes, mime)
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Video read failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val voiceMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startMicRecording()
        }
    }

    val avgYpp = if (bundle.batches.isNotEmpty()) {
        bundle.batches.map { it.yppScore }.average().toInt()
    } else 94
    val avgRpm = if (bundle.batches.isNotEmpty()) {
        bundle.batches.map { it.estimatedRpmUsd }.average()
    } else 4.65

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianFloor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Analytics & Velocity Deck
        item {
            AnalyticsTelemetryCharts(
                dailyFrequency = bundle.config.dailyVideoFrequency,
                avgYppScore = avgYpp,
                avgRpm = avgRpm
            )
        }

        // 2. Monetization & YPP Compliance Auditor (Deep Thinking + Video Understanding)
        item {
            RackModuleCard(accentColor = YppEmerald.copy(alpha = 0.55f)) {
                SectionHeaderRow(
                    title = "YPP Monetization & Safety Auditor",
                    subtitle = "Deep Thinking compliance check + direct video clip analysis",
                    badge = "YPP GUARD",
                    badgeColor = YppEmerald
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = auditTitle,
                    onValueChange = { auditTitle = it },
                    label = { Text("Video Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = auditScript,
                    onValueChange = { auditScript = it },
                    label = { Text("Script, Audio Source & Visual Plan") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.auditConceptOrBatch(auditTitle, auditScript) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("run_ypp_audit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YppEmerald,
                            contentColor = ObsidianFloor
                        )
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Script")
                    }

                    OutlinedButton(
                        onClick = {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("analyze_video_button")
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Video")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.auditActionState,
                    loadingLabel = "Gemini 4 Argon running high-thinking YPP audit..."
                ) { record ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(RackSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TelemetryPill(
                                text = "SAFETY ${record.safetyScore}/100",
                                color = if (record.safetyScore >= 80) YppEmerald else AmberSignal
                            )
                            TelemetryPill(
                                text = "REUSED: ${record.reusedContentRisk}",
                                color = if (record.reusedContentRisk == "LOW") YppEmerald else PolicyRed
                            )
                            TelemetryPill(
                                text = "RPM $${"%.2f".format(record.estimatedRpmUsd)}",
                                color = TelemetryCyan
                            )
                        }
                        Text(text = record.summary, style = MaterialTheme.typography.bodyMedium)
                        record.recommendations.forEach { rec ->
                            Text(text = "• $rec", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                        }
                    }
                }

                if (bundle.audits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("RECENT CLOUD AUDITS", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    bundle.audits.take(3).forEach { past ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = past.targetTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryPill(
                                text = "${past.safetyScore}% • $${"%.2f".format(past.estimatedRpmUsd)}",
                                color = if (past.safetyScore >= 80) YppEmerald else AmberSignal
                            )
                        }
                    }
                }
            }
        }

        // 3. Gemini Trend Intelligence & Grounded Search/Maps Scraper
        item {
            RackModuleCard(accentColor = TelemetryCyan.copy(alpha = 0.5f)) {
                SectionHeaderRow(
                    title = "Gemini Trend & Niche Scraper",
                    subtitle = "Grounded with Google Search & Maps creator trend intel",
                    badge = "GROUNDED",
                    badgeColor = TelemetryCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = trendQuery,
                    onValueChange = { trendQuery = it },
                    label = { Text("Niche or Viral Topic Query") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = locationHint,
                    onValueChange = { locationHint = it },
                    label = { Text("Optional City / Region for Local Vlog & Creator Spots") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.runGroundedTrendSearch(trendQuery, locationHint) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scrape_trends_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianFloor)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scrape Trends")
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateQuickHooks(trendQuery) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("fast_hooks_button")
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fast Hooks")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.trendSearchState,
                    loadingLabel = "Scraping real-time trends via Google Search & Maps..."
                ) { text ->
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = StudioTextPrimary)
                }

                AsyncStateFeedback(
                    state = transient.quickHookState,
                    loadingLabel = "Gemini 3.5 Flash-Lite generating fast hooks..."
                ) { hooks ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(RackSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Text("LOW-LATENCY HOOK VARIATIONS", style = MaterialTheme.typography.labelSmall, color = AmberSignal)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(hooks, style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
                    }
                }
            }
        }

        // 4. SEO Title, Tags & Engagement Loop Generator
        item {
            RackModuleCard {
                SectionHeaderRow(
                    title = "SEO & Engagement Loop Engine",
                    subtitle = "High-CTR titles, tags, pinned comments & reply templates",
                    badge = "CTR + SEO",
                    badgeColor = BroadcastCrimson
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = seoTopic,
                    onValueChange = { seoTopic = it },
                    label = { Text("Video Topic for SEO Optimization") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.generateSeoBundle(seoTopic) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate High-CTR SEO Package")
                }
                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.seoGenState,
                    loadingLabel = "Generating CTR titles, SEO tags & pinned comment loop..."
                ) { seo ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TOP CTR TITLES", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                        seo.titles.forEach { t ->
                            Text("• $t", style = MaterialTheme.typography.bodyMedium, color = StudioTextPrimary)
                        }
                        Text("PINNED COMMENT LOOP", style = MaterialTheme.typography.labelSmall, color = YppEmerald)
                        Text(seo.pinnedComment, style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            seo.tags.forEach { tag -> TelemetryPill(text = "#$tag", color = TelemetryCyan) }
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(
                                        "${seo.titles.firstOrNull().orEmpty()}\n\n${seo.description}\n\nTags: ${seo.tags.joinToString(", ")}"
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Full SEO Package")
                        }
                    }
                }
            }
        }

        // 5. Multi-Turn OmniTube AI Producer Chatbot & Voice Conversation
        item {
            RackModuleCard(accentColor = BroadcastCrimson.copy(alpha = 0.55f)) {
                SectionHeaderRow(
                    title = "OmniTube AI Producer Chat",
                    subtitle = "Multi-turn strategist with streaming replies & voice conversation",
                    badge = "LIVE CHAT",
                    badgeColor = BroadcastCrimson
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(ObsidianFloor)
                        .border(1.dp, RackHairline, MaterialTheme.shapes.medium)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    transient.chatMessages.forEach { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (msg.isUser) BroadcastCrimson.copy(alpha = 0.2f)
                                        else RackSurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (msg.isUser) BroadcastCrimson.copy(alpha = 0.5f) else RackHairline,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (msg.isUser) "CREATOR" else "OMNITUBE AI",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (msg.isUser) BroadcastCrimson else TelemetryCyan
                                    )
                                    if (!msg.isUser && !msg.isStreaming && msg.text.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.speakScriptWithAiTts(msg.text.take(450)) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.VolumeUp,
                                                contentDescription = "Speak reply",
                                                tint = TelemetryCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text.ifBlank { "Thinking..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask about viral pacing, RPM, or Manim...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    IconButton(
                        onClick = {
                            if (transient.isRecordingMic) {
                                viewModel.stopMicAndTranscribe(sendToVoiceChat = true)
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.startMicRecording()
                                } else {
                                    voiceMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (transient.isRecordingMic) BroadcastCrimson else RackSurfaceElevated)
                            .border(
                                1.dp,
                                if (transient.isRecordingMic) BroadcastCrimson else TelemetryCyan,
                                MaterialTheme.shapes.small
                            )
                    ) {
                        Icon(
                            imageVector = if (transient.isRecordingMic) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = "Voice Conversation",
                            tint = StudioTextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            val prompt = chatInput
                            chatInput = ""
                            viewModel.sendChatMessage(prompt)
                        },
                        enabled = chatInput.isNotBlank() && !transient.isChatStreaming,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(BroadcastCrimson)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send Chat Message",
                            tint = StudioTextPrimary
                        )
                    }
                }
            }
        }
    }
}
