package com.giwu.bible.ui.welcome

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.giwu.bible.AppContainer
import com.giwu.bible.data.BibleSeed
import com.giwu.bible.model.Bible
import com.giwu.bible.repo.BibleRepository
import com.giwu.bible.repo.DownloadStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val bibles: List<Bible> = BibleSeed.versions,
    val selected: Set<String> = emptySet(),
    val working: Boolean = false,

    /** What the progress overlay says, e.g. "Downloading KJV…". */
    val status: String = "",
    val done: Int = 0,
    val total: Int = 0,

    /** A one-shot message for the snackbar. */
    val message: String? = null,
) {
    /** True when at least one ticked translation still has to be fetched. */
    val hasPendingDownloads: Boolean
        get() = selected.any { table -> bibles.none { it.table == table && it.downloaded } }

    /** True once something is on the device, so reading can begin. */
    val canContinue: Boolean
        get() = bibles.any { it.downloaded }
}

/**
 * The setup screen: pick translations, fetch them, or sideload a database.
 *
 * The list fills in three passes so nothing ever blocks on the network: the
 * bundled seed shows instantly, the database overlays the real downloaded
 * flags, and the server refresh adds anything new.
 */
class WelcomeViewModel(private val repository: BibleRepository) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val local = runCatching { repository.localBibles() }.getOrNull()
            if (!local.isNullOrEmpty()) applyCatalog(local)

            val refreshed = try {
                repository.refreshCatalog()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The cards are already on screen from the seed or the
                // database, so a failed refresh stays quiet.
                null
            }
            if (!refreshed.isNullOrEmpty()) applyCatalog(refreshed)
        }
    }

    /** Keeps already-downloaded translations ticked. */
    private fun applyCatalog(bibles: List<Bible>) {
        _state.update { current ->
            current.copy(
                bibles = bibles,
                selected = current.selected + bibles.filter { it.downloaded }.map { it.table },
            )
        }
    }

    fun toggle(table: String) {
        _state.update { current ->
            val selected = if (current.selected.contains(table)) {
                current.selected - table
            } else {
                current.selected + table
            }
            current.copy(selected = selected)
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /**
     * Fetches every ticked translation that is not on the device yet, then
     * calls [onReady] if anything is readable.
     */
    fun downloadSelected(onReady: () -> Unit) {
        val current = _state.value
        val pending = current.selected.filter { table ->
            current.bibles.none { it.table == table && it.downloaded }
        }
        if (pending.isEmpty()) {
            if (current.canContinue) onReady()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(working = true, done = 0, total = pending.size) }

            pending.forEachIndexed { index, table ->
                val name = _state.value.bibles
                    .firstOrNull { it.table == table }
                    ?.abbreviation
                    ?: table

                try {
                    repository.downloadBible(table) { stage ->
                        _state.update {
                            it.copy(
                                status = when (stage) {
                                    DownloadStage.Downloading -> "Downloading $name…"
                                    is DownloadStage.Storing -> "Storing $name…"
                                },
                            )
                        }
                    }
                    _state.update { it.copy(done = index + 1) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Downloading $table failed", e)
                    _state.update {
                        it.copy(message = "Failed to download $name: ${e.message}")
                    }
                }
            }

            val bibles = runCatching { repository.localBibles() }.getOrNull()
            _state.update { current ->
                current.copy(
                    working = false,
                    status = "",
                    bibles = bibles ?: current.bibles,
                )
            }
            if (repository.isSetupComplete()) onReady()
        }
    }

    /** Copies every translation out of a SQLite file the reader supplied. */
    fun syncFromFile(path: String, onReady: () -> Unit) {
        viewModelScope.launch {
            _state.update {
                it.copy(working = true, status = "Reading database…", done = 0, total = 0)
            }
            try {
                val synced = repository.syncFromSqliteFile(path) { table, current, total ->
                    _state.update {
                        it.copy(
                            status = "Syncing ${table.removePrefix("t_").uppercase()}…",
                            done = current,
                            total = total,
                        )
                    }
                }
                val bibles = runCatching { repository.localBibles() }.getOrNull()
                _state.update { current ->
                    current.copy(
                        working = false,
                        status = "",
                        bibles = bibles ?: current.bibles,
                        selected = current.selected + synced,
                        message = if (synced.isEmpty()) {
                            "That file had no Bible tables in it."
                        } else {
                            "Synced ${synced.size} Bible(s) from the file."
                        },
                    )
                }
                if (repository.isSetupComplete()) onReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Syncing from $path failed", e)
                _state.update {
                    it.copy(
                        working = false,
                        status = "",
                        message = "Sync failed: ${e.message}",
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "WelcomeViewModel"

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = WelcomeViewModel(container.repository) as T
            }
    }
}
