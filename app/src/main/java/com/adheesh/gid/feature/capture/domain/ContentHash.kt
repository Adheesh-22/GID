package com.adheesh.gid.feature.capture.domain

import java.io.InputStream
import java.security.MessageDigest

// ── File fingerprints (SPEC §2.8) ────────────────────────────────────────────
// Two files with the same SHA-256 have the same contents, so a PDF shared
// twice can reuse the copy GID already has instead of storing it again.
object ContentHash {

    fun newDigest(): MessageDigest = MessageDigest.getInstance("SHA-256")

    /** Lower-case hex of a finished digest. */
    fun hex(digest: MessageDigest): String =
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    /** Reads [input] to the end; the caller closes it. */
    fun of(input: InputStream): String {
        val digest = newDigest()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return hex(digest)
    }
}
