package com.giwu.bible

import android.content.Context
import com.giwu.bible.data.AppPrefs
import com.giwu.bible.data.BibleDatabase
import com.giwu.bible.data.TokenStore
import com.giwu.bible.data.remote.AccountApi
import com.giwu.bible.data.remote.ApiClient
import com.giwu.bible.data.remote.BibleApi
import com.giwu.bible.repo.AuthStore
import com.giwu.bible.repo.BibleRepository
import com.giwu.bible.repo.BookmarkStore
import com.giwu.bible.repo.ServerSettings
import com.giwu.bible.tts.AndroidTtsEngine
import com.giwu.bible.tts.TtsController
import com.giwu.bible.tts.TtsEngine
import com.giwu.bible.util.launchCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * The app's object graph, assembled by hand.
 *
 * Small enough that a dependency-injection framework would add more ceremony
 * than it saves. Everything here is a process-wide singleton: the reader's
 * position, session and playback all have to survive a rotation and a trip
 * through another screen.
 */
class AppContainer(context: Context, private val appScope: CoroutineScope) {

    private val appContext = context.applicationContext

    val prefs: AppPrefs = AppPrefs.create(appContext)
    val database: BibleDatabase = BibleDatabase.open(appContext)
    val client: ApiClient = ApiClient()

    private val bibleApi = BibleApi(client)
    private val accountApi = AccountApi(client)
    private val tokens = TokenStore.create(appContext)

    val repository: BibleRepository = BibleRepository(database, bibleApi)

    val ttsEngine: TtsEngine = AndroidTtsEngine(appContext)
    val tts: TtsController = TtsController(
        engine = ttsEngine,
        scope = appScope,
        rate = prefs.speechRate.value,
        announceNumbers = prefs.announceVerseNumbers.value,
    )

    /**
     * Started as soon as the process does, and awaited by the first screen.
     *
     * The stored server URL has to reach the HTTP client before any request
     * goes out, and the session restore has to wait for that URL — so both
     * happen here rather than in whichever screen happens to come first.
     */
    val startup: Deferred<AppGraph> = appScope.async(Dispatchers.IO) {
        val serverSettings = ServerSettings.load(database, client)
        val auth = AuthStore(accountApi, client, tokens, appScope)
        val bookmarks = BookmarkStore(accountApi, auth, appScope)

        AppGraph(
            serverSettings = serverSettings,
            auth = auth,
            bookmarks = bookmarks,
            showWelcome = !repository.isSetupComplete(),
        )
    }

    /** Keeps playback settings and the controller in step. */
    fun observePlaybackSettings(scope: CoroutineScope) {
        scope.launchCollect(prefs.speechRate) { tts.setRate(it) }
        scope.launchCollect(prefs.announceVerseNumbers) { tts.announceNumbers = it }
    }
}

/** The parts of the graph that only exist once startup has resolved. */
class AppGraph(
    val serverSettings: ServerSettings,
    val auth: AuthStore,
    val bookmarks: BookmarkStore,

    /** True on a fresh install, where nothing is downloaded yet. */
    val showWelcome: Boolean,
)
