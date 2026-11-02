package com.adheesh.gid.feature.capture.domain

/** Shared text split into the link it contains and whatever words came with it. */
data class ExtractedText(
    val url  : String?,
    val text : String,
)

// ── Finding the link in shared text ──────────────────────────────────────────
// Apps share links in different shapes: "https://x.com", "Check this https://x.com",
// or a page title on one line and the URL on the next. We keep the first link
// as the capture's url and the remaining words as its title.
object LinkExtractor {

    private val urlRegex = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")

    fun extract(shared: String): ExtractedText {
        val match = urlRegex.find(shared) ?: return ExtractedText(url = null, text = shared.trim())
        // Sentence punctuation that ends up glued to a link isn't part of it.
        val raw = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
        val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
        val rest = shared.removeRange(match.range)
            .lines().joinToString(" ") { it.trim() }.trim()
            .trim('-', '–', ':', '|').trim()
        return ExtractedText(url = url, text = rest)
    }

    /** Escapes user input for an FTS MATCH: each word becomes a quoted prefix search. */
    fun ftsQuery(input: String): String =
        input.split(Regex("""\s+"""))
            .map { it.replace("\"", "") }
            .filter { it.isNotBlank() }
            .joinToString(" ") { "\"$it\"*" }
}
