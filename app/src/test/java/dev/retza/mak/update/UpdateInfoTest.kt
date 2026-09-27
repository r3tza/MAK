package dev.retza.mak.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateInfoTest {
    @Test
    fun parsesValidFileAndIgnoresUnknownFields() {
        val result = parseUpdateInfo(validJson(extra = ", \"future\": true").encodeToByteArray())

        assertTrue(result is UpdateParseResult.Valid)
        assertEquals("0.2.0", (result as UpdateParseResult.Valid).info.versionName)
    }

    @Test
    fun rejectsMalformedOrInvalidFields() {
        val invalidFiles = listOf(
            "not-json",
            validJson(versionCode = 0),
            validJson(minSdk = 0),
            validJson(versionName = " "),
            validJson(apkUrl = "http://github.com/r3tza/MAK/app.apk"),
            validJson(apkUrl = "https://github.com.evil.example/app.apk"),
            validJson(apkUrl = "https://objects.githubusercontent.com/app.apk"),
            validJson(sha256 = "abc")
        )

        invalidFiles.forEach { json ->
            assertEquals(UpdateParseResult.Invalid, parseUpdateInfo(json.encodeToByteArray()))
        }
    }

    @Test
    fun comparesVersionAndDeviceSdk() {
        val info = validInfo(versionCode = 200, minSdk = 31)

        assertEquals(UpdateAvailability.UpToDate, compareUpdate(info, 200, 36))
        assertEquals(UpdateAvailability.UpToDate, compareUpdate(info, 201, 36))
        assertEquals(UpdateAvailability.Available(info), compareUpdate(info, 199, 31))
        assertEquals(
            UpdateAvailability.RequiresNewerAndroid(31),
            compareUpdate(info, 199, 30)
        )
    }
}

internal fun validInfo(
    versionCode: Int = 200,
    versionName: String = "0.2.0",
    apkUrl: String = "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
    sha256: String = "a".repeat(64),
    minSdk: Int = 31,
    notes: String = "Zmiany"
) = UpdateInfo(versionCode, versionName, apkUrl, sha256, minSdk, notes)

private fun validJson(
    versionCode: Int = 200,
    versionName: String = "0.2.0",
    apkUrl: String = "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
    sha256: String = "a".repeat(64),
    minSdk: Int = 31,
    extra: String = ""
) = """
    {
      "versionCode": $versionCode,
      "versionName": "$versionName",
      "apkUrl": "$apkUrl",
      "sha256": "$sha256",
      "minSdk": $minSdk,
      "notes": "Zmiany"$extra
    }
""".trimIndent()
