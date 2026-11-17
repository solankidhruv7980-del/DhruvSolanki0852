package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudioCatalog
import com.example.ui.components.AsyncStateFeedback
import com.example.ui.components.KineticStudioViewport
import com.example.ui.components.RackModuleCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
import com.example.ui.theme.RackHairline
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.YppEmerald
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.StudioAspectOption
import com.example.ui.viewmodel.StudioImageSizeOption
import com.example.ui.viewmodel.StudioSnapshotBundle
import com.example.ui.viewmodel.StudioTransientState
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoEngineTabScreen(
    bundle: StudioSnapshotBundle,
    transient: StudioTransientState,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val playingTrackId by viewModel.audioEngine.playingTrackId.collectAsStateWithLifecycle()
    val beatPulse by viewModel.audioEngine.beatPulse.collectAsStateWithLifecycle()

    val activeTrack = StudioCatalog.funkTracks.firstOrNull {
        it.id == bundle.config.activeFunkTrackId
    } ?: StudioCatalog.funkTracks.first()

    var topicInput by remember { mutableStateOf("Why 90% of Shorts Lose Retention at 1.8 Seconds") }
    var selectedFormat by remember { mutableStateOf("9:16") }
    var selectedCodeTab by remember { mutableIntStateOf(0) }
    var imagePrompt by remember {
        mutableStateOf("Cyberpunk neon audio studio equalizer exploding with electric cyan and crimson waveforms, ultra crisp YouTube thumbnail")
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    viewModel.setReferenceBitmap(bmp)
                }
            } catch (_: Exception) {
            }
        }
    }

    val previewPkg = (transient.animationGenState as? AsyncUiState.Success)?.data

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianFloor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KineticStudioViewport(
                headline = previewPkg?.seoTitle?.ifBlank { topicInput } ?: topicInput,
                funkTitle = "${activeTrack.title} (${activeTrack.bpm} BPM)",
                isShortsFormat = selectedFormat == "9:16",
                beatPulse = beatPulse,
                isPlayingAudio = playingTrackId != null,
                memeCues = previewPkg?.memeCues ?: listOf("0:01 Sub Boom", "0:04 Glitch Cut")
            )
        }

        // 1. AI Animation & Video Prompt/Code Engine
        item {
            RackModuleCard(accentColor = TelemetryCyan.copy(alpha = 0.5f)) {
                SectionHeaderRow(
                    title = "AI Animation & Video Engine",
                    subtitle = "Generates Manim, Remotion & Runway/Kling/Sora/Pika payloads",
                    badge = "GEMINI 4 ARGON",
                    badgeColor = TelemetryCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("Video Topic or Scene Concept") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("9:16", "16:9").forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(if (fmt == "9:16") "9:16 Shorts" else "16:9 Wide") }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (transient.referenceBitmap != null) "Image Linked" else "Ref Photo")
                    }
                }

                if (transient.referenceBitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            bitmap = transient.referenceBitmap.asImageBitmap(),
                            contentDescription = "Reference frame",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.small)
                                .border(1.dp, TelemetryCyan, MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Reference frame attached for scene & image remixing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetryCyan,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { viewModel.setReferenceBitmap(null) }) {
                            Text("Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.generateAnimationPackage(topicInput, selectedFormat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_animation_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesize Script, Code & Video Payload")
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.animationGenState,
                    loadingLabel = "Synthesizing Manim, Remotion & Runway/Kling payloads..."
                ) { pkg ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = pkg.hookScript,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.speakScriptWithAiTts(pkg.hookScript) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI TTS Voiceover")
                            }
                        }

                        AsyncStateFeedback(
                            state = transient.ttsState,
                            loadingLabel = "Generating Gemini 3.1 Flash TTS voiceover..."
                        ) { status ->
                            Text(status, style = MaterialTheme.typography.labelMedium, color = YppEmerald)
                        }

                        TabRow(
                            selectedTabIndex = selectedCodeTab,
                            containerColor = RackSurfaceElevated,
                            contentColor = TelemetryCyan
                        ) {
                            listOf("Manim", "Remotion", "Video API").forEachIndexed { idx, label ->
                                Tab(
                                    selected = selectedCodeTab == idx,
                                    onClick = { selectedCodeTab = idx },
                                    text = { Text(label) }
                                )
                            }
                        }

                        val activeCode = when (selectedCodeTab) {
                            0 -> pkg.manimCode
                            1 -> pkg.remotionCode
                            else -> pkg.videoApiPayload
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(ObsidianFloor)
                                .border(1.dp, RackHairline, MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { clipboard.setText(AnnotatedString(activeCode)) }
                                    ) {
                                        Icon(
                                            Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy Code",
                                            tint = TelemetryCyan
                                        )
                                    }
                                }
                                Text(
                                    text = activeCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = YppEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. AI Thumbnail & Scene Frame Generator (Aspect Ratio + 1K/2K/4K)
        item {
            RackModuleCard(accentColor = BroadcastCrimson.copy(alpha = 0.5f)) {
                SectionHeaderRow(
                    title = "AI Thumbnail & Scene Generator",
                    subtitle = "Control aspect ratio & 1K/2K/4K resolution via Nano Banana",
                    badge = if (transient.useProImageModel) "PRO 3" else "NANO 2.1",
                    badgeColor = BroadcastCrimson
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { imagePrompt = it },
                    label = { Text("Thumbnail or Scene Art Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("ASPECT RATIO", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StudioAspectOption.entries.forEach { aspect ->
                        FilterChip(
                            selected = transient.selectedAspect == aspect,
                            onClick = { viewModel.setSelectedAspect(aspect) },
                            label = { Text(aspect.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudioImageSizeOption.entries.forEach { sz ->
                            FilterChip(
                                selected = transient.selectedImageSize == sz,
                                onClick = { viewModel.setSelectedImageSize(sz) },
                                label = { Text(sz.label) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pro", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = transient.useProImageModel,
                            onCheckedChange = viewModel::setUseProImageModel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.generateThumbnailImage(imagePrompt) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_thumbnail_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianFloor)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render Studio Frame (${transient.selectedAspect.label} • ${transient.selectedImageSize.label})")
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsyncStateFeedback(
                    state = transient.imageGenState,
                    loadingLabel = "Rendering ${transient.selectedImageSize.label} frame (30–60s)..."
                ) { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated Studio Frame",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .border(1.dp, TelemetryCyan, MaterialTheme.shapes.large),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // 3. Meme & SFX Placement Manager
        item {
            RackModuleCard {
                SectionHeaderRow(
                    title = "Meme & SFX Placement Rack",
                    subtitle = "Tap any SFX trigger to audition synthesized drop timing",
                    badge = "5 SFX",
                    badgeColor = TelemetryCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioCatalog.memeSfxPresets.forEach { sfx ->
                        OutlinedButton(
                            onClick = { viewModel.audioEngine.playSfx(sfx.toneType) },
                            border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = sfx.name,
                                tint = TelemetryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(sfx.name, style = MaterialTheme.typography.labelLarge)
                                Text(sfx.timingTip, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 4. Customizable Funk Music & Song Swapper Library
        item {
            RackModuleCard(accentColor = YppEmerald.copy(alpha = 0.5f)) {
                SectionHeaderRow(
                    title = "Funk Music & Song Swapper",
                    subtitle = "Live PCM groove preview + 1-tap swap across queued shorts",
                    badge = "${activeTrack.bpm} BPM ACTIVE",
                    badgeColor = YppEmerald
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioCatalog.funkTracks.forEach { track ->
                        val isActive = track.id == activeTrack.id
                        val isPreviewing = playingTrackId == track.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (isActive) YppEmerald.copy(alpha = 0.1f) else RackSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isActive) YppEmerald else RackHairline,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.audioEngine.toggleFunkPreview(track) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (isPreviewing) BroadcastCrimson else RackSurface)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPreviewing) "Stop preview" else "Preview ${track.title}",
                                    tint = StudioTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = if (isActive) YppEmerald else TelemetryCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${track.bpm} BPM • ${track.artistStyle} • ${track.vibeTag}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isActive) {
                                TelemetryPill(text = "ACTIVE", color = YppEmerald)
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.swapActiveFunkTrack(track, applyToQueued = true) },
                                    modifier = Modifier.testTag("swap_funk_${track.id}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Swap")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
