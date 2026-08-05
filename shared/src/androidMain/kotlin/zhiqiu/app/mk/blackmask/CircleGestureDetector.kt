package zhiqiu.app.mk.blackmask

import android.view.MotionEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Detects a single-finger circular gesture ("draw an O") on the black screen overlay.
 *
 * The path is accumulated while the finger is down. A gesture counts as a circle once the finger
 * has swept a full loop around the path centroid (angular sweep >= [MIN_ANGLE_SWEEP]) over a path
 * that is long enough ([MIN_PATH_LENGTH]). This is a fallback exit method for devices whose system
 * gestures swallow multi-finger taps/swipes.
 */
class CircleGestureDetector(private val onCircle: () -> Unit) {

    companion object {
        private const val MIN_SAMPLE_DISTANCE = 20f
        private const val MIN_PATH_LENGTH = 300.0
        private const val MIN_POINTS = 12
        private const val MIN_ANGLE_SWEEP = 2.0 * PI - 0.5 // ~283°, forgiving of imperfect loops
    }

    private val path = ArrayList<Pair<Float, Float>>(64)

    private var active = false

    /**
     * Feeds a [MotionEvent] to the detector. Returns true when a complete circle was just drawn.
     */
    fun onTouch(event: MotionEvent): Boolean {
        if (event.pointerCount != 1) {
            reset()
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.clear()
                active = true
                path.add(event.x to event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!active) return false
                val (x, y) = event.x to event.y
                val (lastX, lastY) = path[path.size - 1]
                if (hypot(x - lastX, y - lastY) >= MIN_SAMPLE_DISTANCE) {
                    path.add(x to y)
                    if (path.size >= MIN_POINTS && checkCircle()) {
                        reset()
                        onCircle()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return false
    }

    private fun checkCircle(): Boolean {
        val n = path.size
        if (n < MIN_POINTS) return false
        var cx = 0.0
        var cy = 0.0
        for ((x, y) in path) {
            cx += x
            cy += y
        }
        cx /= n
        cy /= n

        var pathLength = 0.0
        var sweep = 0.0
        var prevAngle = atan2(path[0].second - cy, path[0].first - cx)
        for (i in 1 until n) {
            val (x, y) = path[i]
            pathLength += hypot(x - path[i - 1].first, y - path[i - 1].second).toDouble()
            val angle = atan2(y - cy, x - cx)
            var delta = angle - prevAngle
            while (delta > PI) delta -= 2 * PI
            while (delta < -PI) delta += 2 * PI
            sweep += delta
            prevAngle = angle
        }
        return pathLength >= MIN_PATH_LENGTH && abs(sweep) >= MIN_ANGLE_SWEEP
    }

    private fun reset() {
        path.clear()
        active = false
    }
}
