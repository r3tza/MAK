package dev.retza.mak.di

import dev.retza.mak.data.database.AppDatabase
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

class BackupRulesTest {
    private val rules = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(File("src/main/res/xml/data_extraction_rules.xml"))
        .documentElement

    @Test
    fun backupAllowsOnlyPlanDatabaseAndSettings() {
        val expected = setOf(
            "database" to AppDatabase.DATABASE_NAME,
            "database" to "${AppDatabase.DATABASE_NAME}-wal",
            "database" to "${AppDatabase.DATABASE_NAME}-shm",
            "file" to "datastore/$SETTINGS_DATASTORE_NAME.preferences_pb"
        )

        listOf("cloud-backup", "device-transfer").forEach { section ->
            assertEquals(section, expected, includes(section))
            assertEquals("$section has no exclude rules", 0, section(section).getElementsByTagName("exclude").length)
        }
    }

    @Test
    fun cloudBackupRequiresEndToEndEncryption() {
        assertEquals("true", section("cloud-backup").getAttribute("disableIfNoEncryptionCapabilities"))
    }

    private fun section(name: String): Element = rules.getElementsByTagName(name).item(0) as Element

    private fun includes(section: String): Set<Pair<String, String>> {
        val nodes = section(section).getElementsByTagName("include")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .map { it.getAttribute("domain") to it.getAttribute("path") }
            .toSet()
    }
}
