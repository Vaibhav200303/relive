package com.vaibhav.relive

import android.content.Context
import android.accounts.Account
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.vaibhav.relive.domain.backup.GoogleDriveAuthorizationUnavailableException
import com.vaibhav.relive.platform.backup.backupAuthLog
import com.vaibhav.relive.platform.backup.AndroidBackupPreferencesRepository
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first

/** Obtains short-lived Drive tokens without persisting them in Relive. */
interface AndroidDriveAccessTokenProvider {
    suspend fun accessToken(): String?
}

/**
 * Application-context authorization path used after a user has already granted
 * `drive.appdata`. It never launches a resolution UI, so it is safe for workers.
 */
class AndroidBackgroundDriveAccessTokenProvider(context: Context) : AndroidDriveAccessTokenProvider {
    private val appContext = context.applicationContext
    private val preferences = AndroidBackupPreferencesRepository(appContext)

    override suspend fun accessToken(): String? {
        val account = preferences.account.first()?.let { Account(it.email, "com.google") }
        return suspendCancellableCoroutine { continuation ->
        backupAuthLog("background Drive authorization requested")
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
        account?.let(request::setAccount)
        Identity.getAuthorizationClient(appContext).authorize(request.build()).addOnSuccessListener { result ->
            if (result.hasResolution()) {
                backupAuthLog("background Drive authorization requires interactive resolution")
                continuation.resumeWithException(
                    GoogleDriveAuthorizationUnavailableException("Google Drive authorization requires reconnecting your Google account."),
                )
            } else {
                backupAuthLog("background Drive authorization completed tokenPresent=${!result.accessToken.isNullOrBlank()}")
                continuation.resume(result.accessToken)
            }
        }.addOnFailureListener { error ->
            backupAuthLog("background Drive authorization failed type=${error::class.simpleName} message=${error.message}")
            continuation.resumeWithException(
                GoogleDriveAuthorizationUnavailableException("Google Drive authorization requires reconnecting your Google account."),
            )
        }
        }
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
