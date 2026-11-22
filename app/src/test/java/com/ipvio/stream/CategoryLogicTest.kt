package com.ipvio.stream

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-054: provider categories list every title of the provider, also when it is in several categories. Pure JVM, no Android. */
class CategoryLogicTest {
    private fun movie(id: String, title: String, group: String, year: Int? = 2026, lang: String = "de") =
        Item(id, Kind.MOVIE, title, year, "http://h/movie/u/p/$id.mp4", null, group, lang = lang)

    @Test fun movieInTwoCategories_isInBothCategories() {
        val all = listOf(
            movie("1", "Film A", "Action"), movie("2", "Film B", "Action"),
            movie("3", "Film A", "All films 2026"), movie("4", "Film B", "All films 2026"), movie("5", "Film C", "All films 2026")
        )
        val cats = CategoryLogic.byGroup(all)
        assertEquals(2, cats.getValue("Action").size)
        assertEquals(3, cats.getValue("All films 2026").size)
    }

    @Test fun allMovies_showsEachTitleAndLanguageOnce() {
        val all = listOf(
            movie("1", "Film A", "Action"), movie("3", "Film A", "All films 2026"),
            movie("4", "Film A", "German", lang = "en"), movie("5", "Film B", "Action")
        )
        val unique = CategoryLogic.uniqueMovies(all)
        assertEquals(listOf("1", "4", "5"), unique.map { it.id })
    }

    @Test fun categoriesKeepProviderOrderAndEveryEntry() {
        val all = listOf(movie("1", "Same", "G"), movie("2", "Same", "G"), movie("3", "Other", "H"))
        val cats = CategoryLogic.byGroup(all)
        assertEquals(listOf("G", "H"), cats.keys.toList())
        assertEquals(listOf("1", "2"), cats.getValue("G").map { it.id })
    }

    @Test fun mergedSeries_isListedInEveryCategoryItCameFrom() {
        val s = Item("s_dark", Kind.SERIES, "Dark", null, "", null, "Netflix", moreGroups = listOf("Sci-Fi", "Netflix"))
        val cats = CategoryLogic.byGroup(listOf(s))
        assertEquals(setOf("Netflix", "Sci-Fi"), cats.keys)
        assertEquals(1, cats.getValue("Netflix").size)
        assertEquals(1, cats.getValue("Sci-Fi").size)
    }

    @Test fun parsedMovies_sameFilmInTwoCategoriesKeepsBoth() {
        val m3u = """#EXTM3U
#EXTINF:-1 group-title="Action",Film A (2026)
http://h/movie/u/p/1.mp4
#EXTINF:-1 group-title="All films 2026",Film A (2026)
http://h/movie/u/p/2.mp4
#EXTINF:-1 group-title="All films 2026",Film B (2026)
http://h/movie/u/p/3.mp4
"""
        val items = M3uParser.parse(m3u)
        val cats = CategoryLogic.byGroup(items)
        assertEquals(1, cats.getValue("Action").size)
        assertEquals(2, cats.getValue("All films 2026").size)
        assertEquals(2, CategoryLogic.uniqueMovies(items).size)
    }

    @Test fun parsedSeries_sameNameInTwoCategoriesIsOneTitleInBoth() {
        val m3u = """#EXTM3U
#EXTINF:-1 group-title="Netflix",Dark S01E01
http://h/series/u/p/1.mkv
#EXTINF:-1 group-title="Sci-Fi",Dark S01E01
http://h/series/u/p/2.mkv
#EXTINF:-1 group-title="Sci-Fi",Other S01E01
http://h/series/u/p/3.mkv
"""
        val items = M3uParser.parse(m3u)
        val dark = items.single { it.title == "Dark" }
        assertEquals("Netflix", dark.group)
        assertEquals(listOf("Sci-Fi"), dark.moreGroups)
        assertEquals(2, items.size)
        val cats = CategoryLogic.byGroup(items)
        assertEquals(listOf("Dark"), cats.getValue("Netflix").map { it.title })
        assertEquals(setOf("Dark", "Other"), cats.getValue("Sci-Fi").map { it.title }.toSet())
    }

    @Test fun parsedSeries_sameCategoryTwiceAddsNoFurtherCategory() {
        val m3u = """#EXTM3U
#EXTINF:-1 group-title="Netflix",Dark S01E01
http://h/series/u/p/1.mkv
#EXTINF:-1 group-title="Netflix",Dark S01E02
http://h/series/u/p/2.mkv
"""
        assertTrue(M3uParser.parse(m3u).single().moreGroups.isEmpty())
    }

    @Test fun furtherCategories_areLimited() {
        val sb = StringBuilder("#EXTM3U\n")
        for (g in 0..40) sb.append("#EXTINF:-1 group-title=\"G$g\",Dark S01E01\nhttp://h/series/u/p/$g.mkv\n")
        assertEquals(CategoryLogic.MAX_MORE_GROUPS, M3uParser.parse(sb.toString()).single().moreGroups.size)
    }

    @Test fun cache_furtherCategoriesSurviveARoundTrip() {
        val s = Item("s_dark", Kind.SERIES, "Dark", null, "", null, "Netflix", moreGroups = listOf("Sci-Fi", "Drama"))
        val plain = movie("1", "Film", "Action")
        val out = ByteArrayOutputStream()
        PlaylistCacheIO.write(out, "http://a", listOf(s, plain), emptyList(), emptyMap())
        val c = PlaylistCacheIO.read(ByteArrayInputStream(out.toByteArray()), "http://a", emptySet())!!
        assertEquals(listOf("Sci-Fi", "Drama"), c.items[0].moreGroups)
        assertTrue(c.items[1].moreGroups.isEmpty())
    }

    @Test fun cache_hiddenLanguageSkipsTheFurtherCategoriesToo() {
        val hiddenOne = Item("s_a", Kind.SERIES, "Hidden", null, "", null, "TR | Dizi", lang = "tr", moreGroups = listOf("X", "Y"))
        val shown = Item("s_b", Kind.SERIES, "Shown", null, "", null, "DE | Serien", lang = "de", moreGroups = listOf("Z"))
        val out = ByteArrayOutputStream()
        PlaylistCacheIO.write(out, "http://a", listOf(hiddenOne, shown), emptyList(), emptyMap())
        val c = PlaylistCacheIO.read(ByteArrayInputStream(out.toByteArray()), "http://a", setOf("tr"))!!
        assertEquals(listOf("Shown"), c.items.map { it.title })
        assertEquals(listOf("Z"), c.items.single().moreGroups)
    }

    /** A cache written by the previous app version (6, without further categories) is still read. */
    @Test fun cache_version6IsStillRead() {
        val out = ByteArrayOutputStream()
        val o = DataOutputStream(out)
        fun str(s: String?) { if (s == null) o.writeInt(-1) else { val b = s.toByteArray(); o.writeInt(b.size); o.write(b) } }
        o.writeInt(6); str("http://a"); o.writeInt(0); o.writeInt(0)
        str(LangTagLogic.encode(emptySet())); o.writeInt(0); o.writeInt(0)
        o.writeInt(2)
        for (n in 1..2) {
            str("$n"); o.writeInt(Kind.MOVIE.ordinal); str("de"); str("Film $n"); o.writeInt(2026); str("http://h/movie/u/p/$n.mp4")
            str(null); str("Action"); str(null); str(null); o.writeInt(0); o.writeLong(1700000000L + n)
        }
        val c = PlaylistCacheIO.read(ByteArrayInputStream(out.toByteArray()), "http://a", emptySet())!!
        assertEquals(listOf("Film 1", "Film 2"), c.items.map { it.title })
        assertEquals(1700000002L, c.items[1].added)
        assertTrue(c.items.all { it.moreGroups.isEmpty() })
    }
}
