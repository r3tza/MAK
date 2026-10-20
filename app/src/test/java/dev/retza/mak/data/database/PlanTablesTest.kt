package dev.retza.mak.data.database

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanTablesTest {
    @Test
    fun planTablesCoverEveryTableOfTheLatestSchema() {
        val schemas = File("schemas/${AppDatabase::class.qualifiedName}").listFiles().orEmpty()
        val latest = requireNotNull(schemas.maxByOrNull { it.nameWithoutExtension.toInt() }) { "No exported Room schema" }
        val entities = Json.parseToJsonElement(latest.readText())
            .jsonObject.getValue("database").jsonObject.getValue("entities").jsonArray
        val tables = entities.map { it.jsonObject.getValue("tableName").jsonPrimitive.content }.toSet()

        assertEquals(tables, PLAN_TABLES.toSet())
    }
}
