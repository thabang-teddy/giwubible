package com.giwu.bible.update

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdatesTest {

    // ── Fakes ──────────────────────────────────────────────────────────────

    private class FakeSource(
        var release: AppRelease? = null,
        var failure: Exception? = null,
    ) : ReleaseSource {
        var calls = 0

        override suspend fun latest(): AppRelease? {
            calls++
            failure?.let { throw it }
            return release
        }
    }

    private class FakeInstaller(var allowed: Boolean = true) : ApkInstaller {
        var permissionRequests = 0
        var installed: AppRelease? = null
        var failure: Exception? = null

        override fun canInstallPackages() = allowed

        override fun requestInstallPermission() {
            permissionRequests++
        }

        override suspend fun downloadAndInstall(
            release: AppRelease,
            onProgress: (Int) -> Unit,
        ) {
            onProgress(50)
            failure?.let { throw it }
            installed = release
        }
    }

    private class FakePrefs(automatic: Boolean = true) : UpdatePreferences {
        override val automaticUpdateChecks: StateFlow<Boolean> = MutableStateFlow(automatic)
        override var lastUpdateCheck: Long = 0
        override var skippedUpdateTag: String? = null
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun releaseOf(tag: String) = AppRelease(
        tag = tag,
        title = tag,
        notes = "",
        pageUrl = "",
        apkUrl = "https://example.test/app.apk",
        apkSizeBytes = 1,
        version = requireNotNull(ReleaseVersion.parse(tag)),
    )

    private val installed = InstalledVersion(name = "1.0.0", code = 10)

    private fun updatesOf(
        source: FakeSource,
        installer: FakeInstaller = FakeInstaller(),
        prefs: FakePrefs = FakePrefs(),
        scope: CoroutineScope,
        now: Long = NOW,
    ) = AppUpdates(
        source = source,
        installer = installer,
        prefs = prefs,
        installed = installed,
        scope = scope,
        now = { now },
    )

    // ── Checking ───────────────────────────────────────────────────────────

    @Test
    fun `a newer release is offered`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.11"))

        val updates = updatesOf(source, scope = scope)
        updates.checkOnLaunch()

        assertEquals(
            UpdateState.Available(releaseOf("v1.0.0-staging.11")),
            updates.state.value,
        )
    }

    @Test
    fun `a release that is not newer leaves a silent check silent`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.10"))

        val updates = updatesOf(source, scope = scope)
        updates.checkOnLaunch()

        assertEquals(UpdateState.Idle, updates.state.value)
    }

    @Test
    fun `the same check answers when it was asked for`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.10"))

        val updates = updatesOf(source, scope = scope)
        updates.checkNow()

        assertEquals(UpdateState.UpToDate, updates.state.value)
    }

    @Test
    fun `a launch check inside the daily window does not reach the network`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.11"))
        val prefs = FakePrefs().apply { lastUpdateCheck = NOW - 60_000 }

        updatesOf(source, prefs = prefs, scope = scope).checkOnLaunch()

        assertEquals(0, source.calls)
    }

    @Test
    fun `a launch check a day later does reach the network`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.11"))
        val prefs = FakePrefs().apply { lastUpdateCheck = NOW - DAY_MS }

        updatesOf(source, prefs = prefs, scope = scope).checkOnLaunch()

        assertEquals(1, source.calls)
        assertEquals(NOW, prefs.lastUpdateCheck)
    }

    @Test
    fun `automatic checks turned off stop the launch check`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(releaseOf("v1.0.0-staging.11"))

        updatesOf(source, prefs = FakePrefs(automatic = false), scope = scope)
            .checkOnLaunch()

        assertEquals(0, source.calls)
    }

    @Test
    fun `a skipped release is passed over silently but still answers a direct ask`() =
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val source = FakeSource(releaseOf("v1.0.0-staging.11"))
            val prefs = FakePrefs().apply { skippedUpdateTag = "v1.0.0-staging.11" }

            val updates = updatesOf(source, prefs = prefs, scope = scope)
            updates.checkOnLaunch()
            assertEquals(UpdateState.Idle, updates.state.value)

            updates.checkNow()
            assertTrue(updates.state.value is UpdateState.Available)
        }

    @Test
    fun `skipping stores the tag`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val prefs = FakePrefs()
        val release = releaseOf("v1.0.0-staging.11")

        val updates = updatesOf(FakeSource(release), prefs = prefs, scope = scope)
        updates.skip(release)

        assertEquals("v1.0.0-staging.11", prefs.skippedUpdateTag)
        assertEquals(UpdateState.Idle, updates.state.value)
    }

    @Test
    fun `a failed silent check says nothing and does not bank the timestamp`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val source = FakeSource(failure = IOException("offline"))
        val prefs = FakePrefs()

        val updates = updatesOf(source, prefs = prefs, scope = scope)
        updates.checkOnLaunch()

        assertEquals(UpdateState.Idle, updates.state.value)
        assertEquals(0L, prefs.lastUpdateCheck)
    }

    @Test
    fun `a failed check that was asked for reports the failure`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val updates = updatesOf(FakeSource(failure = IOException("offline")), scope = scope)
        updates.checkNow()

        assertEquals(UpdateState.Failed("offline"), updates.state.value)
    }

    // ── Installing ─────────────────────────────────────────────────────────

    @Test
    fun `installing without permission asks for it instead of downloading`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val installer = FakeInstaller(allowed = false)
        val release = releaseOf("v1.0.0-staging.11")

        val updates = updatesOf(FakeSource(release), installer, scope = scope)
        updates.install(release)

        assertEquals(UpdateState.NeedsPermission(release), updates.state.value)
        assertEquals(1, installer.permissionRequests)
        assertNull(installer.installed)
    }

    @Test
    fun `installing downloads and hands over, then clears the prompt`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val installer = FakeInstaller()
        val release = releaseOf("v1.0.0-staging.11")

        val updates = updatesOf(FakeSource(release), installer, scope = scope)
        updates.install(release)

        assertEquals(release, installer.installed)
        assertEquals(UpdateState.Idle, updates.state.value)
    }

    @Test
    fun `a failed download is reported`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val installer = FakeInstaller().apply { failure = IOException("no space") }
        val release = releaseOf("v1.0.0-staging.11")

        val updates = updatesOf(FakeSource(release), installer, scope = scope)
        updates.install(release)

        assertEquals(UpdateState.Failed("no space"), updates.state.value)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000

        /** Far enough past the epoch that a stored 0 reads as "never checked". */
        const val NOW = 2 * DAY_MS
    }
}
