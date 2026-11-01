package com.zhuanshi.feixing.ui.globe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.WorldLand
import com.zhuanshi.feixing.ui.theme.Gold
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val VISIBLE_Z = 0.02f
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 16f

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun vecFrom(lat: Double, lon: Double): Vec3 {
    val la = Math.toRadians(lat)
    val lo = Math.toRadians(lon)
    return Vec3(
        (cos(la) * sin(lo)).toFloat(),
        sin(la).toFloat(),
        (cos(la) * cos(lo)).toFloat()
    )
}

private fun vecFrom(city: City): Vec3 = vecFrom(city.lat, city.lon)

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

/** 把单位球面上的一点按当前视角旋转到视空间 */
private class Camera(rotLon: Float, rotLat: Float) {
    private val loC: Float
    private val loS: Float
    private val laC: Float
    private val laS: Float

    init {
        val lo = Math.toRadians(rotLon.toDouble())
        val la = Math.toRadians(rotLat.toDouble())
        loC = cos(lo).toFloat()
        loS = sin(lo).toFloat()
        laC = cos(la).toFloat()
        laS = sin(la).toFloat()
    }

    fun rot(x: Float, y: Float, z: Float): Vec3 {
        val x1 = x * loC + z * loS
        val z1 = -x * loS + z * loC
        val y2 = y * laC - z1 * laS
        val z2 = y * laS + z1 * laC
        return Vec3(x1, y2, z2)
    }

    fun rot(v: Vec3): Vec3 = rot(v.x, v.y, v.z)
}

/** 预先把经纬度转成单位向量，省掉每帧的三角函数 */
private object LandCache {
    val rings: List<FloatArray> by lazy {
        WorldLand.rings.map { flat ->
            val pts = flat.size / 2
            val out = FloatArray(pts * 3)
            var j = 0
            var i = 0
            while (i < flat.size) {
                val la = Math.toRadians(flat[i].toDouble())
                val lo = Math.toRadians(flat[i + 1].toDouble())
                out[j++] = (cos(la) * sin(lo)).toFloat()
                out[j++] = sin(la).toFloat()
                out[j++] = (cos(la) * cos(lo)).toFloat()
                i += 2
            }
            out
        }
    }
}

object GlobeMath {
    /** 返回 [rotLon, rotLat]，让整条航线出现在视野中央 */
    fun routeCamera(from: City, to: City): FloatArray {
        val m = slerp(vecFrom(from), vecFrom(to), 0.5f)
        val lat = Math.toDegrees(asin(m.y.coerceIn(-1f, 1f).toDouble())).toFloat()
        val lon = Math.toDegrees(atan2(m.x.toDouble(), m.z.toDouble())).toFloat()
        return floatArrayOf(-lon, lat)
    }
}

@Composable
fun GlobeMap(
    from: City,
    to: City,
    modifier: Modifier = Modifier,
    progress: Float = -1f,
    scale: Float = 1f,
    rotLon: Float = 0f,
    rotLat: Float = 0f,
    interactive: Boolean = true,
    onCamera: (Float, Float, Float) -> Unit = { _, _, _ -> },
    onTap: (() -> Unit)? = null
) {
    val curScale = rememberUpdatedState(scale)
    val curLon = rememberUpdatedState(rotLon)
    val curLat = rememberUpdatedState(rotLat)
    val camera = rememberUpdatedState(onCamera)

    val gesture: Modifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val next = (curScale.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                val factor = 0.34f / (0.55f + next * 0.45f)
                camera.value(
                    next,
                    curLon.value + pan.x * factor,
                    (curLat.value - pan.y * factor).coerceIn(-88f, 88f)
                )
            }
        }
    } else {
        Modifier
    }

    val act = onTap
    val tap: Modifier = if (act != null) {
        Modifier.pointerInput(act) {
            detectTapGestures { act() }
        }
    } else {
        Modifier
    }

    Canvas(modifier.then(gesture).then(tap)) {
        val base = min(size.width, size.height) * 0.44f
        val radius = base * scale
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cam = Camera(rotLon, rotLat)

        if (scale < 1.9f) {
            val atmo = base * 1.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Gold.copy(alpha = 0.12f),
                        Gold.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = atmo
                ),
                radius = atmo,
                center = Offset(cx, cy)
            )
        }

        drawSphere(cx, cy, radius)
        drawLand(cam, cx, cy, radius)
        drawGraticule(cam, cx, cy, radius, scale)
        drawRoute(from, to, cam, cx, cy, radius, scale, progress)
        drawCities(from, to, cam, cx, cy, radius)
    }
}

@Composable
fun GlobeCanvas(
    from: City,
    to: City,
    modifier: Modifier = Modifier,
    progress: Float = -1f,
    onTap: (() -> Unit)? = null
) {
    val cam = remember(from.code, to.code) { GlobeMath.routeCamera(from, to) }
    var scale by remember(from.code, to.code) { mutableFloatStateOf(1f) }
    var lon by remember(from.code, to.code) { mutableFloatStateOf(cam[0]) }
    var lat by remember(from.code, to.code) { mutableFloatStateOf(cam[1]) }

    GlobeMap(
        from = from,
        to = to,
        modifier = modifier,
        progress = progress,
        scale = scale,
        rotLon = lon,
        rotLat = lat,
        interactive = true,
        onCamera = { s, lo, la ->
            scale = s
            lon = lo
            lat = la
        },
        onTap = onTap
    )
}

private fun DrawScope.drawSphere(cx: Float, cy: Float, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0E2A1B),
                Color(0xFF081711),
                Color(0xFF03080A)
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

private fun DrawScope.drawLand(cam: Camera, cx: Float, cy: Float, radius: Float) {
    val path = Path()
    for (ring in LandCache.rings) {
        var started = false
        var i = 0
        while (i < ring.size) {
            val v = cam.rot(ring[i], ring[i + 1], ring[i + 2])
            if (v.z > VISIBLE_Z) {
                val px = cx + v.x * radius
                val py = cy - v.y * radius
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            } else {
                started = false
            }
            i += 3
        }
    }
    drawPath(path, color = Color(0xFF17402A))
    drawPath(path, color = Gold.copy(alpha = 0.22f), style = Stroke(width = 1f))
}

private fun DrawScope.drawGraticule(
    cam: Camera,
    cx: Float,
    cy: Float,
    radius: Float,
    scale: Float
) {
    if (scale < 1.12f) return
    val alpha = ((scale - 1.12f) / 2.4f).coerceIn(0f, 1f) * 0.20f
    val color = Gold.copy(alpha = alpha)
    val stroke = Stroke(width = 0.8f)

    var lat = -60.0
    while (lat <= 60.0) {
        val p = Path()
        var started = false
        var lon = -180.0
        while (lon <= 180.0) {
            val v = cam.rot(vecFrom(lat, lon))
            if (v.z > VISIBLE_Z) {
                val px = cx + v.x * radius
                val py = cy - v.y * radius
                if (!started) {
                    p.moveTo(px, py)
                    started = true
                } else {
                    p.lineTo(px, py)
                }
            } else {
                started = false
            }
            lon += 6.0
        }
        drawPath(p, color = color, style = stroke)
        lat += 20.0
    }

    var lon2 = -180.0
    while (lon2 < 180.0) {
        val p = Path()
        var started = false
        var lat2 = -84.0
        while (lat2 <= 84.0) {
            val v = cam.rot(vecFrom(lat2, lon2))
            if (v.z > VISIBLE_Z) {
                val px = cx + v.x * radius
                val py = cy - v.y * radius
                if (!started) {
                    p.moveTo(px, py)
                    started = true
                } else {
                    p.lineTo(px, py)
                }
            } else {
                started = false
            }
            lat2 += 6.0
        }
        drawPath(p, color = color, style = stroke)
        lon2 += 20.0
    }
}

private fun DrawScope.drawRoute(
    from: City,
    to: City,
    cam: Camera,
    cx: Float,
    cy: Float,
    radius: Float,
    scale: Float,
    progress: Float
) {
    val a = vecFrom(from)
    val b = vecFrom(to)
    val path = Path()
    var started = false
    val steps = 120
    for (i in 0..steps) {
        val v = cam.rot(slerp(a, b, i.toFloat() / steps.toFloat()))
        if (v.z <= VISIBLE_Z) {
            started = false
            continue
        }
        val px = cx + v.x * radius
        val py = cy - v.y * radius
        if (!started) {
            path.moveTo(px, py)
            started = true
        } else {
            path.lineTo(px, py)
        }
    }
    drawPath(
        path = path,
        color = Gold.copy(alpha = 0.78f),
        style = Stroke(width = if (scale > 3f) 3.2f else 2f)
    )

    val endA = cam.rot(a)
    if (endA.z > VISIBLE_Z) {
        drawCircle(
            color = Color(0xFFFFF3CE),
            radius = if (scale > 3f) 6f else 4.2f,
            center = Offset(cx + endA.x * radius, cy - endA.y * radius)
        )
    }
    val endB = cam.rot(b)
    if (endB.z > VISIBLE_Z) {
        drawCircle(
            color = Color(0xFFFFF3CE),
            radius = if (scale > 3f) 6f else 4.2f,
            center = Offset(cx + endB.x * radius, cy - endB.y * radius)
        )
    }

    if (progress < 0f) return
    val p = cam.rot(slerp(a, b, progress.coerceIn(0f, 1f)))
    if (p.z <= VISIBLE_Z) return
    val px = cx + p.x * radius
    val py = cy - p.y * radius
    val glow = if (scale > 3f) 26f else 18f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Gold.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(px, py),
            radius = glow
        ),
        radius = glow,
        center = Offset(px, py)
    )
    drawCircle(color = Color(0xFFFFF3CE), radius = if (scale > 3f) 6.5f else 5f, center = Offset(px, py))
    drawCircle(
        color = Gold.copy(alpha = 0.7f),
        radius = if (scale > 3f) 12f else 9f,
        center = Offset(px, py),
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawCities(
    from: City,
    to: City,
    cam: Camera,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val focus = setOf(from.code, to.code)
    Cities.all.forEach { city ->
        val p = cam.rot(vecFrom(city))
        if (p.z <= VISIBLE_Z) return@forEach
        val px = cx + p.x * radius
        val py = cy - p.y * radius
        val isFocus = focus.contains(city.code)
        if (isFocus) return@forEach
        drawCircle(
            color = Gold.copy(alpha = 0.55f),
            radius = 1.9f,
            center = Offset(px, py)
        )
    }
}
