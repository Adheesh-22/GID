package com.adheesh.gid.feature.capture.domain

// ── Files GID can keep (SPEC §2.8) ───────────────────────────────────────────
// Photos, PDFs, Word and PowerPoint. Some apps share a file as the generic
// 'application/octet-stream', so its name's extension decides then.
object CaptureFileTypes {

    const val PDF = "application/pdf"
    private const val DOC  = "application/msword"
    private const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private const val PPT  = "application/vnd.ms-powerpoint"
    private const val PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

    private val BY_EXTENSION = mapOf("pdf" to PDF, "doc" to DOC, "docx" to DOCX, "ppt" to PPT, "pptx" to PPTX)
    private const val GENERIC = "application/octet-stream"

    /** What the file picker offers. */
    val PICKABLE: Array<String> = arrayOf("image/*", PDF, DOC, DOCX, PPT, PPTX)

    fun isPowerPoint(mime: String?) = mime == PPT || mime == PPTX

    /** Word or PowerPoint; PDFs keep their own kind. */
    fun isOfficeDocument(mime: String?) = mime == DOC || mime == DOCX || isPowerPoint(mime)

    /** The real type: the first definite hint, else the file name's extension, else generic. */
    fun resolve(mimeHints: List<String?>, fileName: String?): String {
        val definite = mimeHints.firstOrNull { !it.isNullOrBlank() && !it.endsWith("/*") && it != GENERIC }
        return definite ?: BY_EXTENSION[fileName?.substringAfterLast('.', "")?.lowercase()] ?: GENERIC
    }

    /** The extension to store a file under. */
    fun extensionOf(mime: String): String? = BY_EXTENSION.entries.firstOrNull { it.value == mime }?.key
}
