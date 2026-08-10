package zhiqiu.app.mk.blackmask

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.hypot
import kotlin.math.min

/**
 * Content of the independent floating ball window.
 *
 * A vivid "power" ball in the app's cyan→indigo palette: opaque glossy body, soft drop shadow,
 * white rim and a power symbol, so it stays clearly visible on both light and dark screens.
 * The colors match [zhiqiu.app.mk.ui.theme.AccentGradient].
 *
 * The ball manages its own window position: dragging moves it around (clamped to the
 * screen) and releasing snaps it to the nearest edge. A tap (no movement beyond the
 * touch slop) triggers [onTap], which toggles the black screen.
 */
class FloatingBallView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onTap: () -> Unit,
) : View(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8CFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        style = Paint.Style.STROKE
    }
    private val powerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val powerRect = RectF()

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false
    private var shaderRadius = -1f

    override fun onDraw(canvas: Canvas) {
        val radius = min(width, height) / 2f
        val cx = width / 2f
        val cy = height / 2f

        // Soft drop shadow lifts the ball off any background so it is easy to spot.
        bodyPaint.setShadowLayer(radius * 0.5f, 0f, radius * 0.22f, 0x66000000.toInt())

        // Opaque glossy body: light cyan at the lit corner deepening to indigo at the bottom.
        if (shaderRadius != radius) {
            bodyPaint.shader = RadialGradient(
                cx - radius * 0.25f,
                cy - radius * 0.35f,
                radius * 1.5f,
                intArrayOf(
                    0xFFD8F7F1.toInt(), // light cyan highlight
                    0xFF2DB5A0.toInt(), // AccentCyan
                    0xFF2F4488.toInt(), // deep indigo
                ),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            shaderRadius = radius
        }
        canvas.drawCircle(cx, cy, radius, bodyPaint)

        // Specular highlight near the top, like mainstream assistant floating balls.
        highlightPaint.strokeWidth = radius * 0.16f
        canvas.drawArc(
            cx - radius, cy - radius, cx + radius, cy + radius,
            -115f, 75f, false, highlightPaint
        )

        // White rim for clear definition against any background.
        rimPaint.strokeWidth = radius * 0.09f
        canvas.drawCircle(cx, cy, radius - rimPaint.strokeWidth / 2f, rimPaint)

        // Power symbol: a ring with a gap at the top and a rising stem, hinting the black-screen toggle.
        powerPaint.strokeWidth = radius * 0.15f
        val glyphRadius = radius * 0.34f
        powerRect.set(cx - glyphRadius, cy - glyphRadius, cx + glyphRadius, cy + glyphRadius)
        canvas.drawArc(powerRect, 35f, 290f, false, powerPaint)
        canvas.drawLine(cx, cy - glyphRadius, cx, cy - radius * 0.66f, powerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                if (dragging || hypot(dx, dy) >= touchSlop) {
                    dragging = true
                    layoutParams.x = (layoutParams.x + dx)
                        .toInt()
                        .coerceIn(0, screenWidth - width)
                    layoutParams.y = (layoutParams.y + dy)
                        .toInt()
                        .coerceIn(0, screenHeight - height)
                    windowManager.updateViewLayout(this, layoutParams)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    snapToEdge()
                } else {
                    performClick()
                }
                dragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToEdge() {
        layoutParams.x = if (layoutParams.x < screenWidth / 2) 0 else screenWidth - width
        windowManager.updateViewLayout(this, layoutParams)
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap()
        return true
    }
}
