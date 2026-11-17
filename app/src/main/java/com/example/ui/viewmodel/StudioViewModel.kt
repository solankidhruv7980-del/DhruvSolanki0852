package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.audio.MicRecorderHelper
import com.example.audio.StudioAudioEngine
import com.example.data.model.ChannelConnection
import com.example.data.model.ContentTypeMode
import com.example.data.model.FormatMode
import com.example.data.model.FunkTrack
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioCatalog
import com.example.data.model.StudioConfig
import com.example.data.model.YppAuditRecord
import com.example.data.repository.FirebaseAiStudioRepository
import com.example.data.repository.StudioFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "StudioViewModel"

class StudioViewModel(
    private val firestoreRepo: StudioFirestoreRepository,
    private val aiRepo: FirebaseAiStudioRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String
) : ViewModel() {

    val audioEngine = StudioAudioEngine(viewModelScope)
    private val micRecorder = MicRecorderHelper()
    private var tutorialTickerJob: Job? = null

    val snapshotState: StateFlow<StreamUiState<StudioSnapshotBundle>> = combine(
        firestoreRepo.observeConfig(currentUserId),
        firestoreRepo.observeBatches(currentUserId),
        firestoreRepo.observeAudits(currentUserId),
        firestoreRepo.observeChannelConnection(currentUserId)
    ) { config, batches, audits, conn ->
        StreamUiState.Ready(
            StudioSnapshotBundle(
                config = config ?: StudioConfig(userId = currentUserId),
                batches = batches,
                audits = audits,
                connection = conn ?: ChannelConnection(userId = currentUserId)
            )
        ) as StreamUiState<StudioSnapshotBundle>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing studio workspace", rootError)
        emit(StreamUiState.Error(rootError.message ?: "Failed to sync studio workspace."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StreamUiState.Loading
    )

    private val _transient = MutableStateFlow(StudioTransientState())
    val transient: StateFlow<StudioTransientState> = _transient.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                firestoreRepo.ensureUserWorkspaceInitialized(userDisplayName, userEmail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed workspace bootstrap", e)
                _transient.update {
                    it.copy(statusBanner = e.localizedMessage ?: "Workspace sync error.")
                }
            }
        }
    }

    private fun currentConfig(): StudioConfig {
        return (snapshotState.value as? StreamUiState.Ready)?.data?.config
            ?: StudioConfig(userId = currentUserId)
    }

    private fun currentBatches(): List<ProductionBatchItem> {
        return (snapshotState.value as? StreamUiState.Ready)?.data?.batches.orEmpty()
    }

    fun clearStatusBanner() {
        _transient.update { it.copy(statusBanner = null) }
    }

    fun updateMasterSwitch(enabled: Boolean) {
        val updated = currentConfig().copy(isMasterAutopilotOn = enabled)
        persistConfig(updated)
    }

    fun updateDailyFrequency(freq: Int) {
        val clamped = freq.coerceIn(1, 20)
        val updated = currentConfig().copy(dailyVideoFrequency = clamped)
        persistConfig(updated)
    }

    fun updateContentType(mode: ContentTypeMode) {
        val updated = currentConfig().copy(contentType = mode.wire)
        persistConfig(updated)
    }

    fun updateFormatMode(mode: FormatMode, shortsRatioPercent: Int? = null) {
        val current = currentConfig()
        val updated = current.copy(
            formatMode = mode.wire,
            shortsRatioPercent = shortsRatioPercent ?: current.shortsRatioPercent
        )
        persistConfig(updated)
    }

    fun updatePrimaryNiche(niche: String) {
        val updated = currentConfig().copy(primaryNiche = niche)
        persistConfig(updated)
    }

    fun swapActiveFunkTrack(track: FunkTrack, applyToQueued: Boolean = true) {
        val updated = currentConfig().copy(activeFunkTrackId = track.id)
        viewModelScope.launch {
            try {
                firestoreRepo.saveStudioConfig(updated)
                if (applyToQueued) {
                    val label = "${track.title} (${track.bpm} BPM)"
                    currentBatches()
                        .filter { it.status != "PUBLISHED" }
                        .forEach { batch ->
                            if (batch.id.isNotBlank()) {
                                firestoreRepo.swapBatchFunkTrack(batch.id, track.id, label)
                            }
                        }
                }
                _transient.update {
                    it.copy(statusBanner = "Active Funk Track swapped to ${track.title} (${track.bpm} BPM).")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update { it.copy(statusBanner = e.localizedMessage ?: "Failed to swap track.") }
            }
        }
    }

    private fun persistConfig(config: StudioConfig) {
        viewModelScope.launch {
            try {
                firestoreRepo.saveStudioConfig(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update { it.copy(statusBanner = e.localizedMessage ?: "Failed to save config.") }
            }
        }
    }

    fun generateDailyBatchFromAi() {
        val cfg = currentConfig()
        val track = StudioCatalog.funkTracks.firstOrNull { it.id == cfg.activeFunkTrackId }
            ?: StudioCatalog.funkTracks.first()
        viewModelScope.launch {
            _transient.update { it.copy(batchGenState = AsyncUiState.Loading) }
            try {
                val items = aiRepo.generateDailyBatch(
                    userId = currentUserId,
                    frequency = cfg.dailyVideoFrequency,
                    niche = cfg.primaryNiche,
                    contentMode = ContentTypeMode.fromWire(cfg.contentType),
                    formatMode = cfg.formatMode,
                    shortsRatioPercent = cfg.shortsRatioPercent,
                    funkTrack = track
                )
                items.forEach { firestoreRepo.createBatchItem(it) }
                _transient.update {
                    it.copy(
                        batchGenState = AsyncUiState.Success("Scheduled ${items.size} videos for today's queue."),
                        statusBanner = "${items.size} AI production packages added to schedule."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(batchGenState = AsyncUiState.Error(e.localizedMessage ?: "Batch generation failed."))
                }
            }
        }
    }

    fun markBatchStatus(item: ProductionBatchItem, newStatus: String) {
        if (item.id.isBlank()) return
        viewModelScope.launch {
            try {
                firestoreRepo.updateBatchStatus(item.id, newStatus, item.yppScore, item.estimatedRpmUsd)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update { it.copy(statusBanner = e.localizedMessage ?: "Status update failed.") }
            }
        }
    }

    fun deleteBatch(batchId: String) {
        if (batchId.isBlank()) return
        viewModelScope.launch {
            try {
                firestoreRepo.deleteBatch(batchId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update { it.copy(statusBanner = e.localizedMessage ?: "Delete failed.") }
            }
        }
    }

    fun generateQuickHooks(topic: String) {
        val cfg = currentConfig()
        viewModelScope.launch {
            _transient.update { it.copy(quickHookState = AsyncUiState.Loading) }
            try {
                val hooks = aiRepo.generateQuickHooks(topic, cfg.primaryNiche)
                _transient.update { it.copy(quickHookState = AsyncUiState.Success(hooks)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(quickHookState = AsyncUiState.Error(e.localizedMessage ?: "Hook generation failed."))
                }
            }
        }
    }

    fun generateAnimationPackage(topic: String, format: String) {
        val cfg = currentConfig()
        val track = StudioCatalog.funkTracks.firstOrNull { it.id == cfg.activeFunkTrackId }
            ?: StudioCatalog.funkTracks.first()
        val refBitmap = _transient.value.referenceBitmap
        viewModelScope.launch {
            _transient.update { it.copy(animationGenState = AsyncUiState.Loading) }
            try {
                val pkg = aiRepo.generateAnimationPackage(
                    topic = topic,
                    format = format,
                    contentMode = ContentTypeMode.fromWire(cfg.contentType),
                    funkTrack = track,
                    referenceBitmap = refBitmap
                )
                firestoreRepo.createBatchItem(
                    ProductionBatchItem(
                        userId = currentUserId,
                        title = pkg.seoTitle.ifBlank { topic },
                        hookScript = pkg.hookScript,
                        format = if (format == "16:9") "16:9" else "9:16",
                        contentMode = cfg.contentType,
                        scheduledSlotLabel = "Next Slot",
                        funkTrackId = track.id,
                        funkTrackTitle = "${track.title} (${track.bpm} BPM)",
                        memeCues = pkg.memeCues,
                        animationCode = pkg.manimCode,
                        seoDescription = "Generated via OmniTube Animation Engine",
                        seoTags = pkg.seoTags,
                        yppScore = 94,
                        estimatedRpmUsd = 4.9,
                        status = "READY"
                    )
                )
                _transient.update {
                    it.copy(
                        animationGenState = AsyncUiState.Success(pkg),
                        statusBanner = "Production package saved to queue."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(animationGenState = AsyncUiState.Error(e.localizedMessage ?: "Animation synthesis failed."))
                }
            }
        }
    }

    fun setSelectedAspect(option: StudioAspectOption) {
        _transient.update { it.copy(selectedAspect = option) }
    }

    fun setSelectedImageSize(option: StudioImageSizeOption) {
        _transient.update { it.copy(selectedImageSize = option) }
    }

    fun setUseProImageModel(pro: Boolean) {
        _transient.update { it.copy(useProImageModel = pro) }
    }

    fun setReferenceBitmap(bitmap: Bitmap?) {
        _transient.update { it.copy(referenceBitmap = bitmap) }
    }

    fun generateThumbnailImage(prompt: String) {
        val state = _transient.value
        viewModelScope.launch {
            _transient.update { it.copy(imageGenState = AsyncUiState.Loading) }
            try {
                val bmp = aiRepo.generateStudioImage(
                    prompt = prompt,
                    aspectRatio = state.selectedAspect.ratio,
                    imageSize = state.selectedImageSize.size,
                    studioQuality = state.useProImageModel,
                    referenceBitmap = state.referenceBitmap
                )
                _transient.update { it.copy(imageGenState = AsyncUiState.Success(bmp)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(imageGenState = AsyncUiState.Error(e.localizedMessage ?: "Image generation failed."))
                }
            }
        }
    }

    fun runGroundedTrendSearch(query: String, location: String) {
        viewModelScope.launch {
            _transient.update { it.copy(trendSearchState = AsyncUiState.Loading) }
            try {
                val result = aiRepo.researchTrendsGrounded(query, location)
                _transient.update { it.copy(trendSearchState = AsyncUiState.Success(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(trendSearchState = AsyncUiState.Error(e.localizedMessage ?: "Trend search failed."))
                }
            }
        }
    }

    fun auditConceptOrBatch(title: String, script: String) {
        val cfg = currentConfig()
        viewModelScope.launch {
            _transient.update { it.copy(auditActionState = AsyncUiState.Loading) }
            try {
                val record = aiRepo.auditScriptOrConcept(currentUserId, title, script, cfg.primaryNiche)
                firestoreRepo.saveAuditRecord(record)
                _transient.update { it.copy(auditActionState = AsyncUiState.Success(record)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(auditActionState = AsyncUiState.Error(e.localizedMessage ?: "YPP audit failed."))
                }
            }
        }
    }

    fun analyzePickedVideoBytes(title: String, bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _transient.update { it.copy(auditActionState = AsyncUiState.Loading) }
            try {
                val record = aiRepo.analyzeVideoForYpp(currentUserId, title, bytes, mimeType)
                firestoreRepo.saveAuditRecord(record)
                _transient.update { it.copy(auditActionState = AsyncUiState.Success(record)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(auditActionState = AsyncUiState.Error(e.localizedMessage ?: "Video analysis failed."))
                }
            }
        }
    }

    fun generateSeoBundle(topic: String) {
        val cfg = currentConfig()
        viewModelScope.launch {
            _transient.update { it.copy(seoGenState = AsyncUiState.Loading) }
            try {
                val seo = aiRepo.generateSeoAndEngagement(topic, cfg.primaryNiche)
                _transient.update { it.copy(seoGenState = AsyncUiState.Success(seo)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(seoGenState = AsyncUiState.Error(e.localizedMessage ?: "SEO generation failed."))
                }
            }
        }
    }

    fun speakScriptWithAiTts(scriptText: String) {
        viewModelScope.launch {
            _transient.update { it.copy(ttsState = AsyncUiState.Loading) }
            try {
                val audioBytes = aiRepo.synthesizeSpeechAudio(scriptText)
                _transient.update { it.copy(ttsState = AsyncUiState.Success("Playing AI voiceover (${audioBytes.size / 1024} KB)")) }
                audioEngine.playWavOrPcm(audioBytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(ttsState = AsyncUiState.Error(e.localizedMessage ?: "Voiceover generation failed."))
                }
            }
        }
    }

    fun startMicRecording() {
        _transient.update { it.copy(isRecordingMic = true, transcriptionState = AsyncUiState.Loading) }
        micRecorder.startRecording(viewModelScope) { err ->
            _transient.update {
                it.copy(isRecordingMic = false, transcriptionState = AsyncUiState.Error(err))
            }
        }
    }

    fun stopMicAndTranscribe(sendToVoiceChat: Boolean) {
        val wavBytes = micRecorder.stopAndBuildWav()
        _transient.update { it.copy(isRecordingMic = false) }
        if (wavBytes.isEmpty()) {
            _transient.update {
                it.copy(transcriptionState = AsyncUiState.Error("No microphone audio captured."))
            }
            return
        }
        viewModelScope.launch {
            _transient.update { it.copy(transcriptionState = AsyncUiState.Loading) }
            try {
                val transcript = aiRepo.transcribeAudioWav(wavBytes)
                _transient.update { it.copy(transcriptionState = AsyncUiState.Success(transcript)) }
                if (sendToVoiceChat) {
                    sendChatMessage(transcript, speakReply = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update {
                    it.copy(transcriptionState = AsyncUiState.Error(e.localizedMessage ?: "Transcription failed."))
                }
            }
        }
    }

    fun sendChatMessage(message: String, speakReply: Boolean = false) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _transient.value.isChatStreaming) return
        val userMsg = StudioChatMessage(id = UUID.randomUUID().toString(), isUser = true, text = trimmed)
        val replyId = UUID.randomUUID().toString()
        val initialReply = StudioChatMessage(id = replyId, isUser = false, text = "", isStreaming = true)

        _transient.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg + initialReply,
                isChatStreaming = true
            )
        }

        viewModelScope.launch {
            val sb = StringBuilder()
            try {
                aiRepo.sendChatMessageStream(trimmed).collect { chunk ->
                    sb.append(chunk)
                    val textNow = sb.toString()
                    _transient.update { state ->
                        state.copy(
                            chatMessages = state.chatMessages.map { msg ->
                                if (msg.id == replyId) msg.copy(text = textNow) else msg
                            }
                        )
                    }
                }
                val finalReply = sb.toString().ifBlank { "No response received." }
                _transient.update { state ->
                    state.copy(
                        isChatStreaming = false,
                        chatMessages = state.chatMessages.map { msg ->
                            if (msg.id == replyId) msg.copy(text = finalReply, isStreaming = false) else msg
                        }
                    )
                }
                if (speakReply) {
                    speakScriptWithAiTts(finalReply.take(400))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "Chat error."
                _transient.update { state ->
                    state.copy(
                        isChatStreaming = false,
                        chatMessages = state.chatMessages.map { msg ->
                            if (msg.id == replyId) msg.copy(text = "Error: $errMsg", isStreaming = false) else msg
                        }
                    )
                }
            }
        }
    }

    fun selectTutorialModule(index: Int) {
        val clamped = index.coerceIn(0, StudioCatalog.setupModules.lastIndex)
        _transient.update {
            it.copy(activeTutorialIndex = clamped, tutorialProgress = 0f, isTutorialPlaying = true)
        }
        startTutorialTicker()
    }

    fun toggleTutorialPlay() {
        val next = !_transient.value.isTutorialPlaying
        _transient.update { it.copy(isTutorialPlaying = next) }
        if (next) startTutorialTicker() else tutorialTickerJob?.cancel()
    }

    fun seekTutorial(progress: Float) {
        _transient.update { it.copy(tutorialProgress = progress.coerceIn(0f, 1f)) }
    }

    private fun startTutorialTicker() {
        tutorialTickerJob?.cancel()
        tutorialTickerJob = viewModelScope.launch {
            while (isActive && _transient.value.isTutorialPlaying) {
                delay(100L)
                _transient.update { st ->
                    val nextProg = st.tutorialProgress + 0.008f
                    if (nextProg >= 1f) {
                        st.copy(tutorialProgress = 0f, isTutorialPlaying = false)
                    } else {
                        st.copy(tutorialProgress = nextProg)
                    }
                }
            }
        }
    }

    fun saveYoutubeConnection(
        handle: String,
        channelId: String,
        clientId: String,
        scopes: List<String>,
        linked: Boolean
    ) {
        viewModelScope.launch {
            try {
                firestoreRepo.saveChannelConnection(
                    ChannelConnection(
                        userId = currentUserId,
                        channelHandle = handle,
                        channelId = channelId,
                        oauthClientId = clientId,
                        grantedScopes = scopes,
                        isLinked = linked
                    )
                )
                _transient.update {
                    it.copy(statusBanner = if (linked) "YouTube OAuth 2.0 configuration linked & saved." else "YouTube connection updated.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transient.update { it.copy(statusBanner = e.localizedMessage ?: "Failed to save channel link.") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopFunkPreview()
        micRecorder.stopAndDiscard()
        tutorialTickerJob?.cancel()
    }

    companion object {
        fun provideFactory(
            currentUserId: String,
            userDisplayName: String,
            userEmail: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StudioViewModel(
                    firestoreRepo = StudioFirestoreRepository(db),
                    aiRepo = FirebaseAiStudioRepository(),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail
                )
            }
        }
    }
}
