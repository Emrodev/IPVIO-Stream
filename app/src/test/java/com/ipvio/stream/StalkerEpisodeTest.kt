package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Series in a portal's movie area where the episodes carry no play command (only "series_files"): the file is looked up
 * per episode at play time. Synthetic data, shaped like a real Ministra answer.
 */
class StalkerEpisodeTest {
    @Test fun portalFlagsAreReadInAnySpelling() {
        for (v in listOf("1", "true", "True", "TRUE", " true ")) assertTrue(v, StalkerLogic.truthy(v))
        for (v in listOf(null, "", "0", "false", "False", "no")) assertFalse(v.toString(), StalkerLogic.truthy(v))
    }

    @Test fun episodeRowsAreRecognized() {
        assertTrue(StalkerLogic.isEpisodeRow(mapOf("id" to "77", "is_episode" to "True", "series_number" to "3")))
        assertFalse(StalkerLogic.isEpisodeRow(mapOf("id" to "41", "is_season" to "True", "season_number" to "1")))
    }

    @Test fun episodeFileAddressRoundTrips() {
        val url = StalkerLogic.episodeFileUrl(5, "900", "41", "77", "20")
        assertTrue(url, url.startsWith("stalker://5/efile/") && url.endsWith(".mp4"))
        val ref = StalkerLogic.itemRef(url)!!
        assertEquals("efile", ref.type)
        assertEquals("&movie_id=900&season_id=41&episode_id=77&category=20&p=1", StalkerLogic.episodeFileQuery(ref.cmd))
        assertTrue(StalkerLogic.isItem(url))
    }

    @Test fun brokenEpisodeFileAddressIsRejected() {
        assertNull(StalkerLogic.episodeFileQuery("900/41"))
        assertNull(StalkerLogic.episodeFileQuery("900/41/77/20/x"))
        assertNull(StalkerLogic.episodeFileQuery("/41/77/20"))
    }

    @Test fun fileCommandPrefersThePortalsCommand() {
        assertEquals("/media/file_55.mpg", StalkerLogic.fileCmd(mapOf("id" to "55", "cmd" to "/media/file_55.mpg", "is_file" to "True")))
        assertEquals("/media/file_56.mpg", StalkerLogic.fileCmd(mapOf("id" to "56", "is_file" to "True")))
        assertNull(StalkerLogic.fileCmd(mapOf("name" to "no id")))
    }

    /** Some portals bind the account to the MAC typed as device ID / serial, colons included. */
    @Test fun deviceIdsKeepMacSeparators() {
        assertEquals("00:1A:79:AB:CD:EF", StalkerLogic.cleanId(" 00:1A:79:AB:CD:EF "))
        assertEquals("AB-12", StalkerLogic.cleanId("AB-12"))
        assertEquals("ABC", StalkerLogic.cleanId("A#B&C"))
        val src = StalkerLogic.parse("stalker:http://portal.example/c/#mac=00:1A:79:AB:CD:EF#id1=00:1A:79:AB:CD:EF")!!
        assertEquals("00:1A:79:AB:CD:EF", src.id1)
        assertTrue(StalkerLogic.profileExtra(src).contains("&device_id=00%3A1A%3A79%3AAB%3ACD%3AEF&"))
    }

    @Test fun episodeTitleComesFromThePortal() {
        assertEquals("Holiday Heroes", StalkerLogic.episodeTitle(mapOf("series_name" to "Holiday Heroes", "name" to "Episode 10. Holiday Heroes"), 10))
        assertEquals("Holiday Heroes", StalkerLogic.episodeTitle(mapOf("name" to "Episode 10. Holiday Heroes"), 10))
        // Only a number or nothing: the usual "Episode N"
        assertEquals("Episode 4", StalkerLogic.episodeTitle(mapOf("name" to "Episode 4"), 4))
        assertEquals("Episode 4", StalkerLogic.episodeTitle(mapOf("series_name" to " "), 4))
    }

    /** Portal search (titles loaded on demand): short terms are not sent, spaces are tidied. */
    @Test fun portalSearchTermIsTidied() {
        assertEquals("breaking bad", StalkerLogic.searchTerm("  breaking   bad "))
        assertNull(StalkerLogic.searchTerm("bb"))
        assertNull(StalkerLogic.searchTerm("   "))
        assertEquals(60, StalkerLogic.searchTerm("x".repeat(100))!!.length)
    }

    /** Typing letter by letter: once a term brought all its hits, longer terms with it are not sent again. */
    @Test fun narrowerTermsAreNotSentAgain() {
        assertTrue(StalkerLogic.coveredBy("breaking bad", listOf("breaking")))
        assertTrue(StalkerLogic.coveredBy("the breaking", listOf("breaking")))
        assertFalse(StalkerLogic.coveredBy("breakin", listOf("breaking")))
        assertFalse(StalkerLogic.coveredBy("breaking", emptyList()))
    }
}
