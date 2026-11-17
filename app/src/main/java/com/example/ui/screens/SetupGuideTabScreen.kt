package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOutFromStudio
import com.example.data.model.StudioCatalog
import com.example.ui.components.AsyncStateFeedback
import com.example.ui.components.RackModuleCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupGuideTabScreen(
    bundle: StudioSnapshotBundle,
    transient: StudioTransientState,
    userEmail: String,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val activeModule = StudioCatalog.setupModules[transient.activeTutorialIndex]
    val activeStepIdx = ((transient.tutorialProgress * activeModule.steps.size).toInt())
        .coerceIn(0, activeModule.steps.lastIndex)

    var handle by remember { mutableStateOf(bundle.connection.channelHandle) }
    var channelId by remember { mutableStateOf(bundle.connection.channelId) }
    var clientId by remember { mutableStateOf(bundle.connection.oauthClientId) }
    var selectedScopes by remember { mutableStateOf(bundle.connection.grantedScopes.toSet()) }

    LaunchedEffect(bundle.connection) {
        if (handle.isBlank() && bundle.connection.channelHandle.isNotBlank()) {
            handle = bundle.connection.channelHandle
        }
        if (channelId.isBlank() && bundle.connection.channelId.isNotBlank()) {
            channelId = bundle.connection.channelId
        }
        if (clientId.isBlank() && bundle.connection.oauthClientId.isNotBlank()) {
            clientId = bundle.connection.oauthClientId
        }
        if (bundle.connection.grantedScopes.isNotEmpty()) {
            selectedScopes = bundle.connection.grantedScopes.toSet()
        }
    }

    val allScopes = listOf(
        "https://www.googleapis.com/auth/youtube.upload" to "youtube.upload",
        "https://www.googleapis.com/auth/youtube.readonly" to "youtube.readonly",
        "https://www.googleapis.com/auth/youtube.force-ssl" to "youtube.force-ssl"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianFloor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Embedded In-App Setup & Master Control Video Guide
        item {
            RackModuleCard(accentColor = BroadcastCrimson.copy(alpha = 0.6f)) {
                SectionHeaderRow(
                    title = "Setup & Master Control Video Guide",
                    subtitle = "Interactive 4-module walkthrough with live control simulation",
                    badge = "MODULE ${activeModule.number}/4",
                    badgeColor = BroadcastCrimson
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Embedded Video Player Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                listOf(ObsidianFloor, RackSurfaceElevated)
                            )
                        )
                        .border(1.dp, TelemetryCyan.copy(alpha = 0.55f), MaterialTheme.shapes.large)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val progress = transient.tutorialProgress

                        // Animated broadcast schematic nodes representing the active tutorial module
                        val nodeCount = activeModule.steps.size
                        val nodeW = w / (nodeCount + 1.5f)
                        for (i in 0 until nodeCount) {
                            val cx = (i + 1) * (w / (nodeCount + 1))
                            val cy = h * 0.44f
                            val isActiveNode = i == activeStepIdx
                            drawRoundRect(
                                color = if (isActiveNode) BroadcastCrimson else TelemetryCyan.copy(alpha = 0.35f),
                                topLeft = Offset(cx - nodeW * 0.34f, cy - 28f),
                                size = Size(nodeW * 0.68f, 56f),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                            if (i < nodeCount - 1) {
                                val nextX = (i + 2) * (w / (nodeCount + 1))
                                drawLine(
                                    color = if (i < activeStepIdx) YppEmerald else RackHairline,
                                    start = Offset(cx + nodeW * 0.34f, cy),
                                    end = Offset(nextX - nodeW * 0.34f, cy),
                                    strokeWidth = 4f
                                )
                            }
                        }

                        // Animated scrub playhead line
                        drawLine(
                            color = BroadcastCrimson,
                            start = Offset(w * progress, h - 10f),
                            end = Offset(w * progress, h),
                            strokeWidth = 6f
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelemetryPill(
                                text = "VIDEO GUIDE • ${activeModule.durationLabel}",
                                color = BroadcastCrimson
                            )
                            TelemetryPill(
                                text = "STEP ${activeStepIdx + 1}/${activeModule.steps.size}",
                                color = TelemetryCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(ObsidianFloor.copy(alpha = 0.88f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = activeModule.steps[activeStepIdx],
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = StudioTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = viewModel::toggleTutorialPlay,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BroadcastCrimson)
                            .testTag("tutorial_play_button")
                    ) {
                        Icon(
                            imageVector = if (transient.isTutorialPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or pause guide",
                            tint = StudioTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = transient.tutorialProgress,
                        onValueChange = viewModel::seekTutorial,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = BroadcastCrimson,
                            activeTrackColor = TelemetryCyan
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.speakScriptWithAiTts(activeModule.voiceScript) }
                    ) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Narrate")
                    }
                }

                AsyncStateFeedback(
                    state = transient.ttsState,
                    loadingLabel = "Generating AI voice narration..."
                ) { msg ->
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = YppEmerald)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioCatalog.setupModules.forEachIndexed { idx, mod ->
                        val selected = idx == transient.activeTutorialIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (selected) TelemetryCyan.copy(alpha = 0.1f) else RackSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (selected) TelemetryCyan else RackHairline,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable { viewModel.selectTutorialModule(idx) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = if (selected) TelemetryCyan else StudioTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Module ${mod.number}: ${mod.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StudioTextPrimary
                                    )
                                }
                                Text(
                                    text = mod.durationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TelemetryCyan
                                )
                            }
                            if (selected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                mod.steps.forEachIndexed { sIdx, stepText ->
                                    Text(
                                        text = "${sIdx + 1}. $stepText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sIdx == activeStepIdx) TelemetryCyan else StudioTextSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. YouTube Data API & OAuth 2.0 Connection Dashboard
        item {
            RackModuleCard(accentColor = YppEmerald.copy(alpha = 0.5f)) {
                SectionHeaderRow(
                    title = "YouTube API & OAuth 2.0 Dashboard",
                    subtitle = "Securely store channel credentials and required publishing scopes",
                    badge = if (bundle.connection.isLinked) "LINKED" else "UNLINKED",
                    badgeColor = if (bundle.connection.isLinked) YppEmerald else AmberSignal
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("YouTube Channel Handle (e.g. @omnitube)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = channelId,
                    onValueChange = { channelId = it },
                    label = { Text("YouTube Channel ID (UC...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Google Cloud OAuth 2.0 Client ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("OAUTH 2.0 SCOPES", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allScopes.forEach { (fullScope, shortLabel) ->
                        val active = fullScope in selectedScopes
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedScopes = if (active) selectedScopes - fullScope else selectedScopes + fullScope
                            },
                            label = { Text(shortLabel) },
                            leadingIcon = if (active) {
                                {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val readyToLink = handle.isNotBlank() && channelId.isNotBlank() && selectedScopes.isNotEmpty()
                        viewModel.saveYoutubeConnection(
                            handle = handle,
                            channelId = channelId,
                            clientId = clientId,
                            scopes = selectedScopes.toList(),
                            linked = readyToLink
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_youtube_oauth_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = YppEmerald, contentColor = ObsidianFloor)
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Link YouTube Channel")
                }
            }
        }

        // 3. Creator Session & Sign Out
        item {
            RackModuleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Authenticated Studio Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = userEmail.ifBlank { "Google Sign-In Active" },
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            signOutFromStudio(credentialManager, scope)
                        },
                        border = BorderStroke(1.dp, BroadcastCrimson),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            tint = BroadcastCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", color = BroadcastCrimson)
                    }
                }
            }
        }
    }
}
