package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ContentTypeMode
import com.example.data.model.FormatMode
import com.example.data.model.StudioConfig
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
import com.example.ui.theme.RackHairline
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.YppEmerald
import kotlin.math.roundToInt

data class DrawerTabShortcut(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun GeminiSidebarDrawerContent(
    userEmail: String,
    config: StudioConfig,
    activeTrackTitle: String,
    queuedCount: Int,
    hasReferencePhoto: Boolean,
    currentRoute: String?,
    tabs: List<DrawerTabShortcut>,
    onClose: () -> Unit,
    onUploadPhoto: () -> Unit,
    onUploadVideo: () -> Unit,
    onManageSchedules: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onSelectTab: (String) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        listOf(RackSurfaceElevated, ObsidianFloor)
                    )
                )
                .border(1.dp, TelemetryCyan.copy(alpha = 0.45f), MaterialTheme.shapes.large)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryPill(
                    text = if (config.isMasterAutopilotOn) "AUTOPILOT ON" else "AUTOPILOT OFF",
                    color = if (config.isMasterAutopilotOn) YppEmerald else BroadcastCrimson
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Sidebar",
                        tint = StudioTextSecondary
                    )
                }
            }
            Text(
                text = "OmniTube Gemini Studio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = StudioTextPrimary
            )
            Text(
                text = userEmail.ifBlank { "Google Studio Account Active" },
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TelemetryPill(text = "${config.dailyVideoFrequency} VIDEOS/DAY", color = TelemetryCyan)
                TelemetryPill(text = "$queuedCount QUEUED", color = AmberSignal)
            }
        }

        Text(
            text = "GEMINI MEDIA & SCHEDULE TOOLS",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )

        Button(
            onClick = onUploadPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("drawer_upload_photo_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = TelemetryCyan.copy(alpha = 0.16f),
                contentColor = TelemetryCyan
            ),
            border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.55f))
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (hasReferencePhoto) "Upload Photo (Frame Linked)" else "Upload Photo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onUploadVideo,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("drawer_upload_video_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = BroadcastCrimson.copy(alpha = 0.18f),
                contentColor = BroadcastCrimson
            ),
            border = BorderStroke(1.dp, BroadcastCrimson.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Upload Video for Gemini Audit",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onManageSchedules,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("drawer_manage_schedules_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = YppEmerald,
                contentColor = ObsidianFloor
            )
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Manage Auto-Pilot Schedules",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(RackSurfaceElevated)
                .border(1.dp, RackHairline, MaterialTheme.shapes.medium)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Master Auto-Pilot",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextPrimary
                )
                Text(
                    text = activeTrackTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = config.isMasterAutopilotOn,
                onCheckedChange = onToggleMaster,
                modifier = Modifier.testTag("drawer_master_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianFloor,
                    checkedTrackColor = YppEmerald,
                    uncheckedThumbColor = StudioTextPrimary,
                    uncheckedTrackColor = BroadcastCrimson.copy(alpha = 0.5f)
                )
            )
        }

        HorizontalDivider(color = RackHairline)

        Text(
            text = "STUDIO WORKSPACES",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )

        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationDrawerItem(
                selected = selected,
                onClick = { onSelectTab(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                modifier = Modifier.testTag("drawer_nav_${tab.route}"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = RackSurfaceElevated,
                    selectedIconColor = BroadcastCrimson,
                    selectedTextColor = StudioTextPrimary,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = StudioTextSecondary,
                    unselectedTextColor = StudioTextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("drawer_sign_out_button"),
            border = BorderStroke(1.dp, BroadcastCrimson.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = BroadcastCrimson,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", color = BroadcastCrimson)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoPilotScheduleManagerSheet(
    config: StudioConfig,
    queuedCount: Int,
    onToggleMaster: (Boolean) -> Unit,
    onFrequencyChange: (Int) -> Unit,
    onContentTypeChange: (ContentTypeMode) -> Unit,
    onFormatModeChange: (FormatMode, Int?) -> Unit,
    onDispatchBatch: () -> Unit,
    onOpenQueueTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Pilot Schedule Manager",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioTextPrimary
                )
                Text(
                    text = "$queuedCount items queued • ${config.dailyVideoFrequency} videos/day pace",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
            Switch(
                checked = config.isMasterAutopilotOn,
                onCheckedChange = onToggleMaster,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianFloor,
                    checkedTrackColor = YppEmerald,
                    uncheckedThumbColor = StudioTextPrimary,
                    uncheckedTrackColor = BroadcastCrimson.copy(alpha = 0.5f)
                )
            )
        }

        Column {
            Text(
                text = "DAILY VIDEO FREQUENCY: ${config.dailyVideoFrequency} VIDEOS/DAY",
                style = MaterialTheme.typography.labelMedium,
                color = TelemetryCyan
            )
            Slider(
                value = config.dailyVideoFrequency.toFloat(),
                onValueChange = { onFrequencyChange(it.roundToInt().coerceIn(1, 15)) },
                valueRange = 1f..15f,
                steps = 13,
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
                    FilterChip(
                        selected = config.dailyVideoFrequency == preset,
                        onClick = { onFrequencyChange(preset) },
                        label = { Text(if (preset >= 10) "$preset+ /day" else "$preset/day") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BroadcastCrimson.copy(alpha = 0.25f),
                            selectedLabelColor = BroadcastCrimson
                        )
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CONTENT TYPE ENGINE", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentTypeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.contentType == mode.wire,
                        onClick = { onContentTypeChange(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("FORMAT PRIORITY", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatMode.entries.forEach { fmt ->
                    FilterChip(
                        selected = config.formatMode == fmt.wire,
                        onClick = { onFormatModeChange(fmt, null) },
                        label = { Text(fmt.label) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenQueueTab,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Open Queue")
            }
            Button(
                onClick = onDispatchBatch,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate ${config.dailyVideoFrequency} Now")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
