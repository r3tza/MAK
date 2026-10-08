package dev.retza.mak.export

import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

object JsonExportCodec {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encode(snapshot: ExportSnapshot): ByteArray =
        json.encodeToString(ExportSnapshot.serializer(), snapshot)
            .toByteArray(StandardCharsets.UTF_8)

    fun decode(bytes: ByteArray): ExportSnapshot {
        val source = bytes.toString(StandardCharsets.UTF_8)
        val root = json.parseToJsonElement(source).jsonObject
        val version = root["schemaVersion"] as? JsonPrimitive
        require(version != null && !version.isString && version.intOrNull != null) {
            "Export is missing a valid schemaVersion."
        }
        require(root["studyPrograms"] is JsonArray) { "Export is missing studyPrograms." }
        require(root["semesters"] is JsonArray) { "Export is missing semesters." }
        return json.decodeFromJsonElement(ExportSnapshot.serializer(), root)
    }
}
