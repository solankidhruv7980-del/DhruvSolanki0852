package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChannelConnection
import com.example.data.model.ContentTypeMode
import com.example.data.model.FormatMode
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioConfig
import com.example.data.model.StudioUserProfile
import com.example.data.model.YppAuditRecord
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireActiveUid(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")
    }

    fun observeConfig(userId: String): Flow<StudioConfig?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("studio_config").document(MAIN_CONFIG_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        StudioConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (Firebase.auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeBatches(userId: String): Flow<List<ProductionBatchItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("batches")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProductionBatchItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (Firebase.auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAudits(userId: String): Flow<List<YppAuditRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("audits")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        YppAuditRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (Firebase.auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeChannelConnection(userId: String): Flow<ChannelConnection?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("channel_connection").document(YOUTUBE_CONN_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        ChannelConnection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (Firebase.auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserWorkspaceInitialized(
        displayName: String,
        email: String
    ) {
        val uid = requireActiveUid()
        val userRef = db.collection("users").document(uid)
        try {
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val safeName = displayName.trim().ifBlank { "Creator" }.take(80)
                val profilePayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    if (email.isNotBlank()) put("email", email.trim().take(120))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userRef.set(profilePayload).await()
            }

            val configRef = userRef.collection("studio_config").document(MAIN_CONFIG_ID)
            if (!configRef.get().await().exists()) {
                val defaultConfig = mapOf(
                    "userId" to uid,
                    "isMasterAutopilotOn" to true,
                    "dailyVideoFrequency" to 5,
                    "contentType" to ContentTypeMode.HYBRID.wire,
                    "formatMode" to FormatMode.DUAL.wire,
                    "shortsRatioPercent" to 80,
                    "primaryNiche" to "Viral Shorts & Funk Edits / Multi-Niche",
                    "activeFunkTrackId" to "funk_neon_drift",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                configRef.set(defaultConfig).await()
            }

            val connRef = userRef.collection("channel_connection").document(YOUTUBE_CONN_ID)
            if (!connRef.get().await().exists()) {
                val defaultConn = mapOf(
                    "userId" to uid,
                    "channelHandle" to "",
                    "channelId" to "",
                    "oauthClientId" to "",
                    "grantedScopes" to listOf(
                        "https://www.googleapis.com/auth/youtube.upload",
                        "https://www.googleapis.com/auth/youtube.readonly",
                        "https://www.googleapis.com/auth/youtube.force-ssl"
                    ),
                    "isLinked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                connRef.set(defaultConn).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }
    }

    suspend fun saveStudioConfig(config: StudioConfig) {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid)
            .collection("studio_config").document(MAIN_CONFIG_ID)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "isMasterAutopilotOn" to config.isMasterAutopilotOn,
                    "dailyVideoFrequency" to config.dailyVideoFrequency.coerceIn(1, 20),
                    "contentType" to config.contentType,
                    "formatMode" to config.formatMode,
                    "shortsRatioPercent" to config.shortsRatioPercent.coerceIn(0, 100),
                    "primaryNiche" to config.primaryNiche.ifBlank { "Viral Shorts & Funk Edits" }.take(120),
                    "activeFunkTrackId" to config.activeFunkTrackId.ifBlank { "funk_neon_drift" }.take(64),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "isMasterAutopilotOn" to config.isMasterAutopilotOn,
                    "dailyVideoFrequency" to config.dailyVideoFrequency.coerceIn(1, 20),
                    "contentType" to config.contentType,
                    "formatMode" to config.formatMode,
                    "shortsRatioPercent" to config.shortsRatioPercent.coerceIn(0, 100),
                    "primaryNiche" to config.primaryNiche.ifBlank { "Viral Shorts & Funk Edits" }.take(120),
                    "activeFunkTrackId" to config.activeFunkTrackId.ifBlank { "funk_neon_drift" }.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun createBatchItem(item: ProductionBatchItem): String {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid).collection("batches").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to item.title.ifBlank { "Untitled Production" }.take(160),
            "hookScript" to item.hookScript.ifBlank { "Watch until the drop." }.take(4000),
            "format" to if (item.format == "16:9") "16:9" else "9:16",
            "contentMode" to item.contentMode,
            "scheduledSlotLabel" to item.scheduledSlotLabel.ifBlank { "12:00 UTC" }.take(40),
            "funkTrackId" to item.funkTrackId.ifBlank { "funk_neon_drift" }.take(64),
            "funkTrackTitle" to item.funkTrackTitle.ifBlank { "Neon Drift Phonk" }.take(100),
            "memeCues" to item.memeCues.take(12),
            "animationCode" to item.animationCode.take(12000),
            "seoDescription" to item.seoDescription.take(3000),
            "seoTags" to item.seoTags.take(15),
            "yppScore" to item.yppScore.coerceIn(0, 100),
            "estimatedRpmUsd" to item.estimatedRpmUsd.coerceIn(0.0, 500.0),
            "status" to item.status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun swapBatchFunkTrack(
        batchId: String,
        funkTrackId: String,
        funkTrackTitle: String
    ) {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid).collection("batches").document(batchId)
        try {
            docRef.update(
                mapOf(
                    "funkTrackId" to funkTrackId.take(64),
                    "funkTrackTitle" to funkTrackTitle.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBatchStatus(
        batchId: String,
        status: String,
        yppScore: Int,
        estimatedRpmUsd: Double
    ) {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid).collection("batches").document(batchId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "yppScore" to yppScore.coerceIn(0, 100),
                    "estimatedRpmUsd" to estimatedRpmUsd.coerceIn(0.0, 500.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBatch(batchId: String) {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid).collection("batches").document(batchId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun saveAuditRecord(record: YppAuditRecord): String {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid).collection("audits").document()
        val payload = mapOf(
            "userId" to uid,
            "targetTitle" to record.targetTitle.ifBlank { "Untitled Audit" }.take(200),
            "safetyScore" to record.safetyScore.coerceIn(0, 100),
            "estimatedRpmUsd" to record.estimatedRpmUsd.coerceIn(0.0, 500.0),
            "reusedContentRisk" to record.reusedContentRisk,
            "copyrightRisk" to record.copyrightRisk,
            "advertiserFriendly" to record.advertiserFriendly,
            "summary" to record.summary.ifBlank { "Audit complete." }.take(2000),
            "recommendations" to record.recommendations.take(10),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun saveChannelConnection(connection: ChannelConnection) {
        val uid = requireActiveUid()
        val docRef = db.collection("users").document(uid)
            .collection("channel_connection").document(YOUTUBE_CONN_ID)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "channelHandle" to connection.channelHandle.trim().take(80),
                    "channelId" to connection.channelId.trim().take(80),
                    "oauthClientId" to connection.oauthClientId.trim().take(180),
                    "grantedScopes" to connection.grantedScopes.take(10),
                    "isLinked" to connection.isLinked,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "channelHandle" to connection.channelHandle.trim().take(80),
                    "channelId" to connection.channelId.trim().take(80),
                    "oauthClientId" to connection.oauthClientId.trim().take(180),
                    "grantedScopes" to connection.grantedScopes.take(10),
                    "isLinked" to connection.isLinked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    companion object {
        const val MAIN_CONFIG_ID = "main"
        const val YOUTUBE_CONN_ID = "youtube"
    }
}
