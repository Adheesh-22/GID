package com.adheesh.gid.feature.timetable.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VtopParserTest {

    private val timeRows = """
        THEORY	Start	08:00	09:00	10:00	11:00	12:00	-	Lunch	14:00	15:00	16:00	17:00	18:00	18:51	19:01
        End	08:50	09:50	10:50	11:50	12:50	-	Lunch	14:50	15:50	16:50	17:50	18:50	19:00	19:50
        LAB	Start	08:00	08:51	09:51	10:41	11:40	12:31	Lunch	14:00	14:51	15:51	16:41	17:40	18:31	-
        End	08:50	09:40	10:40	11:30	12:30	13:20	Lunch	14:50	15:40	16:40	17:30	18:30	19:20	-
    """.trimIndent()

    // The grid of a real Winter Semester 2026-27 page.
    private val winter = """
        Winter Semester 2026-27

        BACSE201 - Models of Computation

        ( Theory Only )

        BACSE204 - Software Engineering

        ( Embedded Theory )

        BACSE205 - Fundamentals of Artificial Intelligence and Machine Learning

        BACSE301 - Exploratory Data Analysis

        BACSE325 - Full Stack Development

        BAHUM103 - Ethics and Values

        BASTS102 - Qualitative and Quantitative Skills Practice II

        $timeRows
        MON	THEORY	A1	F1	D1	TB1	TG1	-	Lunch	A2-BACSE325-ETH-PRP309-ALL	F2-BAHUM103-TH-PRP323-ALL	D2-BASTS102-SS-PRP322-ALL	TB2-BACSE301-ETH-PRP322-ALL	TG2-BACSE205-ETH-PRP323-ALL	-	V3
        LAB	L1-BACSE204-ELA-PRP353-ALL	L2-BACSE204-ELA-PRP353-ALL	L3	L4	L5	L6	Lunch	L31	L32	L33	L34	L35	L36	-
        TUE	THEORY	B1	G1	E1	TC1	TAA1	-	Lunch	B2-BACSE301-ETH-PRP322-ALL	G2-BACSE205-ETH-PRP323-ALL	E2-BACSE204-ETH-PRP304-ALL	TC2-BACSE201-TH-PRP304-ALL	TAA2	-	V4
        LAB	L7-BACSE301-ELA-PRP354-ALL	L8-BACSE301-ELA-PRP354-ALL	L9	L10	L11-BACSE325-ELA-PRP137-ALL	L12-BACSE325-ELA-PRP137-ALL	Lunch	L37	L38	L39	L40	L41	L42	-
        WED	THEORY	C1	A1	F1	V1	V2	-	Lunch	C2-BACSE201-TH-PRP304-ALL	A2-BACSE325-ETH-PRP309-ALL	F2-BAHUM103-TH-PRP323-ALL	TD2-BASTS102-SS-PRP322-ALL	TBB2	-	V5
        LAB	L13	L14	L15-BACSE325-ELA-PRP137-ALL	L16-BACSE325-ELA-PRP137-ALL	L17	L18	Lunch	L43	L44	L45	L46	L47	L48	-
        THU	THEORY	D1	B1	G1	TE1	TCC1	-	Lunch	D2-BASTS102-SS-PRP322-ALL	B2-BACSE301-ETH-PRP322-ALL	G2-BACSE205-ETH-PRP323-ALL	TE2-BACSE204-ETH-PRP304-ALL	TCC2-BACSE201-TH-PRP304-ALL	-	V6
        LAB	L19-BACSE205-ELA-SJT517-ALL	L20-BACSE205-ELA-SJT517-ALL	L21	L22	L23	L24	Lunch	L49	L50	L51	L52	L53	L54	-
        FRI	THEORY	E1	C1	TA1	TF1	TD1	-	Lunch	E2-BACSE204-ETH-PRP304-ALL	C2-BACSE201-TH-PRP304-ALL	TA2	TF2	TDD2	-	V7
        LAB	L25	L26	L27	L28	L29	L30	Lunch	L55	L56	L57	L58	L59	L60	-
        SAT	THEORY	V8	X11	X12	Y11	Y12	-	Lunch	X21	Z21	Y21	W21	W22	-	V9
        LAB	L71	L72	L73	L74	L75	L76	Lunch	L77	L78	L79	L80	L81	L82	-
        SUN	THEORY	V10	Y11	Y12	X11	X12	-	Lunch	Y21	Z21	X21	W21	W22	-	V11
        LAB	L83	L84	L85	L86	L87	L88	Lunch	L89	L90	L91	L92	L93	L94	-
    """.trimIndent()

    // The grid of a real Fall Semester 2026-27 page, plus courses with no slot.
    private val fall = """
        Fall Semester 2026-27

        BACSE102 - Problem Solving using Java

        ( Lab Only )

        BACSE202 - Database Systems

        BACSE203 - Computer Networks

        BACSE291 - Innovative Design Project

        ( Project )

        BACSE324 - Embedded Systems Architecture

        BAFRE101 - French Level I

        BAHUM110 - Sustainability and Society

        BAMAT207 - Probability and Statistics

        BASTS101 - Qualitative and Quantitative Skills Practice I

        ACFOC118 - Psychology of Stress, Health and Well-being

        $timeRows
        MON	THEORY	A1	F1	D1	TB1	TG1	-	Lunch	A2-BACSE202-ETH-PRP576-ALL	F2-BAMAT207-ETH-PRP268-ALL	D2-BASTS101-SS-PRP464-ALL	TB2-BACSE203-ETH-PRP268-ALL	TG2-BAFRE101-ETH-SMV104-ALL	-	V3
        LAB	L1	L2	L3-BACSE102-LO-PRP136-ALL	L4-BACSE102-LO-PRP136-ALL	L5	L6	Lunch	L31	L32	L33	L34	L35	L36	-
        TUE	THEORY	B1	G1	E1	TC1	TAA1	-	Lunch	B2-BACSE203-ETH-PRP268-ALL	G2	E2-BACSE324-ETH-PRP471-ALL	TC2-BAHUM110-TH-PRP772-ALL	TAA2	-	V4
        LAB	L7-BAFRE101-ELA-PRP555-ALL	L8-BAFRE101-ELA-PRP555-ALL	L9	L10	L11	L12	Lunch	L37	L38	L39	L40	L41	L42	-
        WED	THEORY	C1	A1	F1	V1	V2	-	Lunch	C2-BAHUM110-TH-PRP772-ALL	A2-BACSE202-ETH-PRP576-ALL	F2-BAMAT207-ETH-PRP268-ALL	TD2-BASTS101-SS-PRP464-ALL	TBB2	-	V5
        LAB	L13-BACSE102-LO-PRP136-ALL	L14-BACSE102-LO-PRP136-ALL	L15	L16	L17	L18	Lunch	L43	L44	L45	L46	L47	L48	-
        THU	THEORY	D1	B1	G1	TE1	TCC1	-	Lunch	D2-BASTS101-SS-PRP464-ALL	B2-BACSE203-ETH-PRP268-ALL	G2	TE2-BACSE324-ETH-PRP471-ALL	TCC2	-	V6
        LAB	L19-BACSE202-ELA-PRP138-ALL	L20-BACSE202-ELA-PRP138-ALL	L21-BACSE324-ELA-SJT515-ALL	L22-BACSE324-ELA-SJT515-ALL	L23	L24	Lunch	L49	L50	L51	L52	L53	L54	-
        FRI	THEORY	E1	C1	TA1	TF1	TD1	-	Lunch	E2-BACSE324-ETH-PRP471-ALL	C2-BAHUM110-TH-PRP772-ALL	TA2-BACSE202-ETH-PRP576-ALL	TF2-BAMAT207-ETH-PRP268-ALL	TDD2	-	V7
        LAB	L25-BAMAT207-ELA-PRP446-ALL	L26-BAMAT207-ELA-PRP446-ALL	L27	L28	L29-BACSE203-ELA-SJT516-ALL	L30-BACSE203-ELA-SJT516-ALL	Lunch	L55	L56	L57	L58	L59	L60	-
        SAT	THEORY	V8	X11	X12	Y11	Y12	-	Lunch	X21	Z21	Y21	W21	W22	-	V9
        LAB	L71	L72	L73	L74	L75	L76	Lunch	L77	L78	L79	L80	L81	L82	-
        SUN	THEORY	V10	Y11	Y12	X11	X12	-	Lunch	Y21	Z21	X21	W21	W22	-	V11
        LAB	L83	L84	L85	L86	L87	L88	Lunch	L89	L90	L91	L92	L93	L94	-
    """.trimIndent()

    private fun minutes(hour: Int, minute: Int) = hour * 60 + minute

    @Test fun `winter page gives every class with VTOP times`() {
        val t = VtopParser.parse(winter)!!
        assertEquals("Winter Semester 2026-27", t.semester)
        assertEquals(25, t.classes.size)

        val monday = t.classes.filter { it.dayOfWeek == 1 }
        // The lab comes first: L1+L2 at 08:00–09:40, merged into one class.
        val lab = monday.first()
        assertEquals("Software Engineering", lab.courseName)
        assertTrue(lab.isLab)
        assertEquals("PRP353", lab.venue)
        assertEquals(minutes(8, 0), lab.startMinutes)
        assertEquals(minutes(9, 40), lab.endMinutes)

        val a2 = monday.first { it.courseCode == "BACSE325" }
        assertEquals("Full Stack Development", a2.courseName)
        assertEquals(minutes(14, 0), a2.startMinutes)
        assertEquals(minutes(14, 50), a2.endMinutes)
        assertEquals("PRP309", a2.venue)
    }

    @Test fun `fall page counts classes and lists courses with no slot`() {
        val t = VtopParser.parse(fall)!!
        assertEquals("Fall Semester 2026-27", t.semester)
        assertEquals(26, t.classes.size)
        assertEquals(listOf("Innovative Design Project", "Psychology of Stress, Health and Well-being"), t.withoutClasses)

        // Thursday has two different labs side by side; they must stay separate.
        val thursdayLabs = t.classes.filter { it.dayOfWeek == 4 && it.isLab }
        assertEquals(listOf("Database Systems", "Embedded Systems Architecture"), thursdayLabs.map { it.courseName })
        assertEquals(minutes(8, 0), thursdayLabs[0].startMinutes)
        assertEquals(minutes(9, 40), thursdayLabs[0].endMinutes)
        assertEquals(minutes(9, 51), thursdayLabs[1].startMinutes)
        assertEquals(minutes(11, 30), thursdayLabs[1].endMinutes)
    }

    @Test fun `nothing is scheduled on the free weekend`() {
        assertTrue(VtopParser.parse(winter)!!.classes.none { it.dayOfWeek >= 6 })
    }

    @Test fun `works when the browser turns tabs and new lines into spaces`() {
        val flattened = winter.replace(Regex("""[\t\r\n]+"""), " ")
        val t = VtopParser.parse(flattened)
        assertNotNull(t)
        assertEquals(25, t!!.classes.size)
        assertEquals("Models of Computation", t.classes.first { it.courseCode == "BACSE201" }.courseName)
    }

    @Test fun `a course missing from the list falls back to its code`() {
        val gridOnly = winter.lineSequence().dropWhile { !it.trimStart().startsWith("THEORY") }.joinToString("\n")
        val t = VtopParser.parse(gridOnly)!!
        assertNull(t.semester)
        assertTrue(t.classes.any { it.courseName == "BACSE201" })
    }

    @Test fun `text without a grid is rejected`() {
        assertNull(VtopParser.parse("hello world"))
        assertNull(VtopParser.parse("BACSE201 - Models of Computation"))
        assertNull(VtopParser.parse(""))
    }

    @Test fun `credits come from the course list`() {
        val page = """
            Winter Semester 2025-26

            BACSE104 - Structured and Object-Oriented Programming

            ( Embedded Theory )

            2 0 0 0 2.0

            Programme Core Courses

            BACSE104 - Structured and Object-Oriented Programming

            ( Embedded Lab )

            0 0 4 0 2.0

            BAHUM101 - India Studies

            ( Online Course )

            0 0 0 0 1.0

            $timeRows
            MON	THEORY	A1	F1	D1	TB1	TG1	-	Lunch	A2	F2	D2-BACSE104-ETH-PRP112-ALL	TB2	TG2	-	V3
            LAB	L1	L2	L3	L4	L5	L6	Lunch	L31	L32	L33	L34	L35	L36	-
        """.trimIndent()
        val t = VtopParser.parse(page)!!
        assertEquals(
            listOf(
                VtopCourse("BACSE104", "Structured and Object-Oriented Programming", "Embedded Theory", 2.0),
                VtopCourse("BACSE104", "Structured and Object-Oriented Programming", "Embedded Lab", 2.0),
                VtopCourse("BAHUM101", "India Studies", "Online Course", 1.0),
            ),
            t.courses,
        )
    }
}
