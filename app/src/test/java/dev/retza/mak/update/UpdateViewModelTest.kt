package dev.retza.mak.update

import dev.retza.mak.ui.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    @Test
    fun exposesInstalledVersionAndKnownHistory() {
        val viewModel = viewModel(UpdateCheckResult.NetworkError, versionName = "0.1.0")

        assertEquals("0.1.0", viewModel.state.value.installedVersionName)
        assertEquals(1, viewModel.state.value.releaseHistory.size)
        assertEquals(3, viewModel.state.value.releaseHistory.first().changes.size)
    }

    @Test
    fun mapsAvailableAndErrorResults() = runTest {
        val info = validInfo()
        val available = viewModel(UpdateCheckResult.Checked(UpdateAvailability.Available(info)))
        available.checkNow()
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.Available, available.state.value.checkStatus)
        assertEquals(info, available.state.value.availableUpdate)

        val failed = viewModel(UpdateCheckResult.NetworkError)
        failed.checkNow()
        advanceUntilIdle()

        assertEquals(UpdateCheckStatus.NetworkError, failed.state.value.checkStatus)
        assertNull(failed.state.value.availableUpdate)
    }

    @Test
    fun ignoresSecondCheckWhileFirstIsRunning() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val viewModel = UpdateViewModel(
            checker = UpdateCheckService {
                calls += 1
                gate.await()
                UpdateCheckResult.Checked(UpdateAvailability.UpToDate)
            },
            appInfoProvider = appInfo("0.1.0")
        )

        viewModel.checkNow()
        viewModel.checkNow()
        assertEquals(1, calls)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(UpdateCheckStatus.UpToDate, viewModel.state.value.checkStatus)
    }

    private fun viewModel(
        result: UpdateCheckResult,
        versionName: String = "0.1.0"
    ) = UpdateViewModel(UpdateCheckService { result }, appInfo(versionName))
}

private fun appInfo(versionName: String) = InstalledAppInfoProvider {
    InstalledAppInfo(versionName, 100, "dev.retza.mak", 36)
}
