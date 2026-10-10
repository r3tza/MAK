package dev.retza.mak.ui

import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import dev.retza.mak.ui.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivePlanSourceTest {
    @Test
    fun twoCollectorsShareOneQuery() = runTest {
        val repository = FakeRepository()
        // The shared stream runs on the test scheduler; backgroundScope tasks are not run by advanceUntilIdle.
        val sourceScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val source = activePlanSource(repository, InMemorySettingsPreferences(), sourceScope)

        val first = launch { source.observeActive().collect {} }
        val second = launch { source.observeActive().collect {} }
        advanceUntilIdle()

        assertEquals(1, repository.activePlanDataQueries)
        first.cancel()
        second.cancel()
        sourceScope.cancel()
    }

    @Test
    fun sameDataIsNotEmittedTwice() = runTest {
        val preferences = InMemorySettingsPreferences()
        val sourceScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val source = activePlanSource(FakeRepository(), preferences, sourceScope)
        val emissions = mutableListOf<Any?>()
        val collector = launch { source.observeActive().collect { emissions += it } }
        advanceUntilIdle()

        preferences.setTheme(ThemeMode.Dark)
        advanceUntilIdle()

        assertEquals(1, emissions.size)
        collector.cancel()
        sourceScope.cancel()
    }
}
