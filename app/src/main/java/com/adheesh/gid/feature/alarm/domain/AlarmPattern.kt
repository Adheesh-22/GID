package com.adheesh.gid.feature.alarm.domain

import kotlin.random.Random

// ── Pattern to stop an alarm (SPEC §2.6) ─────────────────────────────────────
// Dots on a 3×3 grid are numbered row by row:  0 1 2 / 3 4 5 / 6 7 8.
// A line that passes over a dot picks it up, as on a phone's lock screen, so a
// generated path only jumps over dots it has already used.
object AlarmPattern {

    const val SIZE   = 3
    const val LENGTH = 5
    private const val DOTS = SIZE * SIZE

    fun random(random: Random = Random, length: Int = LENGTH): List<Int> {
        while (true) {
            val path = mutableListOf(random.nextInt(DOTS))
            while (path.size < length) {
                val options = (0 until DOTS).filter { it !in path && canJoin(path.last(), it, path) }
                if (options.isEmpty()) break
                path += options[random.nextInt(options.size)]
            }
            if (path.size == length) return path
        }
    }

    /** The dot exactly halfway between two dots, if there is one. */
    fun between(a: Int, b: Int): Int? {
        val rowSum = a / SIZE + b / SIZE
        val colSum = a % SIZE + b % SIZE
        if (rowSum % 2 != 0 || colSum % 2 != 0) return null
        return (rowSum / 2 * SIZE + colSum / 2).takeIf { it != a && it != b }
    }

    /**
     * Adds [dot] to a path being drawn, with any unused dot the line passed over
     * first. A dot already in the path is ignored.
     */
    fun extend(path: List<Int>, dot: Int): List<Int> {
        if (dot in path) return path
        val passed = path.lastOrNull()?.let { between(it, dot) }?.takeIf { it !in path }
        return path + listOfNotNull(passed) + dot
    }

    private fun canJoin(from: Int, to: Int, used: List<Int>): Boolean {
        val middle = between(from, to) ?: return true
        return middle in used
    }
}
