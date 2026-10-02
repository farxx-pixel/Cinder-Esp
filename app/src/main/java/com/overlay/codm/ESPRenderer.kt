package com.overlay.codm

import android.graphics.*
import kotlin.math.*

data class RenderConfig(
    val showBox:       Boolean = true,
    val showSkeleton:  Boolean = true,
    val showHealth:    Boolean = true,
    val showShield:    Boolean = true,
    val showNames:     Boolean = true,
    val showDistance:  Boolean = true,
    val showSnaplines: Boolean = false,
    val maxDistance:   Float   = 30000f
)

class ESPRenderer(private val screenW: Int, private val screenH: Int) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 2.2f; isAntiAlias = true
    }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL }
    private val healthBg  = Paint().apply {
        color = Color.argb(160, 20, 20, 20); style = Paint.Style.FILL
    }
    private val healthFg  = Paint().apply { style = Paint.Style.FILL }
    private val shieldFg  = Paint().apply {
        color = Color.argb(220, 60, 140, 255); style = Paint.Style.FILL
    }
    private val cornerPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 3.2f
        strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val skeletonPaint = Paint().apply {
        color = Color.WHITE; strokeWidth = 1.8f
        style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val snapPaint = Paint().apply {
        color = Color.argb(140, 255, 80, 0)
        strokeWidth = 1.2f; style = Paint.Style.STROKE
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }
    private val namePaint = Paint().apply {
        color = Color.WHITE; textSize = 22f
        isAntiAlias = true; setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }

    private fun healthColor(r: Float) = when {
        r > 0.66f -> Color.rgb((255 * (1f - r) * 3).toInt().coerceIn(0, 255), 220, 60)
        r > 0.33f -> Color.rgb(255, (255 * ((r - 0.33f) / 0.33f)).toInt().coerceIn(0, 255), 0)
        else      -> Color.rgb(255, (80 * r / 0.33f).toInt().coerceIn(0, 80), 0)
    }

    private fun drawCornerBox(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        cornerPaint.color = color
        val cw = (r - l) * 0.22f; val ch = (b - t) * 0.22f
        c.drawLine(l, t, l + cw, t, cornerPaint); c.drawLine(l, t, l, t + ch, cornerPaint)
        c.drawLine(r, t, r - cw, t, cornerPaint); c.drawLine(r, t, r, t + ch, cornerPaint)
        c.drawLine(l, b, l + cw, b, cornerPaint); c.drawLine(l, b, l, b - ch, cornerPaint)
        c.drawLine(r, b, r - cw, b, cornerPaint); c.drawLine(r, b, r, b - ch, cornerPaint)
    }

    fun worldToScreen(world: Vec3, cam: Vec3, yaw: Float, pitch: Float, fov: Float): PointF? {
        val dx = world.x - cam.x; val dy = world.y - cam.y; val dz = world.z - cam.z
        val yr = Math.toRadians(yaw.toDouble())
        val pr = Math.toRadians(pitch.toDouble())
        val cosY = cos(yr); val sinY = sin(yr)
        val cosP = cos(pr); val sinP = sin(pr)
        val rx = (dx * cosY + dy * sinY).toFloat()
        val ry = (-dx * sinY * cosP + dy * cosY * cosP + dz * sinP).toFloat()
        val rz = (dx * sinY * sinP - dy * cosY * sinP + dz * cosP).toFloat()
        if (ry < 0.5f) return null
        val scale = (screenH / 2f) / tan(Math.toRadians(fov / 2.0)).toFloat()
        val sx = screenW / 2f + (rx / ry) * scale
        val sy = screenH / 2f - (rz / ry) * scale
        if (sx < -screenW || sx > screenW * 2f || sy < -screenH || sy > screenH * 2f) return null
        return PointF(sx, sy)
    }

    fun draw(canvas: Canvas, entities: List<PlayerEntity>, cam: CameraState, cfg: RenderConfig) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val snap = PointF(screenW / 2f, screenH.toFloat())

        for (e in entities) {
            val distM  = e.distance / 100f
            val alpha  = (255 * (1f - (distM / 300f).coerceIn(0f, 0.6f))).toInt().coerceIn(80, 255)
            val head   = worldToScreen(e.headPos, cam.location, cam.yaw, cam.pitch, cam.fov) ?: continue
            val foot   = worldToScreen(Vec3(e.location.x, e.location.y, e.location.z - 10f),
                cam.location, cam.yaw, cam.pitch, cam.fov) ?: continue
            val boxH   = abs(head.y - foot.y).coerceAtLeast(20f)
            val boxW   = boxH * 0.42f
            val left   = foot.x - boxW / 2f; val right = foot.x + boxW / 2f
            val top    = head.y;              val bot   = foot.y
            val hRatio = (e.health / e.maxHealth).coerceIn(0f, 1f)

            if (cfg.showBox) {
                fillPaint.color = Color.argb(35, 255, 60, 60); fillPaint.alpha = 35
                canvas.drawRect(left, top, right, bot, fillPaint)
                drawCornerBox(canvas, left, top, right, bot, Color.argb(alpha, 255, 70, 70))
            }

            if (cfg.showHealth) {
                val barW  = 5f; val barX = left - 10f; val barH = bot - top
                canvas.drawRect(barX - barW, top, barX, bot, healthBg)
                healthFg.color = healthColor(hRatio)
                canvas.drawRect(barX - barW, bot - barH * hRatio, barX, bot, healthFg)
                if (cfg.showShield && e.maxShield > 0f) {
                    val sR = (e.shield / e.maxShield).coerceIn(0f, 1f)
                    val sx = barX - barW - 4f
                    canvas.drawRect(sx - barW, top, sx, bot, healthBg)
                    canvas.drawRect(sx - barW, bot - barH * sR, sx, bot, shieldFg)
                }
                if (boxH > 60f) {
                    textPaint.textSize = (boxH * 0.13f).coerceIn(18f, 28f)
                    val hpStr = "${e.health.toInt()}/${e.maxHealth.toInt()}"
                    canvas.drawText(hpStr, left - textPaint.measureText(hpStr) / 2f - 10f, top - 6f, textPaint)
                }
            }

            if (cfg.showNames && e.name.isNotEmpty() && boxH > 40f) {
                namePaint.textSize = (boxH * 0.11f).coerceIn(16f, 24f)
                val nw = namePaint.measureText(e.name)
                canvas.drawText(e.name, foot.x - nw / 2f, bot + 18f, namePaint)
            }

            if (cfg.showDistance) {
                val dStr = "${distM.toInt()}m"
                textPaint.textSize = 22f
                val dy = if (cfg.showNames && e.name.isNotEmpty()) 38f else 18f
                canvas.drawText(dStr, foot.x - textPaint.measureText(dStr) / 2f, bot + dy, textPaint)
            }

            if (cfg.showSnaplines)
                canvas.drawLine(snap.x, snap.y, foot.x, foot.y, snapPaint)

            if (cfg.showSkeleton && e.bones.size >= 6)
                drawSkeleton(canvas, e.bones, cam, alpha)
        }
    }

    private val LINKS = listOf(
        Offsets.BONE_HEAD to Offsets.BONE_NECK,
        Offsets.BONE_NECK to Offsets.BONE_CHEST,
        Offsets.BONE_CHEST to Offsets.BONE_PELVIS,
        Offsets.BONE_CHEST to Offsets.BONE_L_SHOULDER,
        Offsets.BONE_CHEST to Offsets.BONE_R_SHOULDER,
        Offsets.BONE_L_SHOULDER to Offsets.BONE_L_ELBOW,
        Offsets.BONE_R_SHOULDER to Offsets.BONE_R_ELBOW,
        Offsets.BONE_L_ELBOW to Offsets.BONE_L_HAND,
        Offsets.BONE_R_ELBOW to Offsets.BONE_R_HAND,
        Offsets.BONE_PELVIS to Offsets.BONE_L_KNEE,
        Offsets.BONE_PELVIS to Offsets.BONE_R_KNEE,
        Offsets.BONE_L_KNEE to Offsets.BONE_L_FOOT,
        Offsets.BONE_R_KNEE to Offsets.BONE_R_FOOT
    )

    private fun drawSkeleton(canvas: Canvas, bones: Map<Int, Vec3>, cam: CameraState, alpha: Int) {
        skeletonPaint.alpha = (alpha * 0.85f).toInt()
        for ((a, b) in LINKS) {
            val wa = bones[a] ?: continue; val wb = bones[b] ?: continue
            val sa = worldToScreen(wa, cam.location, cam.yaw, cam.pitch, cam.fov) ?: continue
            val sb = worldToScreen(wb, cam.location, cam.yaw, cam.pitch, cam.fov) ?: continue
            canvas.drawLine(sa.x, sa.y, sb.x, sb.y, skeletonPaint)
        }
    }
}
