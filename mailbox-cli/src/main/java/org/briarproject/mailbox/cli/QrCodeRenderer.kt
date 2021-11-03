package org.briarproject.mailbox.cli

import com.google.zxing.common.BitMatrix

object QrCodeRenderer {

    private const val SET = "██"
    private const val UNSET = "  "

    internal fun getQrString(bitMatrix: BitMatrix): String = StringBuilder().apply {
        append(System.lineSeparator())
        for (y in 0 until bitMatrix.height) {
            for (x in 0 until bitMatrix.width) {
                append(if (bitMatrix[x, y]) SET else UNSET)
            }
            append(System.lineSeparator())
        }
    }.toString()

}
