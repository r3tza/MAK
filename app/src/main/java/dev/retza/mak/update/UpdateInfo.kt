package dev.retza.mak.update

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val minSdk: Int,
    val notes: String = ""
)

sealed interface UpdateParseResult {
    data class Valid(val info: UpdateInfo) : UpdateParseResult
    data object Invalid : UpdateParseResult
}

sealed interface UpdateAvailability {
    data object UpToDate : UpdateAvailability
    data class Available(val info: UpdateInfo) : UpdateAvailability
    data class RequiresNewerAndroid(val minSdk: Int) : UpdateAvailability
}

private val updateJson = Json { ignoreUnknownKeys = true }
private val sha256Pattern = Regex("^[0-9a-fA-F]{64}$")

fun parseUpdateInfo(bytes: ByteArray): UpdateParseResult {
    val info = try {
        updateJson.decodeFromString<UpdateInfo>(bytes.decodeToString())
    } catch (_: SerializationException) {
        return UpdateParseResult.Invalid
    } catch (_: IllegalArgumentException) {
        return UpdateParseResult.Invalid
    }
    return if (info.isValid()) UpdateParseResult.Valid(info) else UpdateParseResult.Invalid
}

private fun UpdateInfo.isValid(): Boolean {
    if (versionCode < 1 || minSdk < 1 || versionName.isBlank()) return false
    if (!sha256Pattern.matches(sha256)) return false
    val uri = try {
        URI(apkUrl)
    } catch (_: IllegalArgumentException) {
        return false
    }
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("github.com", ignoreCase = true)
}

fun compareUpdate(
    info: UpdateInfo,
    installedVersionCode: Int,
    deviceSdk: Int
): UpdateAvailability = when {
    info.versionCode <= installedVersionCode -> UpdateAvailability.UpToDate
    info.minSdk > deviceSdk -> UpdateAvailability.RequiresNewerAndroid(info.minSdk)
    else -> UpdateAvailability.Available(info)
}
