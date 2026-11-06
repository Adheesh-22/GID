package com.adheesh.gid.feature.marks.domain

import kotlin.math.max

/** One uploaded mark, as far as the sums care. */
data class MarkInput(
    val maxMark       : Double,
    val weightage     : Double,
    val scoredMark    : Double?,
    val weightageMark : Double?,
) {
    /** VTOP's weighted mark, or worked out from the raw mark if the page left it out. */
    val earned: Double get() = weightageMark
        ?: scoredMark?.let { if (maxMark > 0) it / maxMark * weightage else 0.0 }
        ?: 0.0
}

/** One component (theory or lab), each out of 100. */
data class ComponentMarks(
    /** Weighted marks uploaded so far. */
    val uploaded  : Double,
    /** How much of the 100 those marks were out of. */
    val counted   : Double,
    /** How much of the 100 hasn't been uploaded yet. */
    val left      : Double,
    /** The student's expectation, kept within what is left. */
    val expected  : Double,
    /** Uploaded plus expected. */
    val projected : Double,
)

data class SubjectMarks(
    /** Each component's share of the subject, same order as given; adds up to 1. */
    val shares    : List<Double>,
    /** Out of 100, counting only uploaded marks. */
    val uploaded  : Double,
    /** Out of 100, with expectations included. */
    val projected : Double,
)

// ── Marks maths (SPEC §2.15) ─────────────────────────────────────────────────
// A component is out of 100. A subject's total follows credits: theory 3 and lab 1
// is 75% and 25%, equal credits are 50-50, a lab with no theory is all lab.
object MarksMath {

    const val FULL_MARKS = 100.0

    fun component(entries: List<MarkInput>, expected: Double?): ComponentMarks {
        val uploaded = entries.sumOf { it.earned }
        val counted = entries.sumOf { it.weightage }
        val left = max(0.0, FULL_MARKS - counted)
        val hoped = (expected ?: 0.0).coerceIn(0.0, left)
        return ComponentMarks(uploaded, counted, left, hoped, uploaded + hoped)
    }

    /** A part with no credits known counts as one credit, so it still has a share. */
    fun subject(parts: List<Pair<Double?, ComponentMarks>>): SubjectMarks {
        if (parts.isEmpty()) return SubjectMarks(emptyList(), 0.0, 0.0)
        val credits = parts.map { (c, _) -> c?.takeIf { it > 0 } ?: 1.0 }
        val total = credits.sum()
        val shares = credits.map { it / total }
        return SubjectMarks(
            shares    = shares,
            uploaded  = parts.indices.sumOf { shares[it] * parts[it].second.uploaded },
            projected = parts.indices.sumOf { shares[it] * parts[it].second.projected },
        )
    }
}
