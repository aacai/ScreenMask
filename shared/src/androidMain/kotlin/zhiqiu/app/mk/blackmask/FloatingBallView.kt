package zhiqiu.app.mk.blackmask

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
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
 * A mainstream "glossy ball": a radial-gradient sphere with a soft specular highlight.
 * It is purely decorative - no icon, no control - and translucent enough not to risk
 * OLED burn-in.
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
        color = 0xB3FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x59FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false
    private var shaderRadius = -1f

    override fun onDraw(canvas: Canvas) {
        val radius = min(width, height) / 2f
        val cx = width / 2f
        val cy = height / 2f

        // Bright in the centre fading to translucent at the edge - the classic glossy ball.
        if (shaderRadius != radius) {
            bodyPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0xE6FFFFFF.toInt(), 0x99FFFFFF.toInt(), 0x40FFFFFF.toInt()),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            shaderRadius = radius
        }
        canvas.drawCircle(cx, cy, radius, bodyPaint)

        // Specular highlight near the top, like mainstream assistant floating balls.
        highlightPaint.strokeWidth = radius * 0.18f
        canvas.drawArc(
            cx - radius, cy - radius, cx + radius, cy + radius,
            -115f, 75f, false, highlightPaint
        )

        // Subtle outer ring for definition.
        canvas.drawCircle(cx, cy, radius - borderPaint.strokeWidth / 2f, borderPaint)
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
