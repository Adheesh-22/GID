package com.adheesh.gid.feature.syllabus.domain

// ── Syllabus rules (SPEC §2.13) ──────────────────────────────────────────────

/** A unit read from pasted text, with its topics in order. */
data class ParsedUnit(val name: String, val topics: List<String>)

/**
 * Turns a pasted syllabus into units and topics. A line starting with Unit,
 * Module or Chapter, or ending with ':', starts a unit; every other line is a
 * topic with its bullet or number removed. Topics before any unit go into
 * [GENERAL].
 */
object SyllabusParser {

    const val GENERAL = "General"

    private val unitStart = Regex("""^(unit|module|chapter)\b""", RegexOption.IGNORE_CASE)
    // '-', '*', '•', '1.', '2)', '3.1' and the like, followed by a space.
    private val bullet = Regex("""^([-*•·–]+|\d+(\.\d+)*[.)]?)\s+""")

    fun parse(text: String): List<ParsedUnit> {
        val units = mutableListOf<Pair<String, MutableList<String>>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (unitStart.containsMatchIn(line) || line.endsWith(':')) {
                val name = line.trimEnd(':').trim()
                if (name.isNotEmpty()) units += name to mutableListOf()
                continue
            }
            val topic = line.replace(bullet, "").trim()
            if (topic.isEmpty()) continue
            if (units.isEmpty()) units += GENERAL to mutableListOf()
            units.last().second += topic
        }
        return units.map { (name, topics) -> ParsedUnit(name, topics) }
    }
}

/** How the bar is coloured: on track, or behind with the exam coming up. */
enum class SyllabusLevel { NONE, ON_TRACK, AT_RISK, BEHIND }

data class SyllabusProgress(val done: Int, val total: Int) {

    /** Null when there are no topics yet. */
    val percent: Int? get() = if (total == 0) null else done * 100 / total

    /**
     * [daysLeft] until the exam, or null when there is no exam. Under
     * [ON_TRACK_PERCENT] counts as at risk within two weeks and behind within
     * one.
     */
    fun level(daysLeft: Long? = null): SyllabusLevel {
        val p = percent ?: return SyllabusLevel.NONE
        return when {
            p >= ON_TRACK_PERCENT || daysLeft == null || daysLeft > 14 -> SyllabusLevel.ON_TRACK
            daysLeft > 7 -> SyllabusLevel.AT_RISK
            else         -> SyllabusLevel.BEHIND
        }
    }

    companion object {
        const val ON_TRACK_PERCENT = 70
        val EMPTY = SyllabusProgress(0, 0)
    }
}
