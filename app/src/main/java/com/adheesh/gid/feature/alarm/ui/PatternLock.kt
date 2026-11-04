package com.adheesh.gid.feature.alarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.alarm.domain.AlarmPattern
import kotlinx.coroutines.delay

// ── Pattern to stop an alarm (SPEC §2.6) ─────────────────────────────────────
// The target is shown on its own small grid and drawn on a second, empty one,
// so stopping takes reading and thought, not a reflex tap.

private const val DOTS = AlarmPattern.SIZE * AlarmPattern.SIZE
private const val WRONG_SHOWN_MILLIS = 600L
/** How close to a dot, as a share of a grid cell, counts as touching it. */
private const val TOUCH_RADIUS = 0.38f

private fun center(dot: Int, size: Size): Offset {
    val cellW = size.width / AlarmPattern.SIZE
    val cellH = size.height / AlarmPattern.SIZE
    return Offset(cellW * (dot % AlarmPattern.SIZE + 0.5f), cellH * (dot / AlarmPattern.SIZE + 0.5f))
}

private fun DrawScope.drawPath(path: List<Int>, color: Color, strokeWidth: Float) {
    path.zipWithNext().forEach { (a, b) ->
        drawLine(color, center(a, size), center(b, size), strokeWidth, StrokeCap.Round)
    }
}

/** The pattern to copy, with its dots numbered in order. */
@Composable
fun PatternTarget(pattern: List<Int>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val radius = size.minDimension * 0.095f
        drawPath(pattern, GIDCyan, size.minDimension * 0.03f)
        (0 until DOTS).forEach { dot ->
            val step = pattern.indexOf(dot)
            val c = center(dot, size)
            drawCircle(if (step >= 0) GIDCyan else GIDSurfaceHigh, if (step >= 0) radius else radius * 0.5f, c)
            if (step >= 0) {
                val label = measurer.measure(
                    "${step + 1}",
                    TextStyle(color = OnCard, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                )
                drawText(label, topLeft = c - Offset(label.size.width / 2f, label.size.height / 2f))
            }
        }
    }
}

/** An empty grid to draw on. [wrong] shows the last try in coral, then clears it. */
@Composable
fun PatternInput(
    wrong         : Boolean,
    onWrongShown  : () -> Unit,
    onDrawn       : (List<Int>) -> Unit,
    modifier      : Modifier = Modifier,
) {
    var path by remember { mutableStateOf(emptyList<Int>()) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val drawn by rememberUpdatedState(onDrawn)

    LaunchedEffect(wrong) {
        if (wrong) {
            delay(WRONG_SHOWN_MILLIS)
            path = emptyList()
            onWrongShown()
        }
    }

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                fun touch(position: Offset) {
                    finger = position
                    val area = Size(size.width.toFloat(), size.height.toFloat())
                    val reach = area.minDimension / AlarmPattern.SIZE * TOUCH_RADIUS
                    val hit = (0 until DOTS).firstOrNull { (center(it, area) - position).getDistance() <= reach }
                    if (hit != null) path = AlarmPattern.extend(path, hit)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    path = emptyList()
                    touch(down.position)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        touch(change.position)
                    }
                    finger = null
                    if (path.isNotEmpty()) drawn(path)
                }
            },
    ) {
        val lineColor = if (wrong) GIDCoral else GIDCyan
        val stroke = size.minDimension * 0.02f
        drawPath(path, lineColor, stroke)
        val last = path.lastOrNull()
        val current = finger
        if (last != null && current != null) {
            drawLine(lineColor, center(last, size), current, stroke, StrokeCap.Round)
        }
        val radius = size.minDimension * 0.035f
        (0 until DOTS).forEach { dot ->
            val on = dot in path
            drawCircle(if (on) lineColor else GIDMutedText, if (on) radius * 1.4f else radius, center(dot, size))
        }
    }
}
