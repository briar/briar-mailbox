package org.briarproject.mailbox.core.setup

import java.io.ByteArrayOutputStream

object Base32 {

    private val DIGITS = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
        'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', '2', '3', '4', '5', '6', '7'
    )

    fun encode(b: ByteArray): String {
        val s = StringBuilder()
        var byteIndex = 0
        var currentCode = 0x00
        var byteMask = 0x80
        var codeMask = 0x10
        while (byteIndex < b.size) {
            if (b[byteIndex].toInt() and byteMask != 0) currentCode = currentCode or codeMask
            // After every 8 bits, move on to the next byte
            if (byteMask == 0x01) {
                byteMask = 0x80
                byteIndex++
            } else {
                byteMask = byteMask ushr 1
            }
            // After every 5 bits, move on to the next digit
            if (codeMask == 0x01) {
                s.append(DIGITS[currentCode])
                codeMask = 0x10
                currentCode = 0x00
            } else {
                codeMask = codeMask ushr 1
            }
        }
        // If we're part-way through a digit, output it
        if (codeMask != 0x10) s.append(DIGITS[currentCode])
        return s.toString()
    }

    fun decode(s: String, strict: Boolean): ByteArray {
        val b = ByteArrayOutputStream()
        var digitIndex = 0
        val digitCount = s.length
        var currentByte = 0x00
        var byteMask = 0x80
        var codeMask = 0x10
        while (digitIndex < digitCount) {
            val code = decodeDigit(s[digitIndex])
            if (code and codeMask != 0) currentByte = currentByte or byteMask
            // After every 8 bits, move on to the next byte
            if (byteMask == 0x01) {
                b.write(currentByte)
                byteMask = 0x80
                currentByte = 0x00
            } else {
                byteMask = byteMask ushr 1
            }
            // After every 5 bits, move on to the next digit
            if (codeMask == 0x01) {
                codeMask = 0x10
                digitIndex++
            } else {
                codeMask = codeMask ushr 1
            }
        }
        // If any extra bits were used for encoding, they should all be zero
        require(!(strict && byteMask != 0x80 && currentByte != 0x00))
        return b.toByteArray()
    }

    private fun decodeDigit(c: Char): Int {
        if (c in 'A'..'Z') return c - 'A'
        if (c in 'a'..'z') return c - 'a'
        if (c in '2'..'7') return c - '2' + 26
        throw IllegalArgumentException("Not a base32 digit: $c")
    }
}
