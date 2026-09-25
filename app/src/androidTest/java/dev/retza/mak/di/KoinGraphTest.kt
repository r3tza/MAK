package dev.retza.mak.di

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.PlanBackupGateway
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.export.PlanBackupService
import dev.retza.mak.ui.AppViewModel
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.today.TodayViewModel
import java.time.Clock
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@RunWith(AndroidJUnit4::class)
class KoinGraphTest {
    @Test
    fun graphResolvesDependenciesAndViewModels() {
        val koin = GlobalContext.get()

        assertNotNull(koin.get<ScheduleRepository>())
        assertNotNull(koin.get<SemesterRepository>())
        assertNotNull(koin.get<PlanBackupGateway>())
        assertNotNull(koin.get<PlanBackupService>())
        assertNotNull(koin.get<AppDatabase>())
        assertNotNull(koin.get<FeedbackSink>())
        assertNotNull(koin.get<SettingsPreferences>())
        assertNotNull(koin.get<Clock>())

        assertNotNull(koin.get<AppViewModel>())
        assertNotNull(koin.get<OccurrenceViewModel>())
        // Hosts get SavedStateHandle from viewModel(); resolving directly must pass it in.
        assertNotNull(koin.get<ClassEditViewModel> { parametersOf(SavedStateHandle()) })
        assertNotNull(koin.get<SemesterViewModel>())
        assertNotNull(koin.get<SetupViewModel>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<ScheduleViewModel>())
        assertNotNull(koin.get<TodayViewModel>())
    }
}
