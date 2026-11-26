package com.ipvio.stream

/**
 * T-069c: the other sources of a movie (the same movie as separate entries of the provider, e.g. 4K, FHD and HD): same
 * kind, same cleaned title and year ([Item.mk]) and the same language. Pure logic, no Android imports, covered by JVM tests.
 */
object MovieSourcesLogic {
    /** The movie itself first, then the other entries with its key in playlist order. One pass over [all] (the rule since 1.0). */
    fun scan(item: Item, all: List<Item>): List<Item> {
        if (item.kind != Kind.MOVIE) return listOf(item)
        val k = item.mk
        val same = all.filter { it.kind == Kind.MOVIE && it.lang == item.lang && it.mk == k }
        return if (same.isEmpty()) listOf(item) else (listOf(item) + same.filter { it.id != item.id })
    }

    fun keyHash(mk: String, lang: String): Int = mk.hashCode() * 31 + lang.hashCode()
}

/**
 * T-069c: the movies of one playlist with a number per key ([MovieSourcesLogic.keyHash]), built once in the background.
 * [alternatives] compares numbers instead of texts for every title, so pressing Play no longer runs a text comparison over
 * the whole playlist on the main thread. Same result and order as [MovieSourcesLogic.scan]. Memory: one number and one
 * reference per movie (about 1 MB for 150,000 movies). [all] must not be changed after the build (it never is: every load,
 * filter or change makes a new list).
 */
class MovieSourceIndex private constructor(val all: List<Item>, private val hashes: IntArray, private val movies: Array<Item?>) {
    fun alternatives(item: Item): List<Item> {
        if (item.kind != Kind.MOVIE) return listOf(item)
        val k = item.mk
        val h = MovieSourcesLogic.keyHash(k, item.lang)
        var same: ArrayList<Item>? = null
        for (j in hashes.indices) {
            if (hashes[j] != h) continue
            val x = movies[j] ?: continue
            if (x.lang == item.lang && x.mk == k) (same ?: ArrayList<Item>(4).also { same = it }).add(x)
        }
        val s = same ?: return listOf(item)
        return listOf(item) + s.filter { it.id != item.id }
    }

    companion object {
        fun build(all: List<Item>): MovieSourceIndex {
            var n = 0
            for (x in all) if (x.kind == Kind.MOVIE) n++
            val hashes = IntArray(n)
            val movies = arrayOfNulls<Item>(n)
            var j = 0
            for (x in all) {
                if (x.kind != Kind.MOVIE || j >= n) continue
                hashes[j] = MovieSourcesLogic.keyHash(x.mk, x.lang)
                movies[j] = x
                j++
            }
            return MovieSourceIndex(all, hashes, movies)
        }
    }
}
