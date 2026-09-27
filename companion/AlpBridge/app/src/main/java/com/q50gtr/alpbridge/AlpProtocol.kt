package com.q50gtr.alpbridge

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Формат пакетов моста — единственное, что общее у этого приложения с
 * Q50 GTR+ на головном устройстве. Код не общий: DCU собран под Android 2.3
 * (API 10) ручной цепочкой aapt/javac/d8, а этот модуль — обычный современный
 * Gradle-проект. Общий только протокол, и он описан слово в слово здесь и в
 * `docs/ALP-MIRROR-PROTOCOL.md` в основном репозитории — при любом изменении
 * менять оба места сразу.
 */
object AlpProtocol {

    const val PORT = 45455

    /** "Q1FR" — кусок кадра трансляции экрана. */
    val MAGIC_FRAME = byteArrayOf(0x51, 0x31, 0x46, 0x52)

    /** "Q1TP" — команда касания, ГУ -> телефон. Разбирается уже сейчас, но
     *  ГУ в этой поставке таких пакетов не шлёт (см. docs/ALP-CONTROL.md). */
    val MAGIC_TAP = byteArrayOf(0x51, 0x31, 0x54, 0x50)

    const val FRAME_HEADER_LEN = 26
    const val TAP_PACKET_LEN = 25

    /** Кусок кадра ниже этого размера MTU usb0 (замерено на машине — 1500)
     *  переживает без фрагментации на уровне IP. */
    const val MAX_CHUNK_PAYLOAD = 1400

    /** Собирает один пакет-кусок кадра по docs/ALP-MIRROR-PROTOCOL.md. */
    fun buildFrameChunk(
        frameId: Int,
        totalChunks: Int,
        chunkIndex: Int,
        width: Int,
        height: Int,
        tsMs: Long,
        payload: ByteArray,
        offset: Int,
        length: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream(FRAME_HEADER_LEN + length)
        val d = DataOutputStream(out)
        d.write(MAGIC_FRAME)
        d.writeInt(frameId)
        d.writeShort(totalChunks)
        d.writeShort(chunkIndex)
        d.writeShort(width)
        d.writeShort(height)
        d.writeLong(tsMs)
        d.writeShort(length)
        d.write(payload, offset, length)
        return out.toByteArray()
    }

    data class Beacon(val dcuAddress: String, val port: Int)

    /** Разбирает "Q50GTR BEACON <ip> PORT <port>" — уже реализовано на ГУ
     *  в BridgeSource.toggleBeacon(). Молча возвращает null на всём чужом. */
    fun parseBeacon(text: String): Beacon? {
        val parts = text.trim().split(Regex("\\s+"))
        if (parts.size < 4 || parts[0] != "Q50GTR" || parts[1] != "BEACON") {
            return null
        }
        val port = parts.getOrNull(3)?.toIntOrNull() ?: return null
        return Beacon(parts[2], port)
    }

    data class TapCommand(val seq: Int, val nx: Float, val ny: Float, val longPress: Boolean, val tsMs: Long)

    /** Разбирает команду касания. Возвращает null на чём угодно битом или
     *  чужом — не роняет приём. */
    fun parseTap(buf: ByteArray, len: Int): TapCommand? {
        if (len < TAP_PACKET_LEN) return null
        for (i in MAGIC_TAP.indices) {
            if (buf[i] != MAGIC_TAP[i]) return null
        }
        val seq = i32(buf, 4)
        val nx = java.lang.Float.intBitsToFloat(i32(buf, 8))
        val ny = java.lang.Float.intBitsToFloat(i32(buf, 12))
        val kind = buf[16].toInt()
        val ts = i64(buf, 17)
        if (nx.isNaN() || ny.isNaN() || nx < 0f || nx > 1f || ny < 0f || ny > 1f) return null
        return TapCommand(seq, nx, ny, kind == 1, ts)
    }

    private fun i32(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 24) or ((b[off + 1].toInt() and 0xFF) shl 16) or
            ((b[off + 2].toInt() and 0xFF) shl 8) or (b[off + 3].toInt() and 0xFF)

    private fun i64(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        }
        return v
    }
}
