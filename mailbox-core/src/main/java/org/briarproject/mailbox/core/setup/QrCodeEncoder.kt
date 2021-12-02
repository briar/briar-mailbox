/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.setup

import com.google.zxing.BarcodeFormat.QR_CODE
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.keiji.util.Base32
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.TransactionManager
import org.briarproject.mailbox.core.tor.TorPlugin
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.StringUtils.fromHexString
import org.slf4j.LoggerFactory.getLogger
import java.nio.ByteBuffer
import java.nio.charset.Charset
import javax.inject.Inject

private const val VERSION = 32
private val LOG = getLogger(QrCodeEncoder::class.java)

class QrCodeEncoder @Inject constructor(
    private val db: TransactionManager,
    private val setupManager: SetupManager,
    private val torPlugin: TorPlugin,
) {

    fun getQrCodeBitMatrix(edgeLen: Int = 0): BitMatrix? {
        val bytes = getQrCodeBytes() ?: return null
        // Use ISO 8859-1 to encode bytes directly as a string
        val content = String(bytes, Charset.forName("ISO-8859-1"))
        return QRCodeWriter().encode(content, QR_CODE, edgeLen, edgeLen)
    }

    private fun getQrCodeBytes(): ByteArray? {
        val hiddenServiceBytes = getHiddenServiceBytes() ?: return null
        val setupTokenBytes = getSetupTokenBytes() ?: return null
        return ByteBuffer.allocate(65)
            .put(VERSION.toByte()) // 1
            .put(hiddenServiceBytes) // 32
            .put(setupTokenBytes) // 32
            .array()
    }

    /**
     * https://gitweb.torproject.org/torspec.git/tree/rend-spec-v3.txt?id=29245fd5#n2135
     */
    private fun getHiddenServiceBytes(): ByteArray? {
        val addressString = try {
            torPlugin.hiddenServiceAddress
        } catch (e: DbException) {
            logException(LOG, e)
            return null
        }
        if (addressString == null) {
            LOG.error("Hidden service address not yet available")
            return null
        }
        LOG.error(addressString)
        val addressBytes = Base32.decode(addressString.uppercase())
        check(addressBytes.size == 35) { "$addressString not 35 bytes long" }
        return addressBytes.copyOfRange(0, 32)
    }

    private fun getSetupTokenBytes(): ByteArray? {
        val tokenString = try {
            db.read { txn ->
                setupManager.getSetupToken(txn)
            }
        } catch (e: DbException) {
            logException(LOG, e)
            return null
        }
        if (tokenString == null) {
            LOG.error("Setup token not available")
            return null
        }
        val tokenBytes = fromHexString(tokenString)
        check(tokenBytes.size == 32) { "$tokenString not 32 bytes long" }
        return tokenBytes
    }
}
