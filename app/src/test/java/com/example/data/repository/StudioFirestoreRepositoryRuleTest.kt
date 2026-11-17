package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChannelConnection
import com.example.data.model.ProductionBatchItem
import com.example.data.model.StudioConfig
import com.example.data.model.YppAuditRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudioFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureWorkspaceAndUpdateConfig_ownerSucceeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioFirestoreRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repository.ensureUserWorkspaceInitialized("Alice Studio", ALICE_EMAIL)
            repository.saveStudioConfig(
                StudioConfig(
                    userId = uid,
                    isMasterAutopilotOn = true,
                    dailyVideoFrequency = 8,
                    contentType = "AI_ANIMATION",
                    formatMode = "DUAL",
                    shortsRatioPercent = 75,
                    primaryNiche = "Viral Shorts & Funk Edits",
                    activeFunkTrackId = "funk_rio_voltage"
                )
            )
        }

        val config = withTimeout(TIMEOUT_MS) {
            repository.observeConfig(uid).first { it != null && it.dailyVideoFrequency == 8 }
        }
        assertNotNull(config)
        assertEquals("funk_rio_voltage", config?.activeFunkTrackId)
    }

    @Test
    fun batchLifecycleAndTerminalLock_enforcedByRules() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioFirestoreRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            repository.ensureUserWorkspaceInitialized("Alice Studio", ALICE_EMAIL)
        }

        val batchId = withTimeout(TIMEOUT_MS) {
            repository.createBatchItem(
                ProductionBatchItem(
                    userId = uid,
                    title = "Funk Retention Test ${UUID.randomUUID()}",
                    hookScript = "Watch the beat drop at 1.5s.",
                    format = "9:16",
                    contentMode = "HYBRID",
                    scheduledSlotLabel = "15:00 UTC",
                    funkTrackId = "funk_neon_drift",
                    funkTrackTitle = "Neon Drift Phonk (132 BPM)",
                    memeCues = listOf("0:01 Sub Boom"),
                    animationCode = "class DropScene(Scene): pass",
                    seoDescription = "Shorts batch test",
                    seoTags = listOf("shorts", "funk"),
                    yppScore = 95,
                    estimatedRpmUsd = 4.6,
                    status = "QUEUED"
                )
            )
        }
        assertTrue(batchId.isNotBlank())

        withTimeout(TIMEOUT_MS) {
            repository.swapBatchFunkTrack(batchId, "funk_tokyo_hyper", "Tokyo Hyper Synth (126 BPM)")
            repository.updateBatchStatus(batchId, "PUBLISHED", 97, 5.1)
        }

        // Terminal PUBLISHED lock must reject subsequent status mutations
        var failedWithPermissionDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                repository.updateBatchStatus(batchId, "QUEUED", 90, 4.0)
            }
        } catch (e: FirebaseFirestoreException) {
            failedWithPermissionDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED on terminal PUBLISHED batch", failedWithPermissionDenied)
    }

    @Test
    fun auditAndConnection_ownerWritesAndCrossUserDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioFirestoreRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            aliceRepo.ensureUserWorkspaceInitialized("Alice", ALICE_EMAIL)
            val auditId = aliceRepo.saveAuditRecord(
                YppAuditRecord(
                    userId = aliceUid,
                    targetTitle = "Audit ${UUID.randomUUID()}",
                    safetyScore = 94,
                    estimatedRpmUsd = 4.8,
                    reusedContentRisk = "LOW",
                    copyrightRisk = "LOW",
                    advertiserFriendly = true,
                    summary = "Safe original edit",
                    recommendations = listOf("Keep custom voiceover")
                )
            )
            assertTrue(auditId.isNotBlank())

            aliceRepo.saveChannelConnection(
                ChannelConnection(
                    userId = aliceUid,
                    channelHandle = "@alice_omni",
                    channelId = "UC_ALICE_99",
                    oauthClientId = "client-123.apps.googleusercontent.com",
                    isLinked = true
                )
            )
        }

        // Bob cannot read Alice's batches
        signInTestUser(BOB_EMAIL)
        var crossUserDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("batches").get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            crossUserDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED on cross-user read", crossUserDenied)
    }

    @Test
    fun observeBatches_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StudioFirestoreRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBatches("alice_unauth_test").first()
            }
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            denied = fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED when unauthenticated", denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_omni@test.com"
        const val BOB_EMAIL = "bob_omni@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
