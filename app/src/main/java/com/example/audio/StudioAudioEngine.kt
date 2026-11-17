package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.data.model.FunkTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

private const val TAG = "StudioAudioEngine"
private const val SAMPLE_RATE = 22050

class StudioAudioEngine(private val scope: CoroutineScope) {

    private val _playingTrackId = MutableStateFlow<String?>(null)
    val playingTrackId: StateFlow<String?> = _playingTrackId.asStateFlow()

    private val _beatPulse = MutableStateFlow(0f)
    val beatPulse: StateFlow<Float> = _beatPulse.asStateFlow()

    private var loopJob: Job? = null
    private var activeAudioTrack: AudioTrack? = null

    fun toggleFunkPreview(track: FunkTrack) {
        if (_playingTrackId.value == track.id) {
            stopFunkPreview()
        } else {
            startFunkPreview(track)
        }
    }

    fun startFunkPreview(track: FunkTrack) {
        stopFunkPreview()
        _playingTrackId.value = track.id
        loopJob = scope.launch(Dispatchers.Default) {
            var audioTrack: AudioTrack? = null
            try {
                val patternSamples = synthesizeFunkBar(track)
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(SAMPLE_RATE / 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeAudioTrack = audioTrack
                audioTrack.play()

                val chunkSize = 2048
                var offset = 0
                val stepSamples = patternSamples.size / 8
                while (currentCoroutineContext().isActive) {
                    val count = minOf(chunkSize, patternSamples.size - offset)
                    val written = audioTrack.write(patternSamples, offset, count)
                    if (written < 0) break
                    val stepPhase = (offset % stepSamples).toFloat() / stepSamples.toFloat()
                    _beatPulse.value = (1f - stepPhase).coerceIn(0f, 1f)
                    offset = (offset + count) % patternSamples.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Funk preview stopped: ${e.message}")
            } finally {
                _beatPulse.value = 0f
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {
                }
                if (activeAudioTrack == audioTrack) {
                    activeAudioTrack = null
                }
            }
        }
    }

    fun stopFunkPreview() {
        _playingTrackId.value = null
        _beatPulse.value = 0f
        loopJob?.cancel()
        loopJob = null
        try {
            activeAudioTrack?.pause()
            activeAudioTrack?.flush()
        } catch (_: Exception) {
        }
    }

    fun playSfx(toneType: String) {
        scope.launch(Dispatchers.Default) {
            val pcm = synthesizeSfx(toneType)
            playPcmOnce(pcm, SAMPLE_RATE)
        }
    }

    suspend fun playWavOrPcm(bytes: ByteArray) = withContext(Dispatchers.Default) {
        if (bytes.size <= 44) return@withContext
        val isWav = bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

        val sampleRate = if (isWav) {
            ((bytes[24].toInt() and 0xFF)) or
                ((bytes[25].toInt() and 0xFF) shl 8) or
                ((bytes[26].toInt() and 0xFF) shl 16) or
                ((bytes[27].toInt() and 0xFF) shl 24)
        } else {
            24000
        }
        val offset = if (isWav) 44 else 0
        val shortCount = (bytes.size - offset) / 2
        val samples = ShortArray(shortCount)
        for (i in 0 until shortCount) {
            val lo = bytes[offset + i * 2].toInt() and 0xFF
            val hi = bytes[offset + i * 2 + 1].toInt()
            samples[i] = ((hi shl 8) or lo).toShort()
        }
        playPcmOnce(samples, sampleRate.coerceIn(8000, 48000))
    }

    private fun playPcmOnce(samples: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val byteSize = samples.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L / sampleRate).coerceAtLeast(150L)
            Thread.sleep(durationMs + 40L)
        } catch (e: Exception) {
            Log.w(TAG, "One-shot audio playback failed: ${e.message}")
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun synthesizeFunkBar(track: FunkTrack): ShortArray {
        val beatsPerBar = 4.0
        val barDurationSec = (60.0 / track.bpm) * beatsPerBar
        val totalSamples = (SAMPLE_RATE * barDurationSec).toInt()
        val out = ShortArray(totalSamples)
        val steps = track.bassPattern.size.coerceAtLeast(1)
        val stepLen = totalSamples / steps

        for (i in 0 until totalSamples) {
            val stepIdx = (i / stepLen).coerceAtMost(steps - 1)
            val localIdx = i % stepLen
            val localT = localIdx.toDouble() / SAMPLE_RATE.toDouble()
            val t = i.toDouble() / SAMPLE_RATE.toDouble()

            // Sub-808 bassline with phonk saturation
            val semitone = track.bassPattern[stepIdx]
            val freq = track.rootFreqHz * 2.0.pow(semitone / 12.0)
            val bassEnv = exp(-localT * 6.0)
            val bassWave = tanh(sin(2.0 * PI * freq * t) * 2.2) * 0.55 * bassEnv

            // Punchy kick on steps 0, 2, 4, 6
            val kick = if (stepIdx % 2 == 0) {
                val kickFreq = 130.0 * exp(-localT * 28.0) + 42.0
                sin(2.0 * PI * kickFreq * localT) * exp(-localT * 16.0) * 0.65
            } else 0.0

            // Phonk cowbell melody accent on odd steps
            val cowbell = if (stepIdx % 2 == 1 || stepIdx == 0) {
                val cbFreq = 540.0 * 2.0.pow((semitone % 7) / 12.0)
                val metallic = sin(2.0 * PI * cbFreq * localT) + 0.6 * sin(2.0 * PI * cbFreq * 1.48 * localT)
                metallic * exp(-localT * 18.0) * 0.22
            } else 0.0

            // Crisp hi-hat tick
            val hatEnv = exp(-localT * 45.0)
            val hatNoise = sin(2.0 * PI * 6800.0 * localT) * sin(2.0 * PI * 9100.0 * localT) * hatEnv * 0.14

            val mixed = (bassWave + kick + cowbell + hatNoise).coerceIn(-0.95, 0.95)
            out[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthesizeSfx(toneType: String): ShortArray {
        val durationSec = when (toneType) {
            "boom" -> 0.45
            "whoosh" -> 0.32
            "glitch" -> 0.22
            "freeze" -> 0.35
            else -> 0.28
        }
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val sample = when (toneType) {
                "boom" -> {
                    val f = 140.0 * exp(-t * 9.0) + 38.0
                    tanh(sin(2.0 * PI * f * t) * 2.6) * exp(-t * 4.5) * 0.85
                }
                "glitch" -> {
                    val f = if (((t * 40).toInt() % 2) == 0) 920.0 else 340.0
                    tanh(sin(2.0 * PI * f * t) * 3.0) * exp(-t * 8.0) * 0.55
                }
                "whoosh" -> {
                    val f = 220.0 + t * 1400.0
                    sin(2.0 * PI * f * t) * sin(PI * (i.toDouble() / count)) * 0.55
                }
                "freeze" -> {
                    val f = 620.0 * exp(-t * 6.5) + 90.0
                    sin(2.0 * PI * f * t) * exp(-t * 5.0) * 0.65
                }
                else -> {
                    val f1 = sin(2.0 * PI * 1046.5 * t)
                    val f2 = sin(2.0 * PI * 1567.98 * t)
                    (f1 * 0.5 + f2 * 0.4) * exp(-t * 9.0) * 0.65
                }
            }
            out[i] = (sample.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}

class MicRecorderHelper {
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private val pcmStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope, onError: (String) -> Unit) {
        stopAndDiscard()
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val rec = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                onError("Microphone hardware unavailable.")
                return
            }
            recorder = rec
            pcmStream.reset()
            rec.startRecording()

            recordingJob = scope.launch(Dispatchers.IO) {
                val buf = ByteArray(minBuf)
                while (isActive && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = rec.read(buf, 0, buf.size)
                    if (read > 0) {
                        synchronized(pcmStream) {
                            pcmStream.write(buf, 0, read)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to start microphone recording.")
        }
    }

    fun stopAndBuildWav(): ByteArray {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null

        val pcmData = synchronized(pcmStream) { pcmStream.toByteArray() }
        if (pcmData.isEmpty()) return ByteArray(0)
        return wrapPcmAsWav(pcmData, sampleRate = 16000, channels = 1, bitsPerSample = 16)
    }

    fun stopAndDiscard() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        synchronized(pcmStream) { pcmStream.reset() }
    }

    private fun wrapPcmAsWav(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcm.size + 36
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1)
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, (channels * bitsPerSample / 8).toShort())
        writeShortLE(header, 34, bitsPerSample.toShort())
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcm.size)
        return header + pcm
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Short) {
        buf[offset] = (value.toInt() and 0xFF).toByte()
        buf[offset + 1] = ((value.toInt() ushr 8) and 0xFF).toByte()
    }
}
