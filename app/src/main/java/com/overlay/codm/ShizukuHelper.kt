package com.overlay.codm

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val REQUEST_CODE = 101

    fun isAvailable(): Boolean = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestPermission() {
        try { Shizuku.requestPermission(REQUEST_CODE) } catch (_: Exception) {}
    }

    fun isReady(): Boolean = isAvailable() && hasPermission()

    fun registerListener(onGranted: () -> Unit, onDenied: () -> Unit) {
        Shizuku.addRequestPermissionResultListener { _, result ->
            if (result == PackageManager.PERMISSION_GRANTED) onGranted() else onDenied()
        }
    }
}
