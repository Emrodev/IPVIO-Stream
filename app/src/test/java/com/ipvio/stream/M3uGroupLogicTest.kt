package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #145: categories of an M3U list that are not written as group-title="..." (pure, no Android). */
class M3uGroupLogicTest {

    @Test fun usualSpelling_isUnchanged_andKeepsSpaces() {
        assertEquals(" FR | SPORTS ", M3uGroupLogic.fromInfo("""#EXTINF:-1 tvg-id="a" group-title=" FR | SPORTS ",Name"""))
    }

    @Test fun otherSpellings_areFound() {
        assertEquals("News", M3uGroupLogic.fromInfo("""#EXTINF:-1 GROUP-TITLE="News",X"""))
        assertEquals("News", M3uGroupLogic.fromInfo("#EXTINF:-1 group-title='News',X"))
        assertEquals("News", M3uGroupLogic.fromInfo("""#EXTINF:-1 tvg-group="News",X"""))
    }

    @Test fun groupTitleWins_overTvgGroup_andBlankIsSkipped() {
        assertEquals("A", M3uGroupLogic.fromInfo("""#EXTINF:-1 tvg-group="B" group-title="A",X"""))
        assertEquals("B", M3uGroupLogic.fromInfo("""#EXTINF:-1 group-title="" tvg-group="B",X"""))
        assertNull(M3uGroupLogic.fromInfo("""#EXTINF:-1 group-title="  ",X"""))
        assertNull(M3uGroupLogic.fromInfo("#EXTINF:-1,X"))
    }

    @Test fun extGrpLine() {
        assertEquals("Sport", M3uGroupLogic.fromExtGrp("#EXTGRP:Sport"))
        assertEquals("Sport", M3uGroupLogic.fromExtGrp("#extgrp:  Sport "))
        assertNull(M3uGroupLogic.fromExtGrp("#EXTGRP:"))
        assertNull(M3uGroupLogic.fromExtGrp("#EXTINF:-1,X"))
    }

    @Test fun parser_usesExtGrpAndTvgGroup_andKeepsSonstigeAsLastResort() {
        val list = """
            #EXTM3U
            #EXTINF:-1,Eins HD
            #EXTGRP:Allgemein
            http://s.tv/live/u/p/1.ts
            #EXTINF:-1 tvg-group="Sport",Zwei HD
            http://s.tv/live/u/p/2.ts
            #EXTGRP:Kinder
            #EXTINF:-1,Drei HD
            http://s.tv/live/u/p/3.ts
            #EXTINF:-1 group-title="Musik" ,Vier HD
            #EXTGRP:Ignored
            http://s.tv/live/u/p/4.ts
        """.trimIndent()
        val g = M3uParser.parse(list).associate { it.title to it.group }
        assertEquals("Allgemein", g["Eins HD"])
        assertEquals("Sport", g["Zwei HD"])
        assertEquals("Kinder", g["Drei HD"])
        assertEquals("Musik", g["Vier HD"])
    }

    @Test fun parser_withoutAnyGroup_staysInTheCatchAllCategory() {
        val g = M3uParser.parse("#EXTM3U\n#EXTINF:-1,Eins HD\nhttp://s.tv/live/u/p/1.ts").first().group
        assertEquals("Sonstige", g)
    }
}
