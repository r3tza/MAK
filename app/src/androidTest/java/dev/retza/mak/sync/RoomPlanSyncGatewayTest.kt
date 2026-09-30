package dev.retza.mak.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.RoomPlanBackupGateway
import dev.retza.mak.data.repository.SemesterBackup
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The only synchronization code that replaces the whole database, checked on real Room. */
@RunWith(AndroidJUnit4::class)
class RoomPlanSyncGatewayTest {
    private lateinit var database: AppDatabase
    private lateinit var backup: RoomPlanBackupGateway
    private lateinit var gateway: RoomPlanSyncGateway

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        backup = RoomPlanBackupGateway(database)
        gateway = RoomPlanSyncGateway(database, backup)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun planChangedSinceTheReadIsNotReplaced() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 1))
        val seen = SyncPlanFile.fingerprint(gateway.snapshot())
        backup.replaceAll(plan("A", "B", "Edytowany", active = 1))

        val replaced = gateway.replaceIfUnchanged(seen, plan("Z Dysku"), keepLocalActive = true, fallbackActiveId = 1)

        assertFalse(replaced)
        assertEquals(listOf("A", "B", "Edytowany"), gateway.snapshot().semesters.map { it.semester.name })
    }

    @Test
    fun replacementKeepsLocalActiveSemesterOfTheSameLineage() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 2))

        val replaced = gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("A", "B", "C"), keepLocalActive = true, fallbackActiveId = 3
        )

        assertTrue(replaced)
        assertEquals(2L, gateway.snapshot().activeSemesterId)
    }

    @Test
    fun firstReplacementUsesTheFallbackSemester() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 1))

        gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("X", "Y", "Z"), keepLocalActive = false, fallbackActiveId = 3
        )

        assertEquals(3L, gateway.snapshot().activeSemesterId)
    }

    private fun plan(vararg names: String, active: Long? = null) = BackupData(
        studyPrograms = listOf(StudyProgramEntity(id = 1, name = "Informatyka", color = "#137B71")),
        semesters = names.mapIndexed { index, name ->
            val id = index + 1L
            SemesterBackup(
                semester = SemesterEntity(id = id, name = name, isActive = id == active),
                calendars = listOf(AcademicCalendarEntity(id, id, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31), WeekType.A)),
                programs = listOf(SemesterProgramEntity(id, id, 1, id)),
                classes = emptyList(),
                weekOverrides = emptyList(),
                occurrenceNotes = emptyList(),
                occurrenceChanges = emptyList()
            )
        }
    )
}
