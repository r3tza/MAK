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
        val releases = listOf(
            ReleaseHistoryEntry("0.2.0", "2026-09-27", listOf("Nowość")),
            ReleaseHistoryEntry("0.1.0", "2026-09-19", listOf("Start", "Plan", "Notatki"))
        )
        val viewModel = testUpdateViewModel(
            UpdateCheckService { UpdateCheckResult.NetworkError },
            appInfo("0.1.0"),
            releaseNotes = ReleaseNotesProvider { releases }
        )

        assertEquals("0.1.0", viewModel.state.value.installedVersionName)
        assertEquals(listOf("0.1.0"), viewModel.state.value.releaseHistory.map { it.versionName })
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
        val viewModel = testUpdateViewModel(
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
    ) = testUpdateViewModel(UpdateCheckService { result }, appInfo(versionName))
}

private fun testUpdateViewModel(
    checker: UpdateCheckService,
    appInfoProvider: InstalledAppInfoProvider,
    releaseNotes: ReleaseNotesProvider = ReleaseNotesProvider { emptyList() }
) = UpdateViewModel(
    checker = checker,
    appInfoProvider = appInfoProvider,
    downloader = ApkDownloader { _, _ -> ApkDownloadResult.NetworkError },
    verifier = ApkVerifier { _, _ -> ApkVerificationResult.Corrupted },
    installer = object : UpdateInstaller {
        override fun canInstallPackages() = false
        override fun permissionIntent() = android.content.Intent()
        override fun install(file: java.io.File) = Unit
    },
    installEvents = InstallEventStore(),
    preferences = null,
    clock = java.time.Clock.systemUTC(),
    releaseNotes = releaseNotes
)

private fun appInfo(versionName: String) = InstalledAppInfoProvider {
    InstalledAppInfo(versionName, 100, "dev.retza.mak", 36)
}
