package com.adheesh.gid.feature.settings.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// ── The one row we keep in the cloud (SPEC §6: public.profiles) ──────────────
// Column names match the SQL in the spec. Productivity data never goes here.
@Serializable
data class ProfileRow(
    val id                                  : String,
    val email                               : String? = null,
    @SerialName("full_name")     val fullName    : String? = null,
    @SerialName("date_of_birth") val dateOfBirth : String? = null,   // ISO yyyy-MM-dd
    val phone                               : String? = null,
    val institution                         : String? = null,
    @SerialName("updated_at")    val updatedAt   : String? = null,
)

/** Remote half of the profile. An interface so tests or V2 can swap it. */
interface ProfileRemoteSource {
    suspend fun fetch(userId: String): ProfileRow?
    suspend fun upsert(row: ProfileRow)
}

class SupabaseProfileRemoteSource(private val client: SupabaseClient?) : ProfileRemoteSource {

    override suspend fun fetch(userId: String): ProfileRow? {
        val c = client ?: return null
        return c.from(TABLE).select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileRow>()
    }

    override suspend fun upsert(row: ProfileRow) {
        val c = client ?: error("Supabase is not configured")
        // Row-level security only lets a user write the row whose id is theirs.
        c.from(TABLE).upsert(row.copy(updatedAt = Instant.now().toString()))
    }

    private companion object {
        const val TABLE = "profiles"
    }
}
