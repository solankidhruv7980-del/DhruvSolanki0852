package com.example.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
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
import java.security.MessageDigest

private const val TAG = "GoogleAuthHelper"
private const val PLACEHOLDER_CLIENT_ID_PREFIX = "000000000000-placeholder"
private const val EXPECTED_FIREBASE_SHA1 = "D2511D569768E46959586CB96E049FB83DFD1A76"

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

@Suppress("DEPRECATION")
private fun getAppSigningSha1Hex(context: Context): String? {
    return runCatching {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            pkgInfo.signingInfo?.apkContentsSigners
        } else {
            val pkgInfo = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            pkgInfo.signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString("") { "%02X".format(it) }
    }.getOrNull()
}

private fun formatSignInFailureMessage(context: Context, error: Exception): String {
    val detail = (error as? GetCredentialException)?.errorMessage?.toString()
        ?: error.localizedMessage
        ?: error.message
        ?: ""
    val lower = detail.lowercase()
    val actualSha1 = getAppSigningSha1Hex(context)
    val sha1Mismatch = actualSha1 != null && !actualSha1.equals(EXPECTED_FIREBASE_SHA1, ignoreCase = true)

    return when {
        sha1Mismatch ->
            "This APK's signing certificate SHA-1 ($actualSha1) does not match Firebase ($EXPECTED_FIREBASE_SHA1). " +
                "If built on GitHub Actions, add DEBUG_KEYSTORE_BASE64 and GOOGLE_SERVICES_JSON secrets, or install the APK built in AI Studio."
        error is NoCredentialException ->
            "No Google account credential returned. If you just added a Google account, tap 'Sign in with Google' once more, or verify Play Services is updated."
        error is GetCredentialUnsupportedException ->
            "Google Credential Manager is unsupported or disabled on this device. Update Google Play Services and try again."
        lower.contains("developer_error") || lower.contains("developer console") || lower.contains("10:") ->
            "Google Sign-In configuration mismatch (SHA-1 or OAuth Web Client ID). Check google-services.json and keystore SHA-1."
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
    onAuthCancelled: (String?) -> Unit,
    onAuthError: (String) -> Unit
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }

    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null || clientId.startsWith(PLACEHOLDER_CLIENT_ID_PREFIX)) {
        onAuthError(
            "Firebase OAuth config is missing in this APK (placeholder google-services.json). " +
                "Add GOOGLE_SERVICES_JSON and DEBUG_KEYSTORE_BASE64 secrets in GitHub, or install the APK built directly from AI Studio."
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
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
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
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
            onAuthCancelled(detail)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(formatSignInFailureMessage(context, e))
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
