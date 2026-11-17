package com.example.ui.viewmodel

import android.graphics.Bitmap
import com.example.data.model.ChannelConnection
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioConfig
import com.example.data.model.YppAuditRecord
import com.example.data.repository.AnimationPackageResult
import com.example.data.repository.SeoEngagementResult
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageSize

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data object Loading : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String) : AsyncUiState<Nothing>
}

sealed interface StreamUiState<out T> {
    data object Loading : StreamUiState<Nothing>
    data class Ready<T>(val data: T) : StreamUiState<T>
    data class Error(val message: String) : StreamUiState<Nothing>
}

data class StudioChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

enum class StudioAspectOption(val label: String, val ratio: AspectRatio) {
    RATIO_9_16("9:16", AspectRatio.PORTRAIT_9x16),
    RATIO_16_9("16:9", AspectRatio.LANDSCAPE_16x9),
    RATIO_1_1("1:1", AspectRatio.SQUARE_1x1),
    RATIO_4_3("4:3", AspectRatio.LANDSCAPE_4x3),
    RATIO_3_4("3:4", AspectRatio.PORTRAIT_3x4),
    RATIO_3_2("3:2", AspectRatio.LANDSCAPE_3x2),
    RATIO_2_3("2:3", AspectRatio.PORTRAIT_2x3),
    RATIO_21_9("21:9", AspectRatio.LANDSCAPE_21x9)
}

enum class StudioImageSizeOption(val label: String, val size: ImageSize) {
    RES_1K("1K", ImageSize.SIZE_1K),
    RES_2K("2K", ImageSize.SIZE_2K),
    RES_4K("4K", ImageSize.SIZE_4K)
}

data class StudioTransientState(
    val batchGenState: AsyncUiState<String> = AsyncUiState.Idle,
    val quickHookState: AsyncUiState<String> = AsyncUiState.Idle,
    val animationGenState: AsyncUiState<AnimationPackageResult> = AsyncUiState.Idle,
    val imageGenState: AsyncUiState<Bitmap> = AsyncUiState.Idle,
    val selectedAspect: StudioAspectOption = StudioAspectOption.RATIO_9_16,
    val selectedImageSize: StudioImageSizeOption = StudioImageSizeOption.RES_1K,
    val useProImageModel: Boolean = false,
    val referenceBitmap: Bitmap? = null,
    val trendSearchState: AsyncUiState<String> = AsyncUiState.Idle,
    val auditActionState: AsyncUiState<YppAuditRecord> = AsyncUiState.Idle,
    val seoGenState: AsyncUiState<SeoEngagementResult> = AsyncUiState.Idle,
    val ttsState: AsyncUiState<String> = AsyncUiState.Idle,
    val transcriptionState: AsyncUiState<String> = AsyncUiState.Idle,
    val isRecordingMic: Boolean = false,
    val isVoiceConversationActive: Boolean = false,
    val chatMessages: List<StudioChatMessage> = listOf(
        StudioChatMessage(
            id = "welcome",
            isUser = false,
            text = "OmniTube AI Producer online. Ask me for viral hook breakdowns, funk edit pacing, Manim scenes, or YPP compliance checks."
        )
    ),
    val isChatStreaming: Boolean = false,
    val activeTutorialIndex: Int = 0,
    val isTutorialPlaying: Boolean = false,
    val tutorialProgress: Float = 0f,
    val statusBanner: String? = null
)

data class StudioSnapshotBundle(
    val config: StudioConfig = StudioConfig(),
    val batches: List<ProductionBatchItem> = emptyList(),
    val audits: List<YppAuditRecord> = emptyList(),
    val connection: ChannelConnection = ChannelConnection()
)
