package com.ipvio.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** Shared background work for Live TV. A screen and the home-screen prewarm await the same job. */
object LivePreparation {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var genreSource: List<Item>? = null
    private var genreJob: Deferred<LinkedHashMap<String, List<Item>>>? = null
    private val bundleJobs = ArrayList<Pair<List<Item>, Deferred<LiveBundleLogic.Result?>>>()

    fun genres(live: List<Item>): Deferred<LinkedHashMap<String, List<Item>>> = synchronized(lock) {
        if (genreSource === live) genreJob?.let { return@synchronized it }
        genreSource = live
        scope.async { LiveGenre.split(live) }.also { genreJob = it }
    }

    fun bundle(items: List<Item>): Deferred<LiveBundleLogic.Result?> = synchronized(lock) {
        bundleJobs.firstOrNull { it.first === items }?.let { return@synchronized it.second }
        // Keep the raw rows if the Java heap is already under pressure; bundling needs temporary maps and lists.
        if (Mem.heapTight(0.75)) return@synchronized scope.async { null }
        scope.async { LiveBundleLogic.bundle(items) }.also {
            bundleJobs.add(0, items to it)
            while (bundleJobs.size > 2) bundleJobs.removeAt(bundleJobs.lastIndex)
        }
    }

    /** Drop prepared results after Android reports memory pressure. */
    fun trim() = synchronized(lock) {
        genreSource = null
        genreJob = null
        bundleJobs.clear()
        LiveGenre.trim()
        LiveBundleLogic.trim()
    }
}
