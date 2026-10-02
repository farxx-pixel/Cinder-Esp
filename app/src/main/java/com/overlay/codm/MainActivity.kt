package com.overlay.codm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startBtn:   Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#0D0D0D"))
        }

        val title = TextView(this).apply {
            text = "CODM ESP v2 — Shizuku"
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, 8)
        }

        val subtitle = TextView(this).apply {
            text = "No root required"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#888888"))
            setPadding(0, 0, 0, 32)
        }

        statusText = TextView(this).apply {
            textSize = 13f
            setPadding(0, 0, 0, 24)
        }

        ShizukuHelper.registerListener(
            onGranted = { runOnUiThread { updateStatus() } },
            onDenied  = { runOnUiThread {
                statusText.text = "✗ Shizuku permission denied — try again"
                statusText.setTextColor(android.graphics.Color.RED)
            }}
        )

        fun btn(label: String, color: Int, action: () -> Unit) = Button(this).apply {
            text = label
            setBackgroundColor(color)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 8, 0, 8)
            setOnClickListener { action() }
        }

        val s1 = btn("① Grant Overlay Permission",
            android.graphics.Color.parseColor("#1A3A5C")) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }

        val s2 = btn("② Connect Shizuku",
            android.graphics.Color.parseColor("#1A3A5C")) {
            when {
                !ShizukuHelper.isAvailable() ->
                    Toast.makeText(this, "Open Shizuku app → start service first",
                        Toast.LENGTH_LONG).show()
                !ShizukuHelper.hasPermission() ->
                    ShizukuHelper.requestPermission()
                else ->
                    Toast.makeText(this, "✓ Shizuku already connected",
                        Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }

        startBtn = btn("③ Start ESP Overlay",
            android.graphics.Color.parseColor("#1A5C1A")) {
            when {
                !Settings.canDrawOverlays(this) ->
                    Toast.makeText(this, "Complete step ① first",
                        Toast.LENGTH_SHORT).show()
                !ShizukuHelper.isReady() ->
                    Toast.makeText(this, "Complete step ② first",
                        Toast.LENGTH_SHORT).show()
                else -> {
                    startForegroundService(Intent(this, OverlayService::class.java))
                    Toast.makeText(this,
                        "ESP started — switch to CODM now",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        val stopBtn = btn("Stop ESP",
            android.graphics.Color.parseColor("#5C1A1A")) {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "ESP stopped", Toast.LENGTH_SHORT).show()
        }

        val note = TextView(this).apply {
            text = "Requires Shizuku running in Wireless ADB mode.\n" +
                   "Start CODM and enter a match before tapping ③."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setPadding(0, 24, 0, 0)
        }

        listOf(title, subtitle, statusText, s1, s2, startBtn, stopBtn, note)
            .forEach { root.addView(it) }
        setContentView(root)
        updateStatus()
    }

    private fun updateStatus() {
        when {
            !ShizukuHelper.isAvailable() -> {
                statusText.text = "● Shizuku not running"
                statusText.setTextColor(android.graphics.Color.parseColor("#FF4444"))
            }
            !ShizukuHelper.hasPermission() -> {
                statusText.text = "● Shizuku running — permission needed"
                statusText.setTextColor(android.graphics.Color.parseColor("#FFAA00"))
            }
            else -> {
                statusText.text = "● Shizuku connected and ready"
                statusText.setTextColor(android.graphics.Color.parseColor("#44FF44"))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
