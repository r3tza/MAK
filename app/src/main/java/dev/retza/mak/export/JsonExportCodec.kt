package dev.retza.mak.export

import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

object JsonExportCodec {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encode(snapshot: ExportSnapshot): ByteArray =
        json.encodeToString(ExportSnapshot.serializer(), snapshot)
            .toByteArray(StandardCharsets.UTF_8)

    fun decode(bytes: ByteArray): ExportSnapshot =
        json.decodeFromString(
            ExportSnapshot.serializer(),
            bytes.toString(StandardCharsets.UTF_8)
        )
}
