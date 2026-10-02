package com.overlay.codm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "CODM ESP v2 — Shizuku"
            textSize = 22f; setPadding(0, 0, 0, 8)
        }

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 0, 0, 24)
        }

        updateStatus()

        // register Shizuku grant listener
        ShizukuHelper.registerListener(
            onGranted = {
                runOnUiThread {
                    statusText.text = "✓ Shizuku ready"
                    statusText.setTextColor(android.graphics.Color.GREEN)
                }
            },
            onDenied = {
                runOnUiThread {
                    statusText.text = "✗ Shizuku permission denied"
                    statusText.setTextColor(android.graphics.Color.RED)
                }
            }
        )

        fun btn(label: String, action: () -> Unit) = Button(this).apply {
            text = label; setOnClickListener { action() }; setPadding(0, 4, 0, 4)
        }

        val s1 = btn("① Grant Overlay Permission") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }

        val s2 = btn("② Connect Shizuku") {
            when {
                !ShizukuHelper.isAvailable() ->
                    Toast.makeText(this,
                        "Open Shizuku app and start the service first",
                        Toast.LENGTH_LONG).show()
                !ShizukuHelper.hasPermission() ->
                    ShizukuHelper.requestPermission()
                else ->
                    Toast.makeText(this, "Shizuku already connected", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }

        val s3 = btn("③ Start ESP") {
            when {
                !Settings.canDrawOverlays(this) ->
                    Toast.makeText(this, "Complete step 1 first", Toast.LENGTH_SHORT).show()
                !ShizukuHelper.isReady() ->
                    Toast.makeText(this, "Complete step 2 first", Toast.LENGTH_SHORT).show()
                else -> {
                    startForegroundService(Intent(this, OverlayService::class.java))
                    Toast.makeText(this, "ESP active — switch to CODM", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val s4 = btn("Stop ESP") {
            stopService(Intent(this, OverlayService::class.java))
        }

        val note = TextView(this).apply {
            text = "No root needed. Requires Shizuku app running in ADB mode."
            textSize = 12f; setPadding(0, 20, 0, 0)
            setTextColor(android.graphics.Color.GRAY)
        }

        listOf(title, statusText, s1, s2, s3, s4, note).forEach { root.addView(it) }
        setContentView(root)
    }

    private fun updateStatus() {
        statusText.text = when {
            !ShizukuHelper.isAvailable()  -> "Shizuku not running — open Shizuku app"
            !ShizukuHelper.hasPermission() -> "Shizuku running — tap ② to grant permission"
            else                           -> "✓ Shizuku ready"
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
