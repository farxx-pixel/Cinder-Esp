package com.overlay.codm

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MemoryReader(private val pid: Int) {

    private val _base: Long by lazy { resolveBase("libil2cpp.so") }
    fun getBase(): Long = _base

    // execute shell command via Shizuku privileged shell
    private fun shell(cmd: String): String {
        return try {
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", cmd), null, null
            )
            process.inputStream.bufferedReader().readText().trim()
                .also { process.waitFor() }
        } catch (_: Exception) { "" }
    }

    // read raw bytes from /proc/pid/mem via dd through Shizuku shell
    // dd skip= uses block size 1 for byte-accurate seeking
    private fun read(address: Long, size: Int): ByteArray {
        if (address <= 0L) return ByteArray(size)
        return try {
            // dd reads exactly `size` bytes from `address` in /proc/pid/mem
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=$size 2>/dev/null | base64"
            val b64 = shell(cmd)
            if (b64.isEmpty()) return ByteArray(size)
            android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                .also { if (it.size != size) return ByteArray(size) }
        } catch (_: Exception) { ByteArray(size) }
    }

    fun readInt(address: Long): Int =
        ByteBuffer.wrap(read(address, 4)).order(ByteOrder.LITTLE_ENDIAN).int

    fun readLong(address: Long): Long =
        ByteBuffer.wrap(read(address, 8)).order(ByteOrder.LITTLE_ENDIAN).long

    fun readFloat(address: Long): Float =
        ByteBuffer.wrap(read(address, 4)).order(ByteOrder.LITTLE_ENDIAN).float

    fun readFloat2(address: Long): Pair<Float, Float> {
        val buf = ByteBuffer.wrap(read(address, 8)).order(ByteOrder.LITTLE_ENDIAN)
        return buf.float to buf.float
    }

    fun readVec3(address: Long): Vec3 {
        val buf = ByteBuffer.wrap(read(address, 12)).order(ByteOrder.LITTLE_ENDIAN)
        return Vec3(buf.float, buf.float, buf.float)
    }

    fun readFString(address: Long): String {
        val ptr = readLong(address)
        if (ptr <= 0L) return ""
        val len = readInt(ptr).coerceIn(0, 64)
        if (len == 0) return ""
        return try {
            val bytes = read(ptr + 4, len * 2)
            String(bytes, Charsets.UTF_16LE).trimEnd('\u0000')
        } catch (_: Exception) { "" }
    }

    fun resolveBase(libName: String): Long {
        val maps = shell("cat /proc/$pid/maps")
        maps.lines().forEach { line ->
            if (line.contains(libName) && line.contains("r-xp")) {
                return try {
                    java.lang.Long.parseLong(line.substringBefore('-'), 16)
                } catch (_: Exception) { 0L }
            }
        }
        return 0L
    }

    // find CODM pid via Shizuku shell (no root needed)
    companion object {
        fun findCodmPid(): Int {
            return try {
                val result = Shizuku.newProcess(
                    arrayOf("sh", "-c", "pidof com.activision.callofduty.shooter"),
                    null, null
                ).inputStream.bufferedReader().readLine()?.trim()?.toIntOrNull() ?: -1
            } catch (_: Exception) { -1 }
        }
    }
}
