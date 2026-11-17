package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.ui.viewmodel.AsyncUiState
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun RackModuleCard(
    modifier: Modifier = Modifier,
    accentColor: Color = RackHairline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = RackSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: Color = TelemetryCyan
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            TelemetryPill(text = badge, color = badgeColor)
        }
    }
}

@Composable
fun TelemetryPill(
    text: String,
    color: Color = TelemetryCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun <T> AsyncStateFeedback(
    state: AsyncUiState<T>,
    loadingLabel: String,
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is AsyncUiState.Idle -> Unit
        is AsyncUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(RackSurfaceElevated)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = TelemetryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loadingLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = TelemetryCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = TelemetryCyan,
                    trackColor = RackHairline
                )
            }
        }
        is AsyncUiState.Error -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(PolicyRed.copy(alpha = 0.12f))
                    .border(1.dp, PolicyRed.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    tint = PolicyRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextPrimary
                )
            }
        }
        is AsyncUiState.Success -> successContent(state.data)
    }
}

@Composable
fun KineticStudioViewport(
    headline: String,
    funkTitle: String,
    isShortsFormat: Boolean,
    beatPulse: Float,
    isPlayingAudio: Boolean,
    memeCues: List<String>
) {
    val infinite = rememberInfiniteTransition(label = "kinetic_viewport")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val glowColor by animateColorAsState(
        targetValue = if (isPlayingAudio) BroadcastCrimson else TelemetryCyan,
        label = "glow_color"
    )

    val ratio = if (isShortsFormat) 9f / 12f else 16f / 9f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.24f), ObsidianFloor)
                )
            )
            .border(1.dp, glowColor.copy(alpha = 0.6f), MaterialTheme.shapes.large)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barCount = 24
            val barWidth = w / (barCount * 1.6f)
            val gap = (w - barCount * barWidth) / (barCount + 1)

            // Subtle broadcast grid
            for (g in 1..3) {
                val y = h * (g / 4f)
                drawLine(
                    color = RackHairline.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Kinetic audio spectrum bars synced to beatPulse
            val pulseBoost = if (isPlayingAudio) (0.35f + beatPulse * 0.65f) else 0.28f
            for (i in 0 until barCount) {
                val wave = abs(sin(phase + i * 0.42f))
                val barH = (h * 0.34f) * wave * pulseBoost + 10f
                val x = gap + i * (barWidth + gap)
                val barColor = if (i % 3 == 0) BroadcastCrimson else TelemetryCyan
                drawRoundRect(
                    color = barColor.copy(alpha = 0.8f),
                    topLeft = Offset(x, h - barH - 22f),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryPill(
                    text = if (isShortsFormat) "9:16 SHORTS" else "16:9 WIDE",
                    color = TelemetryCyan
                )
                TelemetryPill(
                    text = if (isPlayingAudio) "LIVE GROOVE" else "PREVIEW READY",
                    color = if (isPlayingAudio) BroadcastCrimson else YppEmerald
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = headline.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (memeCues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = memeCues.take(2).joinToString("  •  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "TRACK: $funkTitle",
                style = MaterialTheme.typography.labelMedium,
                color = StudioTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnalyticsTelemetryCharts(
    dailyFrequency: Int,
    avgYppScore: Int,
    avgRpm: Double
) {
    RackModuleCard(accentColor = TelemetryCyan.copy(alpha = 0.5f)) {
        SectionHeaderRow(
            title = "Channel Velocity & Retention",
            subtitle = "Real-time algorithmic projection based on active schedule",
            badge = "RPM $${"%.2f".format(avgRpm)}",
            badgeColor = YppEmerald
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricMiniTile(
                label = "DAILY OUTPUT",
                value = "$dailyFrequency/day",
                accent = BroadcastCrimson,
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "YPP HEALTH",
                value = "$avgYppScore%",
                accent = YppEmerald,
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "EST. MONTHLY",
                value = "$${(dailyFrequency * 30 * avgRpm * 0.85).toInt()}",
                accent = TelemetryCyan,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "30-SECOND RETENTION CURVE (SHORTS + HOOK DROP)",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(ObsidianFloor)
                .padding(12.dp)
        ) {
            val points = listOf(1.0f, 0.94f, 0.89f, 0.86f, 0.83f, 0.80f, 0.77f, 0.75f)
            val w = size.width
            val h = size.height
            val stepX = w / (points.size - 1)

            val linePath = Path()
            points.forEachIndexed { idx, p ->
                val x = idx * stepX
                val y = h * (1f - p * 0.85f)
                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(
                path = linePath,
                color = TelemetryCyan,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, p ->
                val x = idx * stepX
                val y = h * (1f - p * 0.85f)
                drawCircle(
                    color = if (idx == 0 || idx == 3) BroadcastCrimson else YppEmerald,
                    radius = 7f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(RackSurfaceElevated)
            .border(1.dp, accent.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1
        )
    }
}
