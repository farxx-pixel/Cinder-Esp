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
        c.drawLine(r, b,
