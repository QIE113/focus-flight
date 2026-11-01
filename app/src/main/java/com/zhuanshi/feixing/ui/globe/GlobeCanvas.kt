package com.zhuanshi.feixing.ui.globe

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.EarthTexture
import com.zhuanshi.feixing.ui.theme.Gold
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val VISIBLE_Z = 0.02f
private const val MIN_SCALE = 0.85f
private const val MAX_SCALE = 12f
private const val RAD2DEG = 57.29578f

private fun vecFrom(lat: Double, lon: Double): FloatArray {
    val la = Math.toRadians(lat)
    val lo = Math.toRadians(lon)
    return floatArrayOf(
        (cos(la) * sin(lo)).toFloat(),
        sin(la).toFloat(),
        (cos(la) * cos(lo)).toFloat()
    )
}

private fun slerp(a: FloatArray, b: FloatArray, t: Float): FloatArray {
    var dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    dot = dot.coerceIn(-1f, 1f)
    val omega = Math.acos(dot.toDouble()).toFloat()
    if (omega < 1e-4f) return a
    val so = sin(omega)
    val ka = sin((1f - t) * omega) / so
    val kb = sin(t * omega) / so
    return floatArrayOf(
        a[0] * ka + b[0] * kb,
        a[1] * ka + b[1] * kb,
        a[2] * ka + b[2] * kb
    )
}

/** 世界坐标 → 视空间：先绕地轴转经度，再抬头转纬度 */
private class View(rotLon: Float, rotLat: Float) {
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

    fun px(x: Float, y: Float, z: Float): Float = x * loC + z * loS

    fun pz(x: Float, y: Float, z: Float): Float = -x * loS + z * loC

    fun py(x: Float, y: Float, z: Float): Float = y * laC - pz(x, y, z) * laS

    fun depth(x: Float, y: Float, z: Float): Float = y * laS + pz(x, y, z) * laC

    fun project(x: Float, y: Float, z: Float, cx: Float, cy: Float, r: Float): Offset? {
        val d = depth(x, y, z)
        if (d <= VISIBLE_Z) return null
        return Offset(cx + px(x, y, z) * r, cy - py(x, y, z) * r)
    }

    fun projectLatLon(lat: Float, lon: Float, cx: Float, cy: Float, r: Float): Offset? {
        val v = vecFrom(lat.toDouble(), lon.toDouble())
        return project(v[0], v[1], v[2], cx, cy, r)
    }
}

object GlobeMath {

    /** 把镜头摆到航线中点，让整条大圆航线落在视野中央 */
    fun routeCamera(from: City, to: City): FloatArray {
        val a = vecFrom(from.lat, from.lon)
        val b = vecFrom(to.lat, to.lon)
        val m = slerp(a, b, 0.5f)
        val lat = asin(m[1].coerceIn(-1f, 1f).toDouble()) * RAD2DEG
        val lon = atan2(m[0].toDouble(), m[2].toDouble()) * RAD2DEG
        return floatArrayOf(-lon.toFloat(), lat.toFloat())
    }
}

/** 按当前镜头把整颗星球逐像素画进一张位图，供 Canvas 直接贴图 */
private fun renderSphere(radiusPx: Int, rotLon: Float, rotLat: Float): ImageBitmap {
    val d = radiusPx * 2
    val out = IntArray(d * d)
    val tex = EarthTexture.pixels
    val tw = EarthTexture.WIDTH
    val th = EarthTexture.HEIGHT
    val lo = Math.toRadians(rotLon.toDouble())
    val la = Math.toRadians(rotLat.toDouble())
    val loC = cos(lo).toFloat()
    val loS = sin(lo).toFloat()
    val laC = cos(la).toFloat()
    val laS = sin(la).toFloat()
    val lightLat = Math.toRadians(16.0)
    val lightLon = Math.toRadians(-42.0)
    val lx = (cos(lightLat) * sin(lightLon)).toFloat()
    val ly = sin(lightLat).toFloat()
    val lz = (cos(lightLat) * cos(lightLon)).toFloat()

    var index = 0
    for (y in 0 until d) {
        val vy = (radiusPx - y - 0.5f) / radiusPx
        for (x in 0 until d) {
            val vx = (x + 0.5f - radiusPx) / radiusPx
            val rr = vx * vx + vy * vy
            if (rr >= 1f) {
                out[index++] = 0
                continue
            }
            val rlen = sqrt(rr)
            val vz = sqrt(1f - rr)
            val wy = vy * laC + vz * laS
            val z1 = -vy * laS + vz * laC
            val wx = vx * loC - z1 * loS
            val wz = vx * loS + z1 * loC
            val lat = asin(wy.coerceIn(-1f, 1f)) * RAD2DEG
            val lon = atan2(wx, wz) * RAD2DEG
            var sx = ((lon + 180f) / 360f * tw).toInt()
            if (sx < 0) sx = 0
            if (sx > tw - 1) sx = tw - 1
            var sy = ((90f - lat) / 180f * th).toInt()
            if (sy < 0) sy = 0
            if (sy > th - 1) sy = th - 1
            val c = tex[sy * tw + sx]

            val lit = wx * lx + wy * ly + wz * lz
            val day = (lit * 0.5f + 0.5f).coerceIn(0f, 1f)
            val shade = 0.14f + 0.98f * day * day
            val rim = (1f - vz) * (1f - vz) * 0.5f

            val rch = ((c shr 16) and 0xFF) * shade + 44f * rim
            val gch = ((c shr 8) and 0xFF) * shade + 68f * rim
            val bch = (c and 0xFF) * shade + 108f * rim

            val edge = ((1f - rlen) * radiusPx).coerceIn(0f, 1f)
            val a = (255f * edge).toInt().coerceIn(0, 255)
            out[index++] = (a shl 24) or
                (rch.toInt().coerceIn(0, 255) shl 16) or
                (gch.toInt().coerceIn(0, 255) shl 8) or
                bch.toInt().coerceIn(0, 255)
        }
    }
    return Bitmap.createBitmap(out, d, d, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private val OCEAN_LABELS = listOf(
    Triple(6f, -150f, "太平洋"),
    Triple(-6f, -25f, "大西洋"),
    Triple(-22f, 78f, "印度洋"),
    Triple(78f, 20f, "北冰洋"),
    Triple(-62f, 20f, "南冰洋")
)

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
    stars: Boolean = false,
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
                val factor = 96f / (56f + next * 34f)
                camera.value(
                    next,
                    curLon.value + pan.x * factor,
                    (curLat.value - pan.y * factor).coerceIn(-89f, 89f)
                )
            }
        }
    } else {
        Modifier
    }

    val act = onTap
    val tapModifier: Modifier = if (act != null) {
        Modifier.pointerInput(act) {
            detectTapGestures { act() }
        }
    } else {
        Modifier
    }

    val starSeed = remember { java.util.Random(20260304L) }
    val starPoints = remember {
        val arr = FloatArray(240)
        var i = 0
        while (i < arr.size) {
            arr[i] = starSeed.nextFloat()
            arr[i + 1] = starSeed.nextFloat()
            i += 2
        }
        arr
    }

    val lonKey = (rotLon * 4f).roundToInt()
    val latKey = (rotLat * 4f).roundToInt()

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.then(gesture).then(tapModifier)
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val base = min(w, h) * 0.46f
        val radius = base * scale
        val bucket = ((radius / 6f).toInt() * 6).coerceIn(90, 420)
        val sphere = remember(bucket, lonKey, latKey) {
            renderSphere(bucket, lonKey / 4f, latKey / 4f)
        }
        Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        if (stars) {
            var i = 0
            while (i < starPoints.size) {
                val sx = starPoints[i] * size.width
                val sy = starPoints[i + 1] * size.height
                val twinkle = ((i % 7) + 1) / 9f
                drawCircle(Color.White.copy(alpha = 0.12f + twinkle * 0.4f), 0.9f, Offset(sx, sy))
                i += 2
            }
        }

        if (scale < 2.2f) {
            val glow = radius * 1.16f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A6BD4).copy(alpha = 0.20f),
                        Color(0xFF1B3E86).copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = glow
                ),
                radius = glow,
                center = Offset(cx, cy)
            )
        }

        val half = (bucket * scale).toInt()
        drawImage(
            image = sphere,
            dstOffset = IntOffset((cx - half).toInt(), (cy - half).toInt()),
            dstSize = IntSize(half * 2, half * 2)
        )

        val view = View(rotLon, rotLat)
        drawRoute(from, to, view, cx, cy, radius, scale, progress)
        drawCities(from, to, view, cx, cy, radius, scale)
        drawOceanLabels(view, cx, cy, radius, scale)
        }
    }
}

private fun DrawScope.drawRoute(
    from: City,
    to: City,
    view: View,
    cx: Float,
    cy: Float,
    radius: Float,
    scale: Float,
    progress: Float
) {
    val a = vecFrom(from.lat, from.lon)
    val b = vecFrom(to.lat, to.lon)
    val path = Path()
    var started = false
    val steps = 160
    for (i in 0..steps) {
        val v = slerp(a, b, i.toFloat() / steps.toFloat())
        val p = view.project(v[0], v[1], v[2], cx, cy, radius)
        if (p == null) {
            started = false
            continue
        }
        if (!started) {
            path.moveTo(p.x, p.y)
            started = true
        } else {
            path.lineTo(p.x, p.y)
        }
    }
    drawPath(
        path = path,
        color = Color(0xFFDCE7F5).copy(alpha = 0.85f),
        style = Stroke(width = if (scale > 2.5f) 3f else 1.8f)
    )

    val pa = view.project(a[0], a[1], a[2], cx, cy, radius)
    val pb = view.project(b[0], b[1], b[2], cx, cy, radius)
    if (pa != null) drawCircle(Color.White, if (scale > 2.5f) 5f else 3.5f, pa)
    if (pb != null) drawCircle(Color.White, if (scale > 2.5f) 5f else 3.5f, pb)

    if (progress < 0f) return
    val t = progress.coerceIn(0f, 1f)
    val cur = slerp(a, b, t)
    val pc = view.project(cur[0], cur[1], cur[2], cx, cy, radius) ?: return
    val glow = if (scale > 2.5f) 22f else 15f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
            center = pc,
            radius = glow
        ),
        radius = glow,
        center = pc
    )
    drawCircle(Color.White, if (scale > 2.5f) 5f else 4f, pc)
}

private fun DrawScope.drawCities(
    from: City,
    to: City,
    view: View,
    cx: Float,
    cy: Float,
    radius: Float,
    scale: Float
) {
    val focus = setOf(from.code, to.code)
    for (city in Cities.all) {
        val p = view.projectLatLon(city.lat.toFloat(), city.lon.toFloat(), cx, cy, radius) ?: continue
        if (focus.contains(city.code)) {
            drawCircle(Gold.copy(alpha = 0.9f), if (scale > 2.5f) 3.2f else 2.2f, p)
            if (scale > 1.6f) {
                drawLabel(city.name, p.x + 8f, p.y + 4f, Color(0xFFFFF1C8), 11f, left = true)
            }
        } else {
            drawCircle(Color.White.copy(alpha = 0.35f), 1.3f, p)
        }
    }
}

private fun DrawScope.drawOceanLabels(
    view: View,
    cx: Float,
    cy: Float,
    radius: Float,
    scale: Float
) {
    if (scale < 1.05f) return
    for (t in OCEAN_LABELS) {
        val p = view.projectLatLon(t.first, t.second, cx, cy, radius) ?: continue
        drawLabel(t.third, p.x, p.y, Color(0xFF7FA8C9).copy(alpha = 0.75f), 10f, left = false)
    }
}

private fun DrawScope.drawLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    sizePx: Float,
    left: Boolean
) {
    val paint = android.graphics.Paint()
    paint.isAntiAlias = true
    paint.color = color.toArgb()
    paint.textSize = sizePx
    paint.textAlign = if (left) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.CENTER
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

@Composable
fun GlobeCanvas(
    from: City,
    to: City,
    modifier: Modifier = Modifier,
    progress: Float = -1f,
    stars: Boolean = true,
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
        stars = stars,
        onCamera = { s, lo, la ->
            scale = s
            lon = lo
            lat = la
        },
        onTap = onTap
    )
}
