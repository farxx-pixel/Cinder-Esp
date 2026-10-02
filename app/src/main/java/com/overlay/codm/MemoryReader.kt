package com.overlay.codm

import android.util.Base64
import rikka.shizuku.Shizuku
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MemoryReader(private val pid: Int) {

    private val _base: Long by lazy { resolveBase("libil2cpp.so") }
    fun getBase(): Long = _base

    private fun shell(cmd: String): String {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output  = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output
        } catch (_: Exception) { "" }
    }

    private fun read(address: Long, size: Int): ByteArray {
        if (address <= 0L) return ByteArray(size)
        return try {
            val cmd  = "dd if=/proc/$pid/mem bs=1 skip=$address count=$size 2>/dev/null | base64 -w 0"
            val b64  = shell(cmd)
            if (b64.isEmpty()) return ByteArray(size)
            val data = Base64.decode(b64, Base64.DEFAULT)
            if (data.size >= size) data.copyOf(size) else ByteArray(size)
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
            String(read(ptr + 4, len * 2), Charsets.UTF_16LE).trimEnd('\u0000')
        } catch (_: Exception) { "" }
    }

    fun resolveBase(libName: String): Long {
        val maps = shell("cat /proc/$pid/maps")
        for (line in maps.lines()) {
            if (line.contains(libName) && line.contains("r-xp")) {
                return try {
                    java.lang.Long.parseLong(line.substringBefore('-'), 16)
                } catch (_: Exception) { 0L }
            }
        }
        return 0L
    }

    companion object {
        fun findCodmPid(): Int {
            return try {
                val process = Shizuku.newProcess(
                    arrayOf("sh", "-c", "pidof com.activision.callofduty.shooter"),
                    null, null
                )
                val result = process.inputStream.bufferedReader().readLine()?.trim()
                process.waitFor()
                result?.toIntOrNull() ?: -1
            } catch (_: Exception) { -1 }
        }
    }
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    fun length() = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    fun distanceTo(o: Vec3) = (this - o).length()
}
