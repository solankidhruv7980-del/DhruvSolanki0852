package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.ContentTypeMode
import com.example.data.model.FormatMode
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioCatalog
import com.example.data.model.StudioConfig
import com.example.ui.components.AsyncStateFeedback
import com.example.ui.components.RackModuleCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
import com.example.ui.theme.PolicyRed
import com.example.ui.theme.RackHairline
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.YppEmerald
import com.example.ui.viewmodel.StudioSnapshotBundle
import com.example.ui.viewmodel.StudioTransientState
import com.example.ui.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardTabScreen(
    bundle: StudioSnapshotBundle,
    transient: StudioTransientState,
    viewModel: StudioViewModel
) {
    val config = bundle.config
    val context = LocalContext.current
    var selectedBatch by remember { mutableStateOf<ProductionBatchItem?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startMicRecording()
        }
    }

    val activeTrack = StudioCatalog.funkTracks.firstOrNull { it.id == config.activeFunkTrackId }
        ?: StudioCatalog.funkTracks.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianFloor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MasterAutopilotHeroCard(
                config = config,
                isChannelLinked = bundle.connection.isLinked,
                activeTrackLabel = "${activeTrack.title} • ${activeTrack.bpm} BPM",
                onToggleMaster = viewModel::updateMasterSwitch
            )
        }

        item {
            DailyFrequencyCard(
                config = config,
                onFrequencyChange = viewModel::updateDailyFrequency,
                onContentTypeChange = viewModel::updateContentType,
                onFormatModeChange = viewModel::updateFormatMode,
                onNicheChange = viewModel::updatePrimaryNiche
            )
        }

        item {
            RackModuleCard(accentColor = TelemetryCyan.copy(alpha = 0.55f)) {
                SectionHeaderRow(
                    title = "Autonomous Batch Dispatcher",
                    subtitle = "Generates ${config.dailyVideoFrequency} spaced packages in ${ContentTypeMode.fromWire(config.contentType).label}",
                    badge = if (config.isMasterAutopilotOn) "ARMED" else "STANDBY",
                    badgeColor = if (config.isMasterAutopilotOn) YppEmerald else AmberSignal
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateDailyBatchFromAi() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("generate_batch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BroadcastCrimson,
                            contentColor = StudioTextPrimary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate ${config.dailyVideoFrequency} Daily Videos")
                    }

                    OutlinedButton(
                        onClick = {
                            if (transient.isRecordingMic) {
                                viewModel.stopMicAndTranscribe(sendToVoiceChat = false)
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.startMicRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("voice_memo_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            1.dp,
                            if (transient.isRecordingMic) BroadcastCrimson else TelemetryCyan
                        )
                    ) {
                        Icon(
                            imageVector = if (transient.isRecordingMic) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = "Voice Memo Transcription",
                            tint = if (transient.isRecordingMic) BroadcastCrimson else TelemetryCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.batchGenState,
                    loadingLabel = "Gemini 3.8 Flash synthesizing ${config.dailyVideoFrequency} daily packages..."
                ) { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = YppEmerald
                    )
                }

                AsyncStateFeedback(
                    state = transient.transcriptionState,
                    loadingLabel = if (transient.isRecordingMic) "Recording voice memo... Tap stop to transcribe" else "Transcribing audio with Gemini..."
                ) { transcript ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(RackSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "VOICE IDEA TRANSCRIPT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TelemetryCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextPrimary
                        )
                    }
                }
            }
        }

        item {
            SectionHeaderRow(
                title = "Today's Scheduled Queue",
                subtitle = "${bundle.batches.size} production packages persisted to cloud",
                badge = "${bundle.batches.count { it.status == "PUBLISHED" }} LIVE",
                badgeColor = BroadcastCrimson
            )
        }

        if (bundle.batches.isEmpty()) {
            item {
                RackModuleCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = TelemetryCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Queue is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Generate ${config.dailyVideoFrequency} Daily Videos' above to schedule today's AI Shorts & Long-Form batch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                }
            }
        } else {
            items(bundle.batches, key = { it.id.ifBlank { it.title + it.scheduledSlotLabel } }) { batch ->
                BatchQueueCard(
                    item = batch,
                    onClick = { selectedBatch = batch },
                    onAdvanceStatus = {
                        val next = when (batch.status) {
                            "QUEUED" -> "READY"
                            "READY" -> "AUDITED"
                            else -> "PUBLISHED"
                        }
                        viewModel.markBatchStatus(batch, next)
                    },
                    onDelete = { viewModel.deleteBatch(batch.id) }
                )
            }
        }
    }

    selectedBatch?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedBatch = null },
            containerColor = RackSurface,
            contentColor = StudioTextPrimary
        ) {
            BatchDetailSheet(
                item = item,
                onSpeakHook = { viewModel.speakScriptWithAiTts(item.hookScript) },
                onMarkPublished = {
                    viewModel.markBatchStatus(item, "PUBLISHED")
                    selectedBatch = null
                }
            )
        }
    }
}

@Composable
private fun MasterAutopilotHeroCard(
    config: StudioConfig,
    isChannelLinked: Boolean,
    activeTrackLabel: String,
    onToggleMaster: (Boolean) -> Unit
) {
    val activeAccent by animateColorAsState(
        targetValue = if (config.isMasterAutopilotOn) YppEmerald else BroadcastCrimson,
        label = "master_accent"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(1.dp, activeAccent.copy(alpha = 0.65f), MaterialTheme.shapes.extraLarge)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_studio_hero),
            contentDescription = "OmniTube Broadcast Deck",
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ObsidianFloor.copy(alpha = 0.94f),
                            RackSurface.copy(alpha = 0.78f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isMasterAutopilotOn) "AUTOPILOT ARMED" else "AUTOPILOT PAUSED",
                            style = MaterialTheme.typography.labelMedium,
                            color = activeAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Master Control Switch",
                        style = MaterialTheme.typography.headlineSmall,
                        color = StudioTextPrimary
                    )
                }

                Switch(
                    checked = config.isMasterAutopilotOn,
                    onCheckedChange = onToggleMaster,
                    modifier = Modifier.testTag("master_autopilot_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianFloor,
                        checkedTrackColor = YppEmerald,
                        uncheckedThumbColor = StudioTextPrimary,
                        uncheckedTrackColor = BroadcastCrimson.copy(alpha = 0.45f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(
                    text = "${config.dailyVideoFrequency} VIDEOS/DAY",
                    color = TelemetryCyan
                )
                TelemetryPill(
                    text = if (isChannelLinked) "YT LINKED" else "YT SETUP PENDING",
                    color = if (isChannelLinked) YppEmerald else AmberSignal
                )
            }

            Text(
                text = "Active Funk Bed: $activeTrackLabel",
                style = MaterialTheme.typography.labelMedium,
                color = StudioTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyFrequencyCard(
    config: StudioConfig,
    onFrequencyChange: (Int) -> Unit,
    onContentTypeChange: (ContentTypeMode) -> Unit,
    onFormatModeChange: (FormatMode, Int?) -> Unit,
    onNicheChange: (String) -> Unit
) {
    RackModuleCard {
        SectionHeaderRow(
            title = "Daily Video Frequency & Mix",
            subtitle = "Anti-spam algorithmic spacing from 1 to 10+ videos/day",
            badge = "${config.dailyVideoFrequency}/DAY",
            badgeColor = BroadcastCrimson
        )
        Spacer(modifier = Modifier.height(10.dp))

        Slider(
            value = config.dailyVideoFrequency.toFloat(),
            onValueChange = { onFrequencyChange(it.roundToInt().coerceIn(1, 15)) },
            valueRange = 1f..15f,
            steps = 13,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_frequency_slider"),
            colors = SliderDefaults.colors(
                thumbColor = BroadcastCrimson,
                activeTrackColor = TelemetryCyan,
                inactiveTrackColor = RackHairline
            )
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 2, 5, 10, 12, 15).forEach { preset ->
                val selected = config.dailyVideoFrequency == preset
                FilterChip(
                    selected = selected,
                    onClick = { onFrequencyChange(preset) },
                    label = { Text(if (preset >= 10) "$preset+ /day" else "$preset/day") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BroadcastCrimson.copy(alpha = 0.25f),
                        selectedLabelColor = BroadcastCrimson
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "CONTENT ENGINE MODE",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContentTypeMode.entries.forEach { mode ->
                val selected = config.contentType == mode.wire
                FilterChip(
                    selected = selected,
                    onClick = { onContentTypeChange(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TelemetryCyan.copy(alpha = 0.22f),
                        selectedLabelColor = TelemetryCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "FORMAT PRIORITY (DUAL / SHORTS / LONG-FORM)",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatMode.entries.forEach { fmt ->
                val selected = config.formatMode == fmt.wire
                FilterChip(
                    selected = selected,
                    onClick = { onFormatModeChange(fmt, null) },
                    label = { Text(fmt.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YppEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = YppEmerald
                    )
                )
            }
        }

        if (config.formatMode == FormatMode.DUAL.wire) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Dual Format Split: ${config.shortsRatioPercent}% Shorts (9:16) / ${100 - config.shortsRatioPercent}% Long-Form (16:9)",
                style = MaterialTheme.typography.labelMedium,
                color = StudioTextSecondary
            )
            Slider(
                value = config.shortsRatioPercent.toFloat(),
                onValueChange = {
                    onFormatModeChange(FormatMode.DUAL, (it.roundToInt() / 10) * 10)
                },
                valueRange = 10f..90f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = TelemetryCyan,
                    activeTrackColor = TelemetryCyan
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "CHANNEL NICHE PRESET",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudioCatalog.nicheOptions.forEach { niche ->
                FilterChip(
                    selected = config.primaryNiche == niche,
                    onClick = { onNicheChange(niche) },
                    label = { Text(niche, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@Composable
private fun BatchQueueCard(
    item: ProductionBatchItem,
    onClick: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isPublished = item.status == "PUBLISHED"
    val statusColor = when (item.status) {
        "PUBLISHED" -> YppEmerald
        "AUDITED" -> TelemetryCyan
        "READY" -> AmberSignal
        else -> StudioTextSecondary
    }
    RackModuleCard(
        modifier = Modifier.clickable(onClick = onClick),
        accentColor = statusColor.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TelemetryPill(text = item.scheduledSlotLabel, color = TelemetryCyan)
                TelemetryPill(text = item.format, color = BroadcastCrimson)
                TelemetryPill(text = item.status, color = statusColor)
            }
            if (!isPublished) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Batch Item",
                        tint = StudioTextSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.hookScript,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.funkTrackTitle} • YPP ${item.yppScore}%",
                style = MaterialTheme.typography.labelMedium,
                color = YppEmerald,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isPublished) {
                OutlinedButton(
                    onClick = onAdvanceStatus,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.status == "AUDITED") "Publish" else "Advance",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchDetailSheet(
    item: ProductionBatchItem,
    onSpeakHook: () -> Unit,
    onMarkPublished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryPill(text = item.format, color = BroadcastCrimson)
            TelemetryPill(text = "YPP ${item.yppScore}%", color = YppEmerald)
            TelemetryPill(text = "RPM $${"%.2f".format(item.estimatedRpmUsd)}", color = TelemetryCyan)
        }
        Text(text = "VIRAL HOOK SCRIPT", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
        Text(text = item.hookScript, style = MaterialTheme.typography.bodyMedium)

        if (item.memeCues.isNotEmpty()) {
            Text(text = "MEME & SFX CUES", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
            item.memeCues.forEach { cue ->
                Text(text = "• $cue", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
            }
        }

        if (item.animationCode.isNotBlank()) {
            Text(text = "ANIMATION CODE PAYLOAD", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(ObsidianFloor)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.animationCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = YppEmerald
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onSpeakHook, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Voiceover")
            }
            if (item.status != "PUBLISHED") {
                Button(
                    onClick = onMarkPublished,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = YppEmerald, contentColor = ObsidianFloor)
                ) {
                    Text("Mark Published")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
