package com.zhuanshi.feixing.ui.globe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.ui.theme.Gold
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val VISIBLE_Z = 0.03f

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun vecFrom(city: City): Vec3 {
    val la = Math.toRadians(city.lat)
    val lo = Math.toRadians(city.lon)
    return Vec3(
        (cos(la) * sin(lo)).toFloat(),
        sin(la).toFloat(),
        (cos(la) * cos(lo)).toFloat()
    )
}

private fun project(v: Vec3, rotLonDeg: Float, rotLatDeg: Float): Offset? {
    val lo = Math.toRadians(rotLonDeg.toDouble())
    val la = Math.toRadians(rotLatDeg.toDouble())

    val x1 = v.x * cos(lo).toFloat() + v.z * sin(lo).toFloat()
    val z1 = -v.x * sin(lo).toFloat() + v.z * cos(lo).toFloat()
    val y1 = v.y

    val y2 = y1 * cos(la).toFloat() - z1 * sin(la).toFloat()
    val z2 = y1 * sin(la).toFloat() + z1 * cos(la).toFloat()

    if (z2 <= VISIBLE_Z) return null
    return Offset(x1, -y2)
}

/** 球面线性插值，用来画大圆弧 */
private fun slerp(a: Vec3, b: Vec3, t: Float): Vec3 {
    var dot = a.x * b.x + a.y * b.y + a.z * b.z
    dot = dot.coerceIn(-1f, 1f)
    val omega = Math.acos(dot.toDouble()).toFloat()
    if (omega < 1e-4f) return a
    val so = sin(omega)
    val ka = sin((1f - t) * omega) / so
    val kb = sin(t * omega) / so
    return Vec3(a.x * ka + b.x * kb, a.y * ka + b.y * kb, a.z * ka + b.z * kb)
}

@Composable
fun GlobeCanvas(
    from: City,
    to: City,
    modifier: Modifier = Modifier
) {
    var rotLon by remember { mutableFloatStateOf(-112f) }
    var rotLat by remember { mutableFloatStateOf(30f) }
    var velocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                } else {
                    val dt = ((now - last) / 1_000_000_000.0).toFloat()
                    last = now
                    if (abs(velocity) > 0.6f) {
                        rotLon += velocity * dt
                        velocity *= 0.955f
                    } else if (velocity != 0f) {
                        velocity = 0f
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { velocity = 0f },
                onDragEnd = {},
                onDragCancel = { velocity = 0f },
                onDrag = { change, drag ->
                    change.consume()
                    rotLon += drag.x * 0.28f
                    rotLat = (rotLat - drag.y * 0.22f).coerceIn(-72f, 72f)
                    velocity = drag.x * 16f
                }
            )
        }
    ) {
        val radius = minOf(size.width, size.height) * 0.44f
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawAtmosphere(cx, cy, radius)
        drawSphere(cx, cy, radius)
        drawRoute(from, to, rotLon, rotLat, cx, cy, radius)
        drawCities(from, to, rotLon, rotLat, cx, cy, radius)
    }
}

private fun DrawScope.drawAtmosphere(cx: Float, cy: Float, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Gold.copy(alpha = 0.13f),
                Gold.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 1.42f
        ),
        radius = radius * 1.42f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawSphere(cx: Float, cy: Float, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF163A24),
                Color(0xFF0A2115),
                Color(0xFF050C09)
            ),
            center = Offset(cx - radius * 0.30f, cy - radius * 0.34f),
            radius = radius * 1.65f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xFF02040A)),
            center = Offset(cx + radius * 0.45f, cy + radius * 0.45f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = Gold.copy(alpha = 0.16f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawRoute(
    from: City,
    to: City,
    rotLon: Float,
    rotLat: Float,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val a = vecFrom(from)
    val b = vecFrom(to)
    val path = Path()
    var started = false

    val steps = 72
    for (i in 0..steps) {
        val t = i.toFloat() / steps.toFloat()
        val v = slerp(a, b, t)
        val p = project(v, rotLon, rotLat)
        if (p == null) {
            started = false
            continue
        }
        val px = cx + p.x * radius
        val py = cy + p.y * radius
        if (!started) {
            path.moveTo(px, py)
            started = true
        } else {
            path.lineTo(px, py)
        }
    }

    drawPath(
        path = path,
        color = Gold.copy(alpha = 0.55f),
        style = Stroke(width = 1.8f)
    )
}

private fun DrawScope.drawCities(
    from: City,
    to: City,
    rotLon: Float,
    rotLat: Float,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val focusCodes = setOf(from.code, to.code)

    listOfCities().forEach { city ->
        val p = project(vecFrom(city), rotLon, rotLat) ?: return@forEach
        val px = cx + p.x * radius
        val py = cy + p.y * radius
        val isFocus = focusCodes.contains(city.code)

        val glowR = if (isFocus) radius * 0.20f else radius * 0.085f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Gold.copy(alpha = if (isFocus) 0.62f else 0.34f),
                    Color.Transparent
                ),
                center = Offset(px, py),
                radius = glowR
            ),
            radius = glowR,
            center = Offset(px, py)
        )

        drawCircle(
            color = if (isFocus) Color(0xFFFFF3CE) else Gold.copy(alpha = 0.85f),
            radius = if (isFocus) 4.2f else 2.1f,
            center = Offset(px, py)
        )
    }
}

private fun listOfCities(): List<City> = com.zhuanshi.feixing.data.Cities.all
