package com.overlay.codm

import android.content.Context
import android.widget.Toast
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val REQUEST_CODE = 100

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) { false }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestPermission() {
        try { Shizuku.requestPermission(REQUEST_CODE) }
        catch (_: Exception) {}
    }

    fun isReady(): Boolean = isAvailable() && hasPermission()

    // call from Activity.onCreate — registers the result listener
    fun registerListener(
        onGranted: () -> Unit,
        onDenied:  () -> Unit
    ) {
        Shizuku.addRequestPermissionResultListener { _, result ->
            if (result == android.content.pm.PackageManager.PERMISSION_GRANTED)
                onGranted() else onDenied()
        }
    }
}
