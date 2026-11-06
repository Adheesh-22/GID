package com.adheesh.gid.feature.semesters.data

import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SemesterEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import kotlinx.coroutines.flow.Flow

/** Names like 'Fall Semester 2026-27': a shorter label, and their order in time. */
object SemesterNames {

    private val parts = Regex("""(Fall|Winter|Summer).*?(\d{4})-\d{2}""")

    /** Fall 2026-27 = 20261, Winter 2026-27 = 20262; anything else sorts last. */
    fun sortKey(name: String): Int {
        val match = parts.find(name) ?: return 0
        val term = when (match.groupValues[1]) { "Fall" -> 1; "Winter" -> 2; else -> 3 }
        return match.groupValues[2].toInt() * 10 + term
    }

    fun short(name: String): String = name.replace(" Semester", "")
}

// ── Semesters (SPEC §2.14) ───────────────────────────────────────────────────
class SemesterRepository(
    private val db    : GIDDatabase,
    private val prefs : GIDPreferences,
) {
    private val dao = db.semesterDao()

    /** Newest first. */
    val semesters: Flow<List<SemesterEntity>> = dao.observeAll()
    val viewedId: Flow<String?> = prefs.viewedSemesterId
    val runningId: Flow<String?> = prefs.runningSemesterId

    /** The semester with this name, created if missing. */
    suspend fun ensure(name: String): SemesterEntity {
        val trimmed = name.trim()
        return dao.getByName(trimmed) ?: run {
            val now = nowMillis()
            SemesterEntity(newId(), trimmed, SemesterNames.sortKey(trimmed), now, now).also { dao.upsert(it) }
        }
    }

    /** Null shows every semester. */
    suspend fun view(id: String?) = prefs.setViewedSemester(id)

    suspend fun setRunning(id: String?) = prefs.setRunningSemester(id)
}
