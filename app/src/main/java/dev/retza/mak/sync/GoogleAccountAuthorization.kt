package dev.retza.mak.sync

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.koin.core.annotation.Single

sealed interface AuthorizationAttempt {
    data class Granted(val account: SyncAccount) : AuthorizationAttempt
    data class NeedsResolution(val pendingIntent: PendingIntent) : AuthorizationAttempt
}

/** Consent expired or lacks a permission; the screen asks for it again with „Połącz ponownie”. */
class UserActionRequiredException : IllegalStateException("Google authorization needs user action")

/** Google answered for a different account than the connected one; a new consent cannot fix that. */
class AccountMismatchException : IllegalStateException("Google account changed; disconnect and connect again")

/** The consent steps the synchronization screen drives; separate from Google Identity for tests. */
interface SyncAuthorization {
    suspend fun beginAccountSelection(): AuthorizationAttempt
    suspend fun refreshPinnedAccount(account: SyncAccount): AuthorizationAttempt
    suspend fun completeAccountSelection(resultIntent: Intent): AuthorizationAttempt
    suspend fun completePinnedAuthorization(resultIntent: Intent, account: SyncAccount): AuthorizationAttempt
}

@Single(binds = [DriveAccessTokenProvider::class, SyncAuthorization::class])
class GoogleAccountAuthorization(
    context: Context,
    private val http: DriveHttpClient,
    private val backgroundDispatcher: CoroutineDispatcher
) : DriveAccessTokenProvider, SyncAuthorization {
    private val client = Identity.getAuthorizationClient(context.applicationContext)

    override suspend fun beginAccountSelection(): AuthorizationAttempt = withContext(backgroundDispatcher) {
        verify(authorize(accountName = null), pinned = null).toAttempt()
    }

    override suspend fun refreshPinnedAccount(account: SyncAccount): AuthorizationAttempt = withContext(backgroundDispatcher) {
        verify(authorize(account.androidAccountName), pinned = account).toAttempt()
    }

    override suspend fun completeAccountSelection(resultIntent: Intent): AuthorizationAttempt = withContext(backgroundDispatcher) {
        verify(authorizationResultFromIntent(resultIntent), pinned = null).toAttempt()
    }

    override suspend fun completePinnedAuthorization(resultIntent: Intent, account: SyncAccount): AuthorizationAttempt =
        withContext(backgroundDispatcher) {
            verify(authorizationResultFromIntent(resultIntent), pinned = account).toAttempt()
        }

    /** Background runs cannot show consent, so any needed resolution asks the user in the app. */
    override suspend fun tokenFor(account: SyncAccount): String = withContext(backgroundDispatcher) {
        when (val verified = verify(authorize(account.androidAccountName), pinned = account)) {
            is Verification.Resolution -> throw UserActionRequiredException()
            is Verification.Verified -> verified.token
        }
    }

    override suspend fun invalidate(token: String) = withContext(backgroundDispatcher) { clearToken(token) }

    /**
     * Checks consent, permissions, the token and the account behind it. The token alone does not name the
     * account, so userinfo binds it to `sub`. A rejected token is cleared and asked for once more.
     */
    private suspend fun verify(first: AuthorizationResult, pinned: SyncAccount?): Verification {
        var result = first
        repeat(2) { attempt ->
            if (result.hasResolution()) return Verification.Resolution(requireNotNull(result.pendingIntent))
            if (!hasRequiredScopes(result.grantedScopes)) throw UserActionRequiredException()
            val token = result.accessToken?.takeIf(String::isNotBlank) ?: throw UserActionRequiredException()
            val response = http.execute(USERINFO_URL, "GET", mapOf("Authorization" to "Bearer $token"), null)
            if (response.status == 401) {
                clearToken(token)
                if (attempt == 1) throw UserActionRequiredException()
                result = authorize(pinned?.androidAccountName)
                return@repeat
            }
            if (response.status !in 200..299) throw DriveHttpException(response.status, "Google identity verification failed")
            val account = accountFrom(response.body)
            if (pinned != null && account.subject != pinned.subject) throw AccountMismatchException()
            return Verification.Verified(account, token)
        }
        error("Google authorization retry was exhausted")
    }

    private fun accountFrom(body: ByteArray): SyncAccount {
        val root = try {
            Json.parseToJsonElement(body.decodeToString()).jsonObject
        } catch (_: IllegalArgumentException) {
            throw DriveHttpException(0, "Google identity response is unreadable")
        }
        val subject = root["sub"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
            ?: throw DriveHttpException(0, "Google identity response has no subject")
        val email = root["email"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
            ?: throw DriveHttpException(0, "Google identity response has no email")
        check(root["email_verified"]?.jsonPrimitive?.content == "true") { "Google account email is not verified" }
        // The email is the label and the Android account name; `sub` is the identity.
        return SyncAccount(subject, email, email)
    }

    private fun hasRequiredScopes(granted: Collection<String>): Boolean {
        val normalized = granted.map { scope ->
            when (scope.lowercase()) {
                "openid", "https://www.googleapis.com/auth/openid" -> "openid"
                "email", "https://www.googleapis.com/auth/userinfo.email" -> "email"
                else -> scope
            }
        }.toSet()
        return DRIVE_APPDATA in normalized && "openid" in normalized && "email" in normalized
    }

    /** Without [accountName] Google shows the account picker. */
    private suspend fun authorize(accountName: String?): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA), Scope("openid"), Scope("email")))
            .setOptOutIncludingGrantedScopes(true)
            .also { builder ->
                if (accountName == null) builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                else builder.setAccount(Account(accountName, "com.google"))
            }
            .build()
        return suspendCancellableCoroutine { continuation ->
            try {
                client.authorize(request)
                    .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(mapGoogleAuthorizationException(error))
                    }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(mapGoogleAuthorizationException(error))
            }
        }
    }

    private suspend fun clearToken(token: String) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                client.clearToken(ClearTokenRequest.builder().setToken(token).build())
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(mapGoogleAuthorizationException(error))
                    }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(mapGoogleAuthorizationException(error))
            }
        }
    }

    private fun authorizationResultFromIntent(intent: Intent): AuthorizationResult = try {
        client.getAuthorizationResultFromIntent(intent)
    } catch (error: Exception) {
        throw mapGoogleAuthorizationException(error)
    }

    private sealed interface Verification {
        data class Resolution(val pendingIntent: PendingIntent) : Verification
        data class Verified(val account: SyncAccount, val token: String) : Verification
    }

    private fun Verification.toAttempt(): AuthorizationAttempt = when (this) {
        is Verification.Resolution -> AuthorizationAttempt.NeedsResolution(pendingIntent)
        is Verification.Verified -> AuthorizationAttempt.Granted(account)
    }

    private companion object {
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo"
    }
}

internal fun mapGoogleAuthorizationException(error: Exception): Exception {
    if (error is CancellationException) return error
    val status = (error as? ApiException)?.statusCode ?: return error
    return when (status) {
        CommonStatusCodes.NETWORK_ERROR,
        CommonStatusCodes.INTERNAL_ERROR,
        CommonStatusCodes.TIMEOUT,
        CommonStatusCodes.CONNECTION_SUSPENDED_DURING_CALL -> IOException("Temporary Google authorization failure", error)
        CommonStatusCodes.CANCELED -> CancellationException("Google authorization was cancelled").also { it.initCause(error) }
        else -> error
    }
}
