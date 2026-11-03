package com.adheesh.gid.feature.syllabus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusParserTest {

    @Test fun `unit lines start units and other lines become topics`() {
        val text = """
            Unit 1: Intro
            - ER model
            * Keys
            1. Relational algebra

            UNIT 2
            2.1 Functional dependencies
            1NF to 3NF
        """.trimIndent()
        val units = SyllabusParser.parse(text)
        assertEquals(listOf("Unit 1: Intro", "UNIT 2"), units.map { it.name })
        assertEquals(listOf("ER model", "Keys", "Relational algebra"), units[0].topics)
        // '1NF' is part of the topic, not a list number.
        assertEquals(listOf("Functional dependencies", "1NF to 3NF"), units[1].topics)
    }

    @Test fun `module, chapter and a trailing colon also start units`() {
        val units = SyllabusParser.parse("Module A\nx\nChapter 3\ny\nLab work:\nz")
        assertEquals(listOf("Module A", "Chapter 3", "Lab work"), units.map { it.name })
    }

    @Test fun `topics before any unit go into General`() {
        val units = SyllabusParser.parse("Sorting\n• Searching\nUnit 2\nTrees")
        assertEquals(SyllabusParser.GENERAL, units[0].name)
        assertEquals(listOf("Sorting", "Searching"), units[0].topics)
    }

    @Test fun `blank text gives nothing`() {
        assertTrue(SyllabusParser.parse("  \n\n ").isEmpty())
    }
}

class SyllabusProgressTest {

    @Test fun `percent and empty`() {
        assertEquals(58, SyllabusProgress(7, 12).percent)
        assertNull(SyllabusProgress.EMPTY.percent)
        assertEquals(SyllabusLevel.NONE, SyllabusProgress.EMPTY.level(3))
    }

    @Test fun `level depends on how close the exam is`() {
        val half = SyllabusProgress(5, 10)
        assertEquals(SyllabusLevel.ON_TRACK, half.level(null))
        assertEquals(SyllabusLevel.ON_TRACK, half.level(20))
        assertEquals(SyllabusLevel.AT_RISK, half.level(10))
        assertEquals(SyllabusLevel.BEHIND, half.level(3))
        assertEquals(SyllabusLevel.ON_TRACK, SyllabusProgress(7, 10).level(1))
    }
}
