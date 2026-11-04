package com.adheesh.gid.feature.timetable.domain

/** One class from VTOP: one course, one day, one sitting. */
data class VtopClass(
    val courseCode   : String,
    val courseName   : String,
    /** 1 = Monday … 7 = Sunday. */
    val dayOfWeek    : Int,
    val startMinutes : Int,
    val endMinutes   : Int,
    val venue        : String,
    val isLab        : Boolean,
)

data class VtopTimetable(
    /** E.g. 'Winter Semester 2026-27', when the paste includes it. */
    val semester       : String?,
    val classes        : List<VtopClass>,
    /** Courses in the list with no slot in the grid, such as projects and MOOCs. */
    val withoutClasses : List<String>,
)

// ── VIT VTOP time table page (SPEC §2.7) ─────────────────────────────────────
// Reads the page as words, not lines or tabs, because copying from a phone
// browser can change both. The grid has two time rows per kind:
//   THEORY Start 08:00 … End 08:50 …  LAB Start 08:00 … End 08:50 …
// then for each day: MON THEORY <cells> LAB <cells>. A cell's column gives its
// time, and a registered cell looks like A2-BACSE202-ETH-PRP576-ALL.
object VtopParser {

    private val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private const val THEORY = "THEORY"
    private const val LAB    = "LAB"
    private const val START  = "Start"
    private const val END    = "End"

    private val whitespace = Regex("""\s+""")
    private val semesterName = Regex("""(?:Fall|Winter|Summer)[A-Za-z ]*Semester\s*\d{4}-\d{2}""")
    // 'BACSE202 - Database Systems'; the name stops at a tab or '( Embedded Theory )'.
    private val courseLine = Regex("""\b([A-Z]{2,}\d{2,}[A-Z]?)\s+-\s+([^\t\r\n(]+)""")
    private val courseCode = Regex("""[A-Z]{2,}\d{2,}[A-Z]?""")
    private val clock = Regex("""(\d{1,2}):(\d{2})""")

    /** Null when the text has no time table grid. */
    fun parse(text: String): VtopTimetable? {
        val words = text.trim().split(whitespace)

        val theoryAt = indexOfPair(words, THEORY, START) ?: return null
        val theoryEndAt = words.indexOfFrom(END, theoryAt) ?: return null
        val labAt = indexOfPair(words, LAB, START, from = theoryEndAt) ?: return null
        val labEndAt = words.indexOfFrom(END, labAt) ?: return null
        val firstDayAt = (labEndAt until words.size).firstOrNull { words[it] in DAYS } ?: return null

        val theoryStarts = times(words.subList(theoryAt + 2, theoryEndAt))
        val theoryEnds   = times(words.subList(theoryEndAt + 1, labAt))
        val labStarts    = times(words.subList(labAt + 2, labEndAt))
        val labEnds      = times(words.subList(labEndAt + 1, firstDayAt))

        val names = courseNames(text)
        val classes = mutableListOf<VtopClass>()

        var i = firstDayAt
        while (i < words.size) {
            val day = DAYS.indexOf(words[i]) + 1
            if (day == 0) { i++; continue }
            val theoryRow = words.getOrNull(i + 1) == THEORY
            val labRowAt = if (theoryRow) words.indexOfFrom(LAB, i + 2) else null
            if (labRowAt == null) { i++; continue }
            val nextDayAt = (labRowAt + 1 until words.size).firstOrNull { words[it] in DAYS } ?: words.size

            // Anything after the last column is page footer, not cells.
            val theoryCells = words.subList(i + 2, labRowAt).take(theoryStarts.size)
            val labCells    = words.subList(labRowAt + 1, nextDayAt).take(labStarts.size)
            classes += row(theoryCells, theoryStarts, theoryEnds, day, isLab = false, names)
            classes += row(labCells, labStarts, labEnds, day, isLab = true, names)
            i = nextDayAt
        }
        if (classes.isEmpty()) return null

        val withSlots = classes.map { it.courseCode }.toSet()
        return VtopTimetable(
            semester       = semesterName.find(text)?.value?.replace(whitespace, " "),
            classes        = classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startMinutes })),
            withoutClasses = names.filterKeys { it !in withSlots }.values.toList(),
        )
    }

    /** One grid row: cells at neighbouring columns of one course become one class (L3+L4). */
    private fun row(
        cells  : List<String>,
        starts : List<Int?>,
        ends   : List<Int?>,
        day    : Int,
        isLab  : Boolean,
        names  : Map<String, String>,
    ): List<VtopClass> {
        val result = mutableListOf<VtopClass>()
        var lastColumn = -2
        cells.forEachIndexed { column, cell ->
            val parts = cell.split('-')
            if (parts.size < 4 || !courseCode.matches(parts[1])) return@forEachIndexed
            val start = starts.getOrNull(column) ?: return@forEachIndexed
            val end   = ends.getOrNull(column) ?: return@forEachIndexed
            val code  = parts[1]
            val venue = parts[3]
            val previous = result.lastOrNull()
            if (previous != null && lastColumn == column - 1 && previous.courseCode == code && previous.venue == venue) {
                result[result.lastIndex] = previous.copy(endMinutes = end)
            } else {
                result += VtopClass(code, names[code] ?: code, day, start, end, venue, isLab)
            }
            lastColumn = column
        }
        return result
    }

    /** Course code to name, from the course list above the grid. */
    private fun courseNames(text: String): Map<String, String> =
        courseLine.findAll(text).associate { it.groupValues[1] to it.groupValues[2].trim() }

    /** One entry per column; null for '-' and 'Lunch', so columns stay lined up. */
    private fun times(cells: List<String>): List<Int?> = cells.map { cell ->
        clock.matchEntire(cell)?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
    }

    private fun indexOfPair(words: List<String>, first: String, second: String, from: Int = 0): Int? =
        (from until words.size - 1).firstOrNull { words[it] == first && words[it + 1] == second }

    private fun List<String>.indexOfFrom(word: String, from: Int): Int? =
        (from until size).firstOrNull { this[it] == word }
}
