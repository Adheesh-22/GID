package com.adheesh.gid.feature.appearance.domain

// ── Home and app look (SPEC §2.2) ────────────────────────────────────────────
// Plain Kotlin so it can be unit tested; the Compose fonts and colours for
// each option live in feature/appearance/ui.

enum class NameFont { GID, DM_SANS, SERIF, MONO, SCRIPT, LIGHT;
    companion object {
        fun of(value: String?): NameFont = entries.firstOrNull { it.name == value } ?: GID
    }
}

enum class NameColor { TEXT, CYAN, CORAL, MUSTARD, PERIWINKLE, SAGE, GREEN;
    companion object {
        fun of(value: String?): NameColor = entries.firstOrNull { it.name == value } ?: TEXT
    }
}

data class Appearance(
    /** The name the user typed for Home, or null to use the profile's first name. */
    val homeName       : String?    = null,
    val font           : NameFont   = NameFont.GID,
    val color          : NameColor  = NameColor.TEXT,
    /** The blurred background photo, or null for the plain background. */
    val backgroundPath : String?    = null,
    /** 0 = photo as is, 0.85 = almost black. */
    val backgroundDim  : Float      = DEFAULT_DIM,
) {
    /** What Home greets: the typed name, else the first word of the profile name. */
    fun greetingName(profileName: String?): String? =
        homeName?.ifBlank { null } ?: profileName?.trim()?.substringBefore(' ')?.ifBlank { null }

    companion object {
        const val DEFAULT_DIM = 0.45f
        const val MAX_DIM     = 0.85f
    }
}

// ── Box blur ─────────────────────────────────────────────────────────────────
// Three box blurs in a row look almost the same as a Gaussian blur and work on
// every Android version (the built-in blur needs Android 12). Runs once, when
// the photo is chosen, on a small copy of it.
object BoxBlur {

    /** Blurs ARGB [pixels] (row by row, [width] × [height]) in place. */
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int, passes: Int = 3) {
        require(pixels.size == width * height) { "pixels must be width × height" }
        if (radius < 1 || width == 0 || height == 0) return
        val scratch = IntArray(pixels.size)
        repeat(passes) {
            pass(pixels, scratch, lines = height, length = width, radius = radius) { line, i -> line * width + i }
            pass(scratch, pixels, lines = width, length = height, radius = radius) { line, i -> i * width + line }
        }
    }

    /** One running-average pass along every row (or column); edges repeat the last pixel. */
    private inline fun pass(
        src: IntArray, dst: IntArray, lines: Int, length: Int, radius: Int,
        index: (line: Int, i: Int) -> Int,
    ) {
        val size = 2 * radius + 1
        for (line in 0 until lines) {
            var a = 0; var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val p = src[index(line, i.coerceIn(0, length - 1))]
                a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until length) {
                dst[index(line, i)] = ((a / size) shl 24) or ((r / size) shl 16) or ((g / size) shl 8) or (b / size)
                val out = src[index(line, (i - radius).coerceIn(0, length - 1))]
                val inc = src[index(line, (i + radius + 1).coerceIn(0, length - 1))]
                a += (inc ushr 24) - (out ushr 24)
                r += ((inc shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inc shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inc and 0xFF) - (out and 0xFF)
            }
        }
    }
}
