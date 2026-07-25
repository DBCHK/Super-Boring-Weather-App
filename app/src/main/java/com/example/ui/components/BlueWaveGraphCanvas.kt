package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.HourlyForecast
import kotlin.math.roundToInt

/**
 * Not Boring–style hourly precipitation chance curve.
 *
 * - Y axis: fixed 0% → 100% (precip probability)
 * - X axis: synced to hourly time labels
 * - Smooth cubic wave + gradient fill + scrubber
 */
@Composable
fun BlueWaveGraphCanvas(
    hourlyList: List<HourlyForecast>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    // Must match Canvas padLeft / padRight so scrub index lines up with time axis
    val padLeftPx = 44f
    val padRightPx = 12f

    fun indexFromX(x: Float, width: Float, count: Int): Int {
        if (width <= 0f || count <= 1) return 0
        val graphWidth = (width - padLeftPx - padRightPx).coerceAtLeast(1f)
        val fraction = ((x - padLeftPx) / graphWidth).coerceIn(0f, 1f)
        return (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }

    Box(
        modifier = modifier
            .pointerInput(hourlyList) {
                detectTapGestures { offset ->
                    val idx = indexFromX(offset.x, size.width.toFloat(), hourlyList.size)
                    if (idx != selectedIndex) onIndexSelected(idx)
                }
            }
            .pointerInput(hourlyList) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val idx = indexFromX(change.position.x, size.width.toFloat(), hourlyList.size)
                    if (idx != selectedIndex) onIndexSelected(idx)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Layout padding: room for Y labels (left) + X time labels (bottom)
            val padLeft = 44f
            val padRight = 12f
            val padTop = 14f
            val padBottom = 36f

            val graphLeft = padLeft
            val graphRight = width - padRight
            val graphTop = padTop
            val graphBottom = height - padBottom
            val graphWidth = (graphRight - graphLeft).coerceAtLeast(1f)
            val graphHeight = (graphBottom - graphTop).coerceAtLeast(1f)

            val count = hourlyList.size
            val last = (count - 1).coerceAtLeast(1)

            // Fixed Y domain: 0% … 100%
            fun chanceY(percent: Int): Float {
                val p = percent.coerceIn(0, 100) / 100f
                return graphBottom - (p * graphHeight)
            }

            fun pointX(index: Int): Float {
                return graphLeft + (index.toFloat() / last) * graphWidth
            }

            val chances = hourlyList.map { it.precipChancePercent.coerceIn(0, 100) }

            // --- Grid + Y-axis labels (0 / 50 / 100) ---
            val yTicks = listOf(0, 50, 100)
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 26f
                color = android.graphics.Color.parseColor("#8E8E93")
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
            }

            yTicks.forEach { tick ->
                val y = chanceY(tick)
                drawLine(
                    color = Color(0xFFD1D1D6).copy(alpha = if (tick == 0) 0.9f else 0.45f),
                    start = Offset(graphLeft, y),
                    end = Offset(graphRight, y),
                    strokeWidth = if (tick == 0) 2f else 1.2f,
                    pathEffect = if (tick == 0) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$tick%",
                    graphLeft - 8f,
                    y + 9f,
                    labelPaint
                )
            }

            // --- Smooth cubic curve path (Catmull-Rom → cubic Bezier style controls) ---
            val strokePath = Path()
            val fillPath = Path()

            if (count == 1) {
                val x = pointX(0)
                val y = chanceY(chances[0])
                strokePath.moveTo(x, y)
                fillPath.moveTo(graphLeft, graphBottom)
                fillPath.lineTo(x, y)
                fillPath.lineTo(graphRight, graphBottom)
                fillPath.close()
            } else {
                val xs = FloatArray(count) { pointX(it) }
                val ys = FloatArray(count) { chanceY(chances[it]) }

                strokePath.moveTo(xs[0], ys[0])
                fillPath.moveTo(xs[0], graphBottom)
                fillPath.lineTo(xs[0], ys[0])

                for (i in 0 until count - 1) {
                    val x0 = if (i == 0) xs[0] else xs[i - 1]
                    val y0 = if (i == 0) ys[0] else ys[i - 1]
                    val x1 = xs[i]
                    val y1 = ys[i]
                    val x2 = xs[i + 1]
                    val y2 = ys[i + 1]
                    val x3 = if (i + 2 < count) xs[i + 2] else xs[i + 1]
                    val y3 = if (i + 2 < count) ys[i + 2] else ys[i + 1]

                    // Smooth tangents (scaled Catmull-Rom)
                    val tension = 0.2f
                    val c1x = x1 + (x2 - x0) * tension
                    val c1y = y1 + (y2 - y0) * tension
                    val c2x = x2 - (x3 - x1) * tension
                    val c2y = y2 - (y3 - y1) * tension

                    strokePath.cubicTo(c1x, c1y, c2x, c2y, x2, y2)
                    fillPath.cubicTo(c1x, c1y, c2x, c2y, x2, y2)
                }

                fillPath.lineTo(xs[count - 1], graphBottom)
                fillPath.lineTo(xs[0], graphBottom)
                fillPath.close()
            }

            // Gradient fill under the curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.55f),
                        Color(0xFF00C7BE).copy(alpha = 0.28f),
                        Color(0xFF007AFF).copy(alpha = 0.04f)
                    ),
                    startY = graphTop,
                    endY = graphBottom
                )
            )

            // Curve stroke
            drawPath(
                path = strokePath,
                color = Color(0xFF007AFF),
                style = Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Subtle point dots along the curve
            for (i in chances.indices) {
                val x = pointX(i)
                val y = chanceY(chances[i])
                drawCircle(
                    color = Color(0xFF007AFF).copy(alpha = 0.35f),
                    radius = 3.5f,
                    center = Offset(x, y)
                )
            }

            // --- Selected hour scrubber ---
            val sel = selectedIndex.coerceIn(0, count - 1)
            val selX = pointX(sel)
            val selY = chanceY(chances[sel])

            drawLine(
                color = Color(0xFF1C1C1E).copy(alpha = 0.55f),
                start = Offset(selX, graphTop),
                end = Offset(selX, graphBottom),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            // Knob
            drawCircle(color = Color.White, radius = 11f, center = Offset(selX, selY))
            drawCircle(color = Color(0xFF007AFF), radius = 6.5f, center = Offset(selX, selY))

            // Floating % bubble above selection
            val bubbleText = "${chances[sel]}%"
            val bubblePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 28f
                color = android.graphics.Color.parseColor("#1C1C1E")
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
            }
            val bubbleY = (selY - 22f).coerceAtLeast(graphTop + 4f)
            drawContext.canvas.nativeCanvas.drawText(bubbleText, selX, bubbleY, bubblePaint)

            // --- X-axis time labels (synced to hourly times) ---
            val timePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 24f
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
            }

            // Show a readable subset of labels to avoid crowding
            val maxLabels = 6
            val labelStep = when {
                count <= maxLabels -> 1
                else -> ((count - 1).toFloat() / (maxLabels - 1)).roundToInt().coerceAtLeast(1)
            }
            val labeled = linkedSetOf(0, count - 1)
            var i = 0
            while (i < count) {
                labeled.add(i)
                i += labelStep
            }
            // Always include selected index if room
            labeled.add(sel)

            labeled.forEach { idx ->
                val hour = hourlyList[idx]
                val x = pointX(idx)
                timePaint.color = if (idx == sel) {
                    android.graphics.Color.parseColor("#1C1C1E")
                } else {
                    android.graphics.Color.parseColor("#8E8E93")
                }
                drawContext.canvas.nativeCanvas.drawText(
                    hour.timeLabel,
                    x,
                    height - 8f,
                    timePaint
                )
            }
        }
    }
}
