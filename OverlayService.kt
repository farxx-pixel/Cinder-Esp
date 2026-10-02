package com.overlay.codm

import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var wm:          WindowManager
    private lateinit var surface:     SurfaceView
    private lateinit var controlPanel:LinearLayout

    private var espEnabled    = true
    private var cfg           = RenderConfig()
    private var scanJob: Job? = null

    private lateinit var mem:      MemoryReader
    private lateinit var scanner:  EntityScanner
    private lateinit var renderer: ESPRenderer

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        if (!ShizukuHelper.isReady()) { stopSelf(); return }

        val pid = MemoryReader.findCodmPid()
        if (pid == -1) {
            android.widget.Toast.makeText(this,
                "CODM not running — start a match first", Toast.LENGTH_LONG).show()
            stopSelf(); return
        }

        val dm   = resources.displayMetrics
        renderer = ESPRenderer(dm.widthPixels, dm.heightPixels)
        mem      = MemoryReader(pid)
        scanner  = EntityScanner(mem)

        setupSurface()
        setupControlPanel()
        startLoop()
    }

    private fun setupSurface() {
        surface = SurfaceView(this).apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
        }
        wm.addView(surface, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ))
    }

    private fun setupControlPanel() {
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(210, 12, 12, 12))
            setPadding(16, 16, 16, 16)
        }

        fun toggle(label: String, initial: Boolean, action: (Boolean) -> Unit) =
            Switch(this).apply {
                text = label; isChecked = initial
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, v -> action(v) }
            }

        listOf(
            toggle("ESP",       true,                { v -> espEnabled = v; if (!v) clearSurface() }),
            toggle("Skeleton",  cfg.showSkeleton)    { cfg = cfg.copy(showSkeleton  = it) },
            toggle("Health",    cfg.showHealth)      { cfg = cfg.copy(showHealth    = it) },
            toggle("Shield",    cfg.showShield)      { cfg = cfg.copy(showShield    = it) },
            toggle("Names",     cfg.showNames)       { cfg = cfg.copy(showNames     = it) },
            toggle("Distance",  cfg.showDistance)    { cfg = cfg.copy(showDistance  = it) },
            toggle("Snaplines", cfg.showSnaplines)   { cfg = cfg.copy(showSnaplines = it) }
        ).forEach { controlPanel.addView(it) }

        var dX = 0f; var dY = 0f
        val lp = WindowManager.LayoutParams(
            260, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 16; y = 180 }

        controlPanel.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dX = lp.x - e.rawX; dY = lp.y - e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = (e.rawX + dX).toInt(); lp.y = (e.rawY + dY).toInt()
                    wm.updateViewLayout(v, lp)
                }
            }; false
        }
        wm.addView(controlPanel, lp)
    }

    private fun startLoop() {
        scanJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (!espEnabled) { delay(150); continue }
                try {
                    val cam      = scanner.getCameraState()
                    val entities = scanner.scanEntities(cam.location, cfg.maxDistance)
                    withContext(Dispatchers.Main) { renderFrame(entities, cam) }
                } catch (_: Exception) {}
                delay(28)
            }
        }
    }

    private fun renderFrame(entities: List<PlayerEntity>, cam: CameraState) {
        val canvas = surface.holder.lockCanvas() ?: return
        try { renderer.draw(canvas, entities, cam, cfg) }
        finally { surface.holder.unlockCanvasAndPost(canvas) }
    }

    private fun clearSurface() {
        val canvas = surface.holder.lockCanvas() ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        surface.holder.unlockCanvasAndPost(canvas)
    }

    private fun buildNotification(): Notification {
        val ch = "esp_svc"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(
                    ch, "Overlay", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, ch)
            .setContentTitle("System Service")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    override fun onDestroy() {
        scanJob?.cancel()
        runCatching { wm.removeView(surface) }
        runCatching { wm.removeView(controlPanel) }
        super.onDestroy()
    }
}
