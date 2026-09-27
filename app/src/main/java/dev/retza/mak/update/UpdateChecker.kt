package dev.retza.mak.update

import java.io.IOException
import java.net.URL
import kotlinx.coroutines.CancellationException

const val UPDATE_JSON_URL = "https://github.com/r3tza/MAK/releases/latest/download/update.json"
const val MAX_UPDATE_JSON_BYTES = 64 * 1024

sealed interface UpdateCheckResult {
    data class Checked(val availability: UpdateAvailability) : UpdateCheckResult
    data object InvalidFile : UpdateCheckResult
    data object NetworkError : UpdateCheckResult
}

fun interface UpdateCheckService {
    suspend fun check(): UpdateCheckResult
}

class UpdateChecker(
    private val source: UpdateSource,
    private val installedVersionCode: Int,
    private val deviceSdk: Int
) : UpdateCheckService {
    override suspend fun check(): UpdateCheckResult {
        val bytes = try {
            source.fetch(MAX_UPDATE_JSON_BYTES)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return UpdateCheckResult.NetworkError
        }
        return when (val parsed = parseUpdateInfo(bytes)) {
            UpdateParseResult.Invalid -> UpdateCheckResult.InvalidFile
            is UpdateParseResult.Valid -> UpdateCheckResult.Checked(
                compareUpdate(parsed.info, installedVersionCode, deviceSdk)
            )
        }
    }

    companion object {
        fun production(installedVersionCode: Int, deviceSdk: Int): UpdateChecker = UpdateChecker(
            source = HttpUpdateSource(URL(UPDATE_JSON_URL)),
            installedVersionCode = installedVersionCode,
            deviceSdk = deviceSdk
        )
    }
}
