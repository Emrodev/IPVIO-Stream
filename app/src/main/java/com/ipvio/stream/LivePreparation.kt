package com.ipvio.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Shared background work for Live TV. A screen and the home-screen prewarm await the same job.
 *
 * Threading: all job bookkeeping happens under [lock]. The jobs run on [Dispatchers.Default] and only read the
 * (never mutated) item lists, so [LiveGenre] and [LiveBundleLogic] guard just their own result caches.
 * A job that failed is never handed out again: the next caller starts a fresh one.
 */
object LivePreparation {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var genreSource: List<Item>? = null
    private var genreJob: Deferred<LinkedHashMap<String, List<Item>>>? = null
    private val bundleJobs = ArrayList<Pair<List<Item>, Deferred<LiveBundleLogic.Result?>>>()

    fun genres(live: List<Item>): Deferred<LinkedHashMap<String, List<Item>>> = synchronized(lock) {
        // A failed job (isCancelled) must not be reused, otherwise every await() would throw again.
        if (genreSource === live) genreJob?.takeIf { !it.isCancelled }?.let { return@synchronized it }
        genreSource = live
        scope.async { LiveGenre.split(live) }.also { genreJob = it }
    }

    fun bundle(items: List<Item>): Deferred<LiveBundleLogic.Result?> = synchronized(lock) {
        bundleJobs.removeAll { it.second.isCancelled }
        bundleJobs.firstOrNull { it.first === items }?.let { return@synchronized it.second }
        // Keep the raw rows if the Java heap is already under pressure; bundling needs temporary maps and lists.
        if (Mem.heapTight(0.75)) return@synchronized scope.async { null }
        scope.async { LiveBundleLogic.bundle(items) }.also {
            bundleJobs.add(0, items to it)
            while (bundleJobs.size > 2) bundleJobs.removeAt(bundleJobs.lastIndex)
        }
    }

    /** Genres for [live], or null if the preparation failed (e.g. out of memory). Cancellation is passed on. */
    suspend fun genresOrNull(live: List<Item>): Map<String, List<Item>>? = genres(live).awaitOrNull()

    /** Bundled rows for [items], or null if bundling was skipped or failed. Cancellation is passed on. */
    suspend fun bundleOrNull(items: List<Item>): LiveBundleLogic.Result? = bundle(items).awaitOrNull()

    /** Drop prepared results after Android reports memory pressure. */
    fun trim() = synchronized(lock) {
        genreSource = null
        genreJob = null
        bundleJobs.clear()
        LiveGenre.trim()
        LiveBundleLogic.trim()
    }
}

/**
 * Awaits a shared job without letting its failure reach the caller. The job belongs to the shared scope, not to the
 * caller, so its failure must not crash a composition; the caller falls back to the unprepared data instead.
 * If the caller itself was cancelled, the cancellation is rethrown.
 */
suspend fun <T> Deferred<T>.awaitOrNull(): T? = try {
    await()
} catch (e: Throwable) {
    currentCoroutineContext().ensureActive()
    null
}
