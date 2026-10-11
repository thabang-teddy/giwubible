package com.giwu.bible.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collects [flow] for as long as this scope lives. */
fun <T> CoroutineScope.launchCollect(flow: Flow<T>, block: suspend (T) -> Unit): Job =
    launch { flow.collect { block(it) } }
