package com.giwu.bible.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the update prompt has got to. */
sealed interface UpdateState {

    data object Idle : UpdateState

    /**
     * [announced] separates the two kinds of check. The daily one happens
     * behind the reader and says nothing until it finds something; the one
     * from Settings was asked for, so it shows its working and its answer.
     */
    data class Checking(val announced: Boolean) : UpdateState

    data object UpToDate : UpdateState

    data class Available(val release: AppRelease) : UpdateState

    data class NeedsPermission(val release: AppRelease) : UpdateState

    data class Downloading(val release: AppRelease, val percent: Int) : UpdateState

    data class Failed(val message: String) : UpdateState
}

/** The slice of the stored preferences the updater keeps for itself. */
interface UpdatePreferences {

    val automaticUpdateChecks: StateFlow<Boolean>

    /** Epoch millis of the last check that actually reached GitHub. */
    var lastUpdateCheck: Long

    /** A release the reader chose to pass over, so it stops asking. */
    var skippedUpdateTag: String?
}

/**
 * Keeps the app in step with the builds CI publishes.
 *
 * Two ways in. [checkOnLaunch] runs every time the app is opened but does the
 * work at most once a day, which is what makes it a daily check without a
 * background job to schedule, a notification channel, or a wakeup for someone
 * who is not reading. [checkNow] is the Settings button and ignores both the
 * interval and a skipped release, because an explicit question deserves an
 * explicit answer.
 *
 * Holds no Android types, so the whole decision is unit-testable; the
 * download and the install sit behind [ApkInstaller].
 */
class AppUpdates(
    private val source: ReleaseSource,
    private val installer: ApkInstaller,
    private val prefs: UpdatePreferences,
    val installed: InstalledVersion,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** One at a time: a check must not land on top of a download. */
    private var job: Job? = null

    fun checkOnLaunch() {
        if (!prefs.automaticUpdateChecks.value) return
        if (now() - prefs.lastUpdateCheck < CHECK_INTERVAL_MS) return
        check(announced = false)
    }

    fun checkNow() = check(announced = true)

    private fun check(announced: Boolean) {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = UpdateState.Checking(announced)

            val release = try {
                source.latest()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A silent check that cannot reach GitHub leaves no trace and
                // no stored timestamp, so the next launch simply tries again.
                _state.value = when {
                    announced -> UpdateState.Failed(e.message ?: "Could not reach GitHub.")
                    else -> UpdateState.Idle
                }
                return@launch
            }

            prefs.lastUpdateCheck = now()
            _state.value = when {
                release == null ||
                    !release.version.isNewerThan(installed.name, installed.code) ->
                    if (announced) UpdateState.UpToDate else UpdateState.Idle

                !announced && release.tag == prefs.skippedUpdateTag -> UpdateState.Idle

                else -> UpdateState.Available(release)
            }
        }
    }

    /**
     * Downloads and installs [release].
     *
     * Sideloading needs the reader to allow this app to install packages, and
     * that is a trip to a system screen rather than a runtime permission
     * dialog — so this parks on [UpdateState.NeedsPermission] and is called
     * again when they come back.
     */
    fun install(release: AppRelease) {
        if (job?.isActive == true) return
        if (!installer.canInstallPackages()) {
            _state.value = UpdateState.NeedsPermission(release)
            installer.requestInstallPermission()
            return
        }
        job = scope.launch {
            _state.value = UpdateState.Downloading(release, 0)
            try {
                installer.downloadAndInstall(release) { percent ->
                    _state.value = UpdateState.Downloading(release, percent)
                }
                // The system installer has the file and the screen now.
                _state.value = UpdateState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(e.message ?: "The download failed.")
            }
        }
    }

    /** Stops the daily check from raising this release again. */
    fun skip(release: AppRelease) {
        prefs.skippedUpdateTag = release.tag
        dismiss()
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
