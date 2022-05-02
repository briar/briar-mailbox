package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.setup.Base32.decode
import org.briarproject.mailbox.core.setup.Base32.encode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.util.Random

class Base32Test {

    // Test vectors from RFC 4648
    // https://tools.ietf.org/html/rfc4648#section-10

    @Test
    fun testEncoding() {
        assertEquals("", encode(ByteArray(0)))
        assertEquals("MY", encode("f".toUtf8Bytes()))
        assertEquals("MZXQ", encode("fo".toUtf8Bytes()))
        assertEquals("MZXW6", encode("foo".toUtf8Bytes()))
        assertEquals("MZXW6YQ", encode("foob".toUtf8Bytes()))
        assertEquals("MZXW6YTB", encode("fooba".toUtf8Bytes()))
        assertEquals("MZXW6YTBOI", encode("foobar".toUtf8Bytes()))
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
        assertArrayEquals("f".toUtf8Bytes(), decode("MY", strict))
        assertArrayEquals("fo".toUtf8Bytes(), decode("MZXQ", strict))
        assertArrayEquals("foo".toUtf8Bytes(), decode("MZXW6", strict))
        assertArrayEquals("foob".toUtf8Bytes(), decode("MZXW6YQ", strict))
        assertArrayEquals("fooba".toUtf8Bytes(), decode("MZXW6YTB", strict))
        assertArrayEquals("foobar".toUtf8Bytes(), decode("MZXW6YTBOI", strict))
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

    private fun String.toUtf8Bytes() = toByteArray(Charset.forName("UTF-8"))
}
