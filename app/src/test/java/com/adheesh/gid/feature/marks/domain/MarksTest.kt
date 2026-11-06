package com.adheesh.gid.feature.marks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MarksMathTest {

    private val delta = 0.0001

    private fun mark(weight: Double, earned: Double) = MarkInput(50.0, weight, earned, earned)

    @Test fun `a component adds uploaded marks and what is left`() {
        val c = MarksMath.component(listOf(mark(15.0, 12.0), mark(15.0, 11.1), mark(30.0, 27.0)), expected = 30.0)
        assertEquals(50.1, c.uploaded, delta)
        assertEquals(60.0, c.counted, delta)
        assertEquals(40.0, c.left, delta)
        assertEquals(30.0, c.expected, delta)
        assertEquals(80.1, c.projected, delta)
    }

    @Test fun `an expectation can't go above what is left`() {
        val c = MarksMath.component(listOf(mark(60.0, 50.0)), expected = 999.0)
        assertEquals(40.0, c.expected, delta)
        assertEquals(90.0, c.projected, delta)
    }

    @Test fun `no expectation counts as nothing, and nothing uploaded leaves all 100`() {
        val c = MarksMath.component(emptyList(), expected = null)
        assertEquals(100.0, c.left, delta)
        assertEquals(0.0, c.projected, delta)
    }

    @Test fun `a missing weighted mark is worked out from the raw mark`() {
        assertEquals(7.5, MarkInput(20.0, 10.0, 15.0, null).earned, delta)
        assertEquals(0.0, MarkInput(20.0, 10.0, null, null).earned, delta)
    }

    private fun scored(mark: Double) = MarksMath.component(listOf(mark(100.0, mark)), null)

    @Test fun `three theory credits and one lab credit is 75-25`() {
        val s = MarksMath.subject(listOf(3.0 to scored(80.0), 1.0 to scored(100.0)))
        assertEquals(listOf(0.75, 0.25), s.shares)
        assertEquals(85.0, s.projected, delta)
    }

    @Test fun `equal credits are 50-50`() {
        val s = MarksMath.subject(listOf(2.0 to scored(70.0), 2.0 to scored(90.0)))
        assertEquals(listOf(0.5, 0.5), s.shares)
        assertEquals(80.0, s.projected, delta)
    }

    @Test fun `lab only is all lab`() {
        val s = MarksMath.subject(listOf(2.0 to scored(91.0)))
        assertEquals(listOf(1.0), s.shares)
        assertEquals(91.0, s.projected, delta)
    }

    @Test fun `unknown credits share equally`() {
        assertEquals(listOf(0.5, 0.5), MarksMath.subject(listOf(null to scored(1.0), null to scored(2.0))).shares)
    }

    @Test fun `uploaded total leaves out expectations`() {
        val theory = MarksMath.component(listOf(mark(60.0, 40.0)), expected = 30.0)
        val s = MarksMath.subject(listOf(3.0 to theory, 1.0 to scored(100.0)))
        assertEquals(0.75 * 40 + 25, s.uploaded, delta)
        assertEquals(0.75 * 70 + 25, s.projected, delta)
    }
}

class VtopMarksParserTest {

    private val page = """
        VIT
        Marks View
        Select Semester

        Fall Semester 2026-27

        Sl.No.	ClassNbr	Course Code	Course Title	Course Type	Course System	Faculty	Slot	Course Mode
        1	VL2026270100124	BASTS101	Qualitative and Quantitative Skills Practice I	Soft Skill	ACE	TEACHER (APT)	D2+TD2	SBC01
        Sl.No.	Mark Title	Max. Mark	Weightage %	Status	Scored Mark	Weightage Mark	Remark
        1	Assessment - 1	15	15	Present	14.0	14
        3	Continuous Assessment Test - I	30	15	Present	26.0	13
        2	VL2026270103897	BACSE202	Database Systems	Embedded Lab	ACE	A TEACHER	L19+L20	LBC02
        Sl.No.	Mark Title	Max. Mark	Weightage %	Status	Scored Mark	Weightage Mark	Remark
        1	Assessment - 1	10	10	Present	9.5	9.5	1(c) (-0.5): operator precedence incorrect.
        2	Quiz - II	20	10	Absent
        3	VL2026270103350	BACSE291	Innovative Design Project	Project	ACE	ACADEMICS	NIL	PBC03
        Sl.No.	Mark Title	Max. Mark	Weightage %	Status	Scored Mark	Weightage Mark	Remark
        1	Review - II	40	40	Present	39.0	39	Steady progress
    """.trimIndent()

    @Test fun `reads the semester and every course in order`() {
        val m = VtopMarksParser.parse(page)!!
        assertEquals("Fall Semester 2026-27", m.semester)
        assertEquals(listOf("BASTS101", "BACSE202", "BACSE291"), m.courses.map { it.code })
        assertEquals(listOf("Soft Skill", "Embedded Lab", "Project"), m.courses.map { it.type })
        assertEquals("Database Systems", m.courses[1].title)
        assertEquals("VL2026270103897", m.courses[1].classNumber)
    }

    @Test fun `reads each mark, with a title that has a number in it`() {
        val row = VtopMarksParser.parse(page)!!.courses[0].rows[0]
        assertEquals("Assessment - 1", row.title)
        assertEquals(15.0, row.maxMark, 0.0)
        assertEquals(15.0, row.weightage, 0.0)
        assertEquals(14.0, row.scoredMark!!, 0.0)
        assertEquals(14.0, row.weightageMark!!, 0.0)
        assertEquals("", row.remark)
    }

    @Test fun `a remark that starts with a digit stays a remark`() {
        val row = VtopMarksParser.parse(page)!!.courses[1].rows[0]
        assertEquals(9.5, row.weightageMark!!, 0.0)
        assertEquals("1(c) (-0.5): operator precedence incorrect.", row.remark)
    }

    @Test fun `an absence has no marks`() {
        val row = VtopMarksParser.parse(page)!!.courses[1].rows[1]
        assertEquals("Absent", row.status)
        assertNull(row.scoredMark)
        assertNull(row.weightageMark)
    }

    @Test fun `works when tabs became spaces`() {
        val m = VtopMarksParser.parse(page.replace('\t', ' '))!!
        assertEquals(3, m.courses.size)
        assertEquals("Continuous Assessment Test - I", m.courses[0].rows[1].title)
        assertEquals(13.0, m.courses[0].rows[1].weightageMark!!, 0.0)
    }

    @Test fun `text with no table is rejected`() {
        assertNull(VtopMarksParser.parse("hello"))
    }
}

class VtopAssessmentParserTest {

    private val page = """
        Assignment Upload
        Semester	Course Code	Course Title	Course Type	Class Number
        Fall Semester 2026-27	BACSE102	Problem Solving using Java	LO	VL2026270103134

        Notes:
        1. File size (Max. upto 4MB)
        2. File type should be pdf,xls,xlsx,doc,docx

        Sl.No.	Title	Max. Mark	Weightage %	Due Date	QP	Document Details
        1	Assessment - 1	10	10	29-Jul-2026		File Not Uploaded
        -
        2	Assessment - 2	10	10	24-Aug-2026		24 Aug 2026 09:36 PM
        ....
        Semester	Course Code	Course Title	Course Type	Class Number
        Fall Semester 2026-27	BAFRE101	French Level I	ELA	VL2026270103735
        Sl.No.	Title	Max. Mark	Weightage %	Due Date	QP	Document Details
        1	Assessment - 1	10	10	04-Aug-2026		30 Jul 2026 06:23 AM
    """.trimIndent()

    @Test fun `reads each course and its assessments, across pages`() {
        val courses = VtopAssessmentParser.parse(page)!!
        assertEquals(listOf("Problem Solving using Java", "French Level I"), courses.map { it.title })
        assertEquals("Fall Semester 2026-27", courses[0].semester)
        assertEquals("VL2026270103134", courses[0].classNumber)
        assertEquals(2, courses[0].items.size)
        assertEquals(LocalDate.of(2026, 7, 29).toEpochDay(), courses[0].items[0].dueEpochDay)
    }

    @Test fun `upload status comes from the page`() {
        val items = VtopAssessmentParser.parse(page)!![0].items
        assertFalse(items[0].uploaded)
        assertTrue(items[1].uploaded)
    }

    @Test fun `the course list page has nothing to import`() {
        val list = """
            Select Semester
            Winter Semester 2025-26
            1	VL2025260501465	BACSE104	Structured and Object-Oriented Programming	ETH	A TEACHER - SCOPE
        """.trimIndent()
        assertNull(VtopAssessmentParser.parse(list))
    }

    @Test fun `the same page pasted twice counts once`() {
        assertEquals(2, VtopAssessmentParser.parse(page + "\n" + page)!!.size)
    }
}
