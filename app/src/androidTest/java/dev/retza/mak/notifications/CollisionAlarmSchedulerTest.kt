package dev.retza.mak.notifications

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.RoomScheduleRepository
import dev.retza.mak.data.repository.RoomSemesterRepository
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.CollisionNotificationPlanner
import dev.retza.mak.domain.PlanDisplaySettings
import dev.retza.mak.ui.settings.CollisionNotificationPreferences
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.ui.settings.ThemeMode
import dev.retza.mak.ui.ActivePlanSource
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollisionAlarmSchedulerTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-21T06:00:00Z"), ZoneId.of("Europe/Warsaw"))
    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        // The app process refreshes its own alarms at start; a separate file keeps the ids apart.
        context = object : ContextWrapper(app) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("$name-scheduler-test", mode)
        }
        context.getSharedPreferences(ALARMS, Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
        context.getSharedPreferences(ALARMS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun parallelRefreshesStoreExactlyThePlannedAlarms() = runBlocking {
        val semesterId = seedCollidingClasses()
        val semesterRepository = RoomSemesterRepository(database)
        val scheduleRepository = RoomScheduleRepository(database)
        val planner = CollisionNotificationPlanner(ActivePlanProvider(), clock)
        val preferences = EnabledNotifications()
        val activePlanSource = ActivePlanSource(semesterRepository, scheduleRepository, preferences)
        val scheduler = CollisionAlarmScheduler(
            context,
            activePlanSource,
            planner,
            preferences,
            clock
        )

        try {
            listOf(
                async(Dispatchers.Default) { scheduler.refresh() },
                async(Dispatchers.Default) { scheduler.refresh() }
            ).awaitAll()

            val expected = planner.plan(
                activePlanSource.observe(semesterId).first()!!,
                preferences.collisionNotifications.first().toPlannerSettings()
            )
                .map { it.id.toString() }
                .toSet()
            val stored = context.getSharedPreferences(ALARMS, Context.MODE_PRIVATE)
                .getStringSet(IDS, emptySet())
            assertTrue(expected.isNotEmpty())
            assertEquals(expected, stored)
        } finally {
            scheduler.cancelAll()
        }
    }

    private suspend fun seedCollidingClasses(): Long {
        val semesterId = database.semesterDao().insert(SemesterEntity(name = "Semestr", isActive = true))
        val calendarId = database.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = semesterId,
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2027, 1, 31),
                firstWeekType = WeekType.A
            )
        )
        val programId = database.studyProgramDao().insert(StudyProgramEntity(name = "Informatyka", color = "#137B71"))
        val assignmentId = database.semesterProgramDao().insert(
            SemesterProgramEntity(semesterId = semesterId, studyProgramId = programId, academicCalendarId = calendarId)
        )
        listOf(LocalTime.of(10, 0) to LocalTime.of(11, 30), LocalTime.of(11, 0) to LocalTime.of(12, 0))
            .forEachIndexed { index, (start, end) ->
                database.classDao().insert(
                    ClassEntity(
                        semesterId = semesterId,
                        semesterProgramId = assignmentId,
                        name = "Zajęcia $index",
                        type = "Wykład",
                        teacherName = null,
                        dayOfWeek = DayOfWeek.TUESDAY,
                        startTime = start,
                        endTime = end,
                        room = null,
                        building = null,
                        group = null,
                        recurrence = Recurrence.EVERY_WEEK,
                        date = null,
                        classNote = null
                    )
                )
            }
        return semesterId
    }

    private class EnabledNotifications : SettingsPreferences {
        override val theme: Flow<ThemeMode> = flowOf(ThemeMode.System)
        override val collisionNotifications: Flow<CollisionNotificationPreferences> =
            flowOf(CollisionNotificationPreferences(enabled = true))
        override val gapThresholdMinutes: Flow<Int> = flowOf(30)
        override val planDisplay: Flow<PlanDisplaySettings> = flowOf(PlanDisplaySettings.DEFAULT)
        override suspend fun setTheme(mode: ThemeMode) = Unit
        override suspend fun setGapThresholdMinutes(minutes: Int) = Unit
        override suspend fun setMinimumBreakMinutes(minutes: Int) = Unit
        override suspend fun setCollisionNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setEveningNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setEveningHour(hour: LocalTime) = Unit
        override suspend fun setBeforeClassLeadMinutes(minutes: Long) = Unit
    }

    private companion object {
        const val ALARMS = "collision_alarms"
        const val IDS = "scheduled_ids"
    }
}
