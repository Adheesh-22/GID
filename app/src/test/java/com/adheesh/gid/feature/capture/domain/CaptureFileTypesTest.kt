package com.adheesh.gid.feature.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFileTypesTest {

    private val docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private val pptx = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

    @Test fun `a definite hint wins`() {
        assertEquals(docx, CaptureFileTypes.resolve(listOf(docx, null), "notes.pdf"))
        assertEquals("image/png", CaptureFileTypes.resolve(listOf("image/png"), null))
    }

    @Test fun `vague hints fall through to the next one`() {
        assertEquals(pptx, CaptureFileTypes.resolve(listOf("*/*", pptx), "x"))
        assertEquals(pptx, CaptureFileTypes.resolve(listOf("application/*", null), "Unit 3.PPTX"))
    }

    @Test fun `a generic type uses the file name`() {
        assertEquals(docx, CaptureFileTypes.resolve(listOf("application/octet-stream"), "Lab record.docx"))
        assertEquals("application/octet-stream", CaptureFileTypes.resolve(listOf(null), "archive.zip"))
        assertEquals("application/octet-stream", CaptureFileTypes.resolve(emptyList(), null))
    }

    @Test fun `office documents are told apart from PDFs and photos`() {
        assertTrue(CaptureFileTypes.isOfficeDocument(docx))
        assertTrue(CaptureFileTypes.isOfficeDocument("application/vnd.ms-powerpoint"))
        assertTrue(CaptureFileTypes.isPowerPoint(pptx))
        assertFalse(CaptureFileTypes.isPowerPoint(docx))
        assertFalse(CaptureFileTypes.isOfficeDocument(CaptureFileTypes.PDF))
        assertFalse(CaptureFileTypes.isOfficeDocument("image/jpeg"))
        assertFalse(CaptureFileTypes.isOfficeDocument(null))
    }

    @Test fun `extensions come back for viewers`() {
        assertEquals("docx", CaptureFileTypes.extensionOf(docx))
        assertEquals("pptx", CaptureFileTypes.extensionOf(pptx))
        assertEquals(null, CaptureFileTypes.extensionOf("image/jpeg"))
    }
}
