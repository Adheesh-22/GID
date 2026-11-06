package com.adheesh.gid.feature.marks.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── VTOP's Marks View and Assignment Upload pages (SPEC §2.15, §2.16) ────────
// Both are tables copied from a browser, so each row is read as one line, whether
// the browser kept the tabs or turned them into spaces.

data class VtopMarkRow(
    val title         : String,
    val maxMark       : Double,
    val weightage     : Double,
    val status        : String,
    val scoredMark    : Double?,
    val weightageMark : Double?,
    val remark        : String,
)

data class VtopMarkCourse(
    val classNumber : String,
    val code        : String,
    val title       : String,
    val type        : String,
    val rows        : List<VtopMarkRow>,
)

data class VtopMarks(val semester: String?, val courses: List<VtopMarkCourse>)

data class VtopAssessment(
    val title        : String,
    val maxMark      : Double,
    val weightage    : Double,
    val dueEpochDay  : Long,
    /** VTOP shows when the file went up, or 'File Not Uploaded'. */
    val uploaded     : Boolean,
)

data class VtopAssessmentCourse(
    val semester    : String,
    val code        : String,
    val title       : String,
    val classNumber : String,
    val items       : List<VtopAssessment>,
)

private const val NUMBER = """\d+(?:\.\d+)?"""
private const val CODE = """[A-Z]{2,}\d{2,}[A-Z]?"""
private const val SEMESTER = """(?:Fall|Winter|Summer)[A-Za-z ]*Semester\s*\d{4}-\d{2}"""
private val SPACES = Regex("""\s+""")

object VtopMarksParser {

    private val semester = Regex(SEMESTER)
    private val course = Regex(
        """^\s*\d+\s+(VL\d+)\s+($CODE)\s+(.+?)\s+(Embedded Theory|Embedded Lab|Theory Only|Lab Only|Soft Skill|Project|Online Course)\b"""
    )
    private val mark = Regex(
        """^\s*\d+\s+(.+?)\s+($NUMBER)\s+($NUMBER)\s+([A-Za-z]+)(?:\s+($NUMBER))?(?:\s+($NUMBER))?\s*(.*)$"""
    )

    /** Null when the text has no marks table. */
    fun parse(text: String): VtopMarks? {
        val courses = mutableListOf<VtopMarkCourse>()
        for (line in text.lineSequence()) {
            val c = course.find(line)
            if (c != null) {
                val (classNumber, code, title, type) = c.destructured
                courses += VtopMarkCourse(classNumber, code, title.trim(), type, emptyList())
                continue
            }
            val current = courses.lastOrNull() ?: continue
            val m = mark.find(line) ?: continue
            val (title, max, weight, status, scored, weighted, remark) = m.destructured
            courses[courses.lastIndex] = current.copy(
                rows = current.rows + VtopMarkRow(
                    title         = title.trim(),
                    maxMark       = max.toDouble(),
                    weightage     = weight.toDouble(),
                    status        = status,
                    scoredMark    = scored.toDoubleOrNull(),
                    weightageMark = weighted.toDoubleOrNull(),
                    remark        = remark.trim(),
                )
            )
        }
        if (courses.isEmpty()) return null
        return VtopMarks(semester.find(text)?.value?.replace(SPACES, " "), courses)
    }
}

object VtopAssessmentParser {

    private val course = Regex("""($SEMESTER)\s+($CODE)\s+(.+?)\s+[A-Z]{2,4}\s+(VL\d+)""")
    private val row = Regex("""^\s*\d+\s+(.+?)\s+($NUMBER)\s+($NUMBER)\s+(\d{2}-[A-Za-z]{3}-\d{4})\s*(.*)$""")
    private val dueFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    private val uploadTime = Regex("""\d{1,2}\s+[A-Za-z]{3}\s+\d{4}""")

    /** One page or several pasted together. Null when there are no assessments in it. */
    fun parse(text: String): List<VtopAssessmentCourse>? {
        val courses = mutableListOf<VtopAssessmentCourse>()
        for (line in text.lineSequence()) {
            val c = course.find(line)
            if (c != null) {
                val (semester, code, title, classNumber) = c.destructured
                courses += VtopAssessmentCourse(semester.replace(SPACES, " "), code, title.trim(), classNumber, emptyList())
                continue
            }
            val current = courses.lastOrNull() ?: continue
            val r = row.find(line) ?: continue
            val (title, max, weight, due, rest) = r.destructured
            val day = runCatching { LocalDate.parse(due, dueFormat).toEpochDay() }.getOrNull() ?: continue
            courses[courses.lastIndex] = current.copy(
                items = current.items + VtopAssessment(
                    title       = title.trim(),
                    maxMark     = max.toDouble(),
                    weightage   = weight.toDouble(),
                    dueEpochDay = day,
                    uploaded    = uploadTime.containsMatchIn(rest),
                )
            )
        }
        // The same page pasted twice shouldn't count twice.
        val found = courses.filter { it.items.isNotEmpty() }.distinctBy { it.classNumber }
        return found.ifEmpty { null }
    }
}
