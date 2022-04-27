package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.setup.Base32.decode
import org.briarproject.mailbox.core.setup.Base32.encode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Random

class Base32Test {

    // Test vectors from RFC 4648
    // https://tools.ietf.org/html/rfc4648#section-10

    @Test
    fun testEncoding() {
        assertEquals("", encode(ByteArray(0)))
        assertEquals("MY", encode(byteArrayOf('f'.code.toByte())))
        assertEquals("MZXQ", encode(byteArrayOf('f'.code.toByte(), 'o'.code.toByte())))
        assertEquals(
            "MZXW6",
            encode(byteArrayOf('f'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte()))
        )
        assertEquals(
            "MZXW6YQ",
            encode(
                byteArrayOf(
                    'f'.code.toByte(),
                    'o'.code.toByte(),
                    'o'.code.toByte(),
                    'b'.code.toByte()
                )
            )
        )
        assertEquals(
            "MZXW6YTB",
            encode(
                byteArrayOf(
                    'f'.code.toByte(),
                    'o'.code.toByte(),
                    'o'.code.toByte(),
                    'b'.code.toByte(),
                    'a'.code.toByte()
                )
            )
        )
        assertEquals(
            "MZXW6YTBOI",
            encode(
                byteArrayOf(
                    'f'.code.toByte(),
                    'o'.code.toByte(),
                    'o'.code.toByte(),
                    'b'.code.toByte(),
                    'a'.code.toByte(),
                    'r'.code.toByte()
                )
            )
        )
    }

    @Test
    fun testStrictDecoding() {
        testDecoding(true)
    }

    @Test
    fun testNonStrictDecoding() {
        testDecoding(false)
    }

    private fun testDecoding(strict: Boolean) {
        assertArrayEquals(ByteArray(0), decode("", strict))
        assertArrayEquals(byteArrayOf('f'.code.toByte()), decode("MY", strict))
        assertArrayEquals(
            byteArrayOf('f'.code.toByte(), 'o'.code.toByte()),
            decode("MZXQ", strict)
        )
        assertArrayEquals(
            byteArrayOf('f'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte()),
            decode("MZXW6", strict)
        )
        assertArrayEquals(
            byteArrayOf('f'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte(), 'b'.code.toByte()),
            decode("MZXW6YQ", strict)
        )
        assertArrayEquals(
            byteArrayOf(
                'f'.code.toByte(),
                'o'.code.toByte(),
                'o'.code.toByte(),
                'b'.code.toByte(),
                'a'.code.toByte()
            ),
            decode("MZXW6YTB", strict)
        )
        assertArrayEquals(
            byteArrayOf(
                'f'.code.toByte(),
                'o'.code.toByte(),
                'o'.code.toByte(),
                'b'.code.toByte(),
                'a'.code.toByte(),
                'r'.code.toByte()
            ),
            decode("MZXW6YTBOI", strict)
        )
    }

    @Test
    fun testStrictDecodingRejectsNonZeroUnusedBits() {
        assertThrows(IllegalArgumentException::class.java) { decode("MZ", true) }
    }

    @Test
    fun testNonStrictDecodingAcceptsNonZeroUnusedBits() {
        assertArrayEquals(byteArrayOf('f'.code.toByte()), decode("MZ", false))
    }

    @Test
    fun testRoundTrip() {
        val random = Random()
        val data = ByteArray(100 + random.nextInt(100))
        random.nextBytes(data)
        assertArrayEquals(data, decode(encode(data), true))
        assertArrayEquals(data, decode(encode(data), false))
    }
}
