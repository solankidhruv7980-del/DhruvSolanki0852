package com.example.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleAuthHelper"
private const val PLACEHOLDER_CLIENT_ID_PREFIX = "000000000000-placeholder"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("DiscouragedApi")
private fun resolveDefaultWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (resId == 0) return null
    return runCatching { context.getString(resId) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

private fun formatSignInFailureMessage(error: Exception): String {
    val detail = (error as? GetCredentialException)?.errorMessage?.toString()
        ?: error.localizedMessage
        ?: error.message
        ?: ""
    val lower = detail.lowercase()

    return when {
        error is NoCredentialException ->
            "No Google account credential was available for this app. Check that a Google account is signed in on this device, and that this APK is signed with the SHA-1 registered in Firebase."
        error is GetCredentialUnsupportedException ->
            "Google Credential Manager is unsupported or disabled on this device. Update Google Play Services and try again."
        lower.contains("developer_error") || lower.contains("developer console") || lower.contains("10:") ->
            "Google Sign-In configuration mismatch (SHA-1 or OAuth Web Client ID). Install the APK signed with the Firebase-registered debug keystore and real google-services.json."
        lower.contains("network") || lower.contains("7:") ->
            "Network error while contacting Google Sign-In. Check your internet connection and retry."
        detail.isNotBlank() ->
            "Google Sign-In failed: $detail"
        else ->
            "Google Sign-In failed (${error::class.java.simpleName}). Please try again."
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null || clientId.startsWith(PLACEHOLDER_CLIENT_ID_PREFIX)) {
        onUnauthenticated()
        return
    }

    val activityContext = context.findActivity() ?: context
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun launchInteractiveGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null || clientId.startsWith(PLACEHOLDER_CLIENT_ID_PREFIX)) {
        onAuthError(
            "Firebase OAuth config is missing in this build (placeholder google-services.json). " +
                "Rebuild with the real google-services.json and matching keystore, or install the APK from AI Studio."
        )
        return
    }

    val activityContext = context.findActivity()
    if (activityContext == null) {
        onAuthError("Unable to start Google Sign-In without an active screen.")
        return
    }

    scope.launch {
        try {
            // Primary request: single-option GetSignInWithGoogleOption (API 36 responsive bottom sheet)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = try {
                credentialManager.getCredential(activityContext, primaryRequest)
            } catch (e: NoCredentialException) {
                Log.w(TAG, "Primary GetSignInWithGoogleOption returned NoCredentialException; trying GetGoogleIdOption fallback", e)
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(activityContext, fallbackRequest)
            } catch (e: GetCredentialUnsupportedException) {
                Log.w(TAG, "Primary GetSignInWithGoogleOption unsupported; trying GetGoogleIdOption fallback", e)
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(activityContext, fallbackRequest)
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(formatSignInFailureMessage(e))
        }
    }
}

fun signOutFromStudio(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onComplete()
            }
        }
    }
}
