package com.adheesh.gid.feature.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkExtractorTest {

    @Test fun `bare link`() {
        val r = LinkExtractor.extract("https://visualgo.net/en/graphds")
        assertEquals("https://visualgo.net/en/graphds", r.url)
        assertEquals("", r.text)
    }

    @Test fun `title on one line and link on the next`() {
        val r = LinkExtractor.extract("Graph visualiser\nhttps://visualgo.net/en/graphds")
        assertEquals("https://visualgo.net/en/graphds", r.url)
        assertEquals("Graph visualiser", r.text)
    }

    @Test fun `trailing punctuation is not part of the link`() {
        val r = LinkExtractor.extract("Use this for BFS: https://visualgo.net/en/dfsbfs.")
        assertEquals("https://visualgo.net/en/dfsbfs", r.url)
        assertEquals("Use this for BFS", r.text)
    }

    @Test fun `www links get https`() {
        assertEquals("https://www.geeksforgeeks.org/dbms", LinkExtractor.extract("www.geeksforgeeks.org/dbms").url)
    }

    @Test fun `plain text has no link`() {
        val r = LinkExtractor.extract("  Q3 from the 2023 paper is important  ")
        assertNull(r.url)
        assertEquals("Q3 from the 2023 paper is important", r.text)
    }

    @Test fun `fts query quotes each word as a prefix`() {
        assertEquals("\"graph\"* \"bfs\"*", LinkExtractor.ftsQuery("  graph \"bfs  "))
        assertEquals("", LinkExtractor.ftsQuery("   "))
    }
}
