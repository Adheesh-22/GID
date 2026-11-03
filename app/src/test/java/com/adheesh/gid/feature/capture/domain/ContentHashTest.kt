package com.adheesh.gid.feature.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentHashTest {

    @Test fun `matches the known SHA-256 of abc`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ContentHash.of("abc".byteInputStream()),
        )
    }

    @Test fun `same bytes give the same hash and different bytes don't`() {
        val big = ByteArray(200_000) { (it % 251).toByte() }
        val changed = big.copyOf().also { it[150_000] = 7 }
        assertEquals(ContentHash.of(big.inputStream()), ContentHash.of(big.copyOf().inputStream()))
        assertNotEquals(ContentHash.of(big.inputStream()), ContentHash.of(changed.inputStream()))
    }
}
