package dev.retza.mak.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.settings.SettingsPreferences
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class KoinGraphTest {
    @Test
    fun graphResolvesCoreDependencies() {
        val koin = GlobalContext.get()

        assertNotNull(koin.get<MakRepository>())
        assertNotNull(koin.get<AppDatabase>())
        assertNotNull(koin.get<FeedbackSink>())
        assertNotNull(koin.get<SettingsPreferences>())
    }
}
