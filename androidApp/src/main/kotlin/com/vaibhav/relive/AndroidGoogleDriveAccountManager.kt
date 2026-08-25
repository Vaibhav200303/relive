package com.vaibhav.relive

import android.accounts.Account
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vaibhav.relive.domain.backup.GoogleDriveAccount
import com.vaibhav.relive.domain.backup.GoogleDriveAccountManager
import com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import android.util.Log
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import kotlinx.coroutines.flow.first

/** Activity-owned implementation: Credential Manager chooses identity, GIS grants Drive access. */
class AndroidGoogleDriveAccountManager(
    private val activity: ComponentActivity,
    private val preferences: AndroidBackupPreferencesRepository,
) : GoogleDriveAccountManager, AndroidDriveAccessTokenProvider {
    private val credentialManager = CredentialManager.create(activity)
    private var selectedAccount: Account? = null
    private var lastAccessToken: String? = null
    private var authorizationContinuation: ((Result<Unit>) -> Unit)? = null
    private val authorizationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val continuation = authorizationContinuation ?: return@registerForActivityResult
        authorizationContinuation = null
        try {
            val authorizationResult = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data)
            lastAccessToken = authorizationResult.accessToken
            continuation(Result.success(Unit))
        } catch (error: Exception) {
            continuation(Result.failure(error))
        }
    }

    override suspend fun connect(): GoogleDriveAccount? {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        Log.d("ReliveBackupAuth", "Android provider entered; implementation=${this::class.java.name}")
        Log.d("ReliveBackupAuth", "webClientId configured=${clientId.isNotBlank()}")
        if (clientId.isBlank()) throw GoogleDriveAuthorizationUnavailableException(
            "Google Drive is not configured. Add RELIVE_GOOGLE_WEB_CLIENT_ID and Android OAuth clients.",
        )
        Log.d("ReliveBackupAuth", "Credential Manager request launching")
        val credential = selectGoogleAccount(clientId) ?: run { Log.d("ReliveBackupAuth", "account selection cancelled or unavailable"); return null }
        Log.d("ReliveBackupAuth", "account result returned")
        val email = credential.id
        selectedAccount = Account(email, "com.google")
        Log.d("ReliveBackupAuth", "Drive authorization request launching")
        authorizeDrive()
        Log.d("ReliveBackupAuth", "Drive authorization result returned")
        return GoogleDriveAccount(subjectId = subjectFrom(credential.idToken) ?: email, email = email)
    }

    override suspend fun accessToken(): String? {
        authorizeDrive()
        return lastAccessToken
    }

    override suspend fun disconnect() {
        val account = selectedAccount ?: preferences.account.first()?.let { Account(it.email, "com.google") }
        account?.let { selected ->
            suspendCancellableCoroutine<Unit> { continuation ->
                Identity.getAuthorizationClient(activity).revokeAccess(
                    RevokeAccessRequest.builder()
                        .setAccount(selected)
                        .setScopes(listOf(Scope("https://www.googleapis.com/auth/drive.appdata")))
                        .build(),
                ).addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { continuation.resume(Unit) }
            }
        }
        selectedAccount = null
    }

    private suspend fun selectGoogleAccount(clientId: String): GoogleIdTokenCredential? {
        suspend fun request(authorizedOnly: Boolean): GoogleIdTokenCredential? = try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(authorizedOnly)
                .build()
            val result = credentialManager.getCredential(
                activity,
                GetCredentialRequest.Builder().addCredentialOption(option).build(),
            )
            GoogleIdTokenCredential.createFrom(result.credential.data)
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (_: NoCredentialException) {
            if (authorizedOnly) request(false) else null
        }
        return request(true)
    }

    private suspend fun authorizeDrive() = suspendCancellableCoroutine<Unit> { continuation ->
        Identity.getAuthorizationClient(activity).authorize(
            AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.appdata")))
                .build(),
        ).addOnSuccessListener { result ->
            if (result.hasResolution()) {
                authorizationContinuation = { outcome -> outcome.fold(continuation::resume, continuation::resumeWithException) }
                authorizationLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build())
            } else {
                lastAccessToken = result.accessToken
                continuation.resume(Unit)
            }
        }.addOnFailureListener { continuation.resumeWithException(it) }
    }

    private fun subjectFrom(idToken: String): String? = runCatching {
        val payload = idToken.split('.')[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        JSONObject(json).optString("sub").takeIf { it.isNotBlank() }
    }.getOrNull()
}
