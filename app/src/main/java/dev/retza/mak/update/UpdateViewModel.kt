package dev.retza.mak.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.KoinViewModel

enum class UpdateCheckStatus {
    Idle,
    Checking,
    UpToDate,
    Available,
    RequiresNewerAndroid,
    InvalidFile,
    NetworkError
}

enum class UpdateDownloadStatus {
    Idle, Downloading, Ready, NeedsPermission, Installing, NotEnoughSpace, NetworkError,
    Corrupted, WrongPackage, NotNewer, WrongSignature, InstallCancelled, InstallFailed
}

data class UpdateUiState(
    val installedVersionName: String,
    val installedVersionCode: Long,
    val checkStatus: UpdateCheckStatus = UpdateCheckStatus.Idle,
    val availableUpdate: UpdateInfo? = null,
    val requiredMinSdk: Int? = null,
    val releaseHistory: List<ReleaseHistoryEntry> = emptyList(),
    val downloadStatus: UpdateDownloadStatus = UpdateDownloadStatus.Idle,
    val downloadedBytes: Long = 0,
    val downloadTotalBytes: Long? = null,
    val automaticChecks: Boolean = false,
    val showUpdateBanner: Boolean = false
)

@KoinViewModel
class UpdateViewModel(
    private val checker: UpdateCheckService,
    appInfoProvider: InstalledAppInfoProvider,
    private val downloader: ApkDownloader = ApkDownloader { _, _ -> ApkDownloadResult.NetworkError },
    private val verifier: ApkVerifier = ApkVerifier { _, _ -> ApkVerificationResult.Corrupted },
    private val installer: UpdateInstaller = NoOpUpdateInstaller,
    installEvents: InstallEventStore = InstallEventStore(),
    private val preferences: UpdatePreferences? = null,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {
    private val appInfo = appInfoProvider.get()
    private val mutableState = MutableStateFlow(
        UpdateUiState(
            installedVersionName = appInfo.versionName,
            installedVersionCode = appInfo.versionCode,
            releaseHistory = releaseHistoryFor(appInfo.versionName)
        )
    )
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()
    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var verifiedFile: File? = null

    init {
        if (preferences != null) viewModelScope.launch {
            preferences.state.collect { preferenceState ->
                mutableState.update { current ->
                    current.copy(
                        automaticChecks = preferenceState.automaticChecks,
                        showUpdateBanner = current.availableUpdate?.versionCode?.toLong()
                            ?.let { it != preferenceState.dismissedVersionCode } == true
                    )
                }
            }
        }
        viewModelScope.launch {
            installEvents.events.collect { event ->
                verifiedFile?.delete()
                verifiedFile = null
                mutableState.update {
                    it.copy(downloadStatus = when (event) {
                        InstallEvent.Success -> UpdateDownloadStatus.Idle
                        InstallEvent.Cancelled -> UpdateDownloadStatus.InstallCancelled
                        is InstallEvent.Failure -> UpdateDownloadStatus.InstallFailed
                    })
                }
            }
        }
    }

    fun checkAutomatically() {
        val updatePreferences = preferences ?: return
        if (checkJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            val pref = updatePreferences.state.first()
            val now = clock.millis()
            if (!shouldCheckAutomatically(pref, now)) return@launch
            updatePreferences.recordAutomaticCheck(now)
            val result = checker.check()
            mutableState.update { current ->
                val checked = current.withCheckResult(result, hideAutomaticError = true)
                checked.copy(
                    showUpdateBanner = checked.availableUpdate?.versionCode?.toLong()
                        ?.let { it != pref.dismissedVersionCode } == true
                )
            }
        }
    }

    fun setAutomaticChecks(enabled: Boolean) {
        preferences?.let { viewModelScope.launch { it.setAutomaticChecks(enabled) } }
    }

    fun dismissAvailableUpdate() {
        val code = mutableState.value.availableUpdate?.versionCode?.toLong() ?: return
        preferences?.let { viewModelScope.launch { it.dismiss(code) } }
    }

    fun checkNow() {
        if (checkJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    checkStatus = UpdateCheckStatus.Checking,
                    availableUpdate = null,
                    requiredMinSdk = null
                )
            }
            val result = try {
                checker.check()
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            mutableState.update { current -> current.withCheckResult(result) }
        }
    }

    fun downloadUpdate() {
        val info = mutableState.value.availableUpdate ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.Downloading, downloadedBytes = 0) }
            when (val result = downloader.download(info) { read, total ->
                mutableState.update { it.copy(downloadedBytes = read, downloadTotalBytes = total) }
            }) {
                ApkDownloadResult.NetworkError -> mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.NetworkError) }
                ApkDownloadResult.NotEnoughSpace -> mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.NotEnoughSpace) }
                is ApkDownloadResult.Success -> {
                    when (verifier.verify(result.file, info.sha256)) {
                        ApkVerificationResult.Valid -> {
                            verifiedFile = result.file
                            mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.Ready) }
                        }
                        ApkVerificationResult.Corrupted -> failVerification(result.file, UpdateDownloadStatus.Corrupted)
                        ApkVerificationResult.WrongPackage -> failVerification(result.file, UpdateDownloadStatus.WrongPackage)
                        ApkVerificationResult.NotNewer -> failVerification(result.file, UpdateDownloadStatus.NotNewer)
                        ApkVerificationResult.WrongSignature -> failVerification(result.file, UpdateDownloadStatus.WrongSignature)
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.Idle) }
    }

    fun installUpdate() {
        val file = verifiedFile ?: return
        if (!installer.canInstallPackages()) {
            mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.NeedsPermission) }
            return
        }
        runCatching { installer.install(file) }
            .onSuccess { mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.Installing) } }
            .onFailure { mutableState.update { it.copy(downloadStatus = UpdateDownloadStatus.InstallFailed) } }
    }

    fun permissionIntent() = installer.permissionIntent()
    fun resumeInstallationAfterPermission() { if (installer.canInstallPackages()) installUpdate() }

    private fun failVerification(file: File, status: UpdateDownloadStatus) {
        file.delete()
        mutableState.update { it.copy(downloadStatus = status) }
    }
}

private object NoOpUpdateInstaller : UpdateInstaller {
    override fun canInstallPackages() = false
    override fun permissionIntent() = android.content.Intent()
    override fun install(file: File) = Unit
}

private fun UpdateUiState.withCheckResult(result: UpdateCheckResult, hideAutomaticError: Boolean = false): UpdateUiState = when (result) {
    UpdateCheckResult.InvalidFile -> copy(checkStatus = if (hideAutomaticError) UpdateCheckStatus.Idle else UpdateCheckStatus.InvalidFile)
    UpdateCheckResult.NetworkError -> copy(checkStatus = if (hideAutomaticError) UpdateCheckStatus.Idle else UpdateCheckStatus.NetworkError)
    is UpdateCheckResult.Checked -> when (val availability = result.availability) {
        UpdateAvailability.UpToDate -> copy(checkStatus = UpdateCheckStatus.UpToDate)
        is UpdateAvailability.Available -> copy(
            checkStatus = UpdateCheckStatus.Available,
            availableUpdate = availability.info,
            showUpdateBanner = true
        )
        is UpdateAvailability.RequiresNewerAndroid -> copy(
            checkStatus = UpdateCheckStatus.RequiresNewerAndroid,
            requiredMinSdk = availability.minSdk
        )
    }
}
