package com.zhuanshi.feixing.ui.globe

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.EarthTexture
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val PI_F = 3.1415927f

/**
 * 球面几何工具：把经纬度正交投影到屏幕上（和贴图渲染使用的是同一套倾斜角）。
 */
object SphereMath {
    const val TILT_DEG = 16f

    fun midLon(a: City, b: City): Float {
        var d = b.lon - a.lon
        while (d > 180.0) d -= 360.0
        while (d < -180.0) d += 360.0
        var m = a.lon + d / 2.0
        if (m > 180.0) m -= 360.0
        if (m < -180.0) m += 360.0
        return m.toFloat()
    }

    fun project(latDeg: Float, lonDeg: Float, viewLon: Float, radius: Float, center: Offset): Offset? {
        val la = latDeg * PI_F / 180f
        val dLon = (lonDeg - viewLon) * PI_F / 180f
        val x = cos(la) * sin(dLon)
        val y = sin(la)
        val z = cos(la) * cos(dLon)
        val t = TILT_DEG * PI_F / 180f
        val ny = y * cos(t) + z * sin(t)
        val nz = -y * sin(t) + z * cos(t)
        if (nz <= 0.02f) return null
        return Offset(center.x + x * radius, center.y - ny * radius)
    }
}

/**
 * 一次预计算、之后逐帧只做整数查表的地球渲染器。
 * 每帧成本 = 直径^2 次数组读取 + 一次 setPixels，因此可以稳定跑满 60fps。
 */
class SphereRenderer(val diameter: Int) {
    private val n = diameter * diameter
    private val tex = EarthTexture.pixels
    private val texW = 1024
    private val texH = 512
    private val mask = texW - 1
    private val uBase = ShortArray(n)
    private val vIdx = ShortArray(n)
    private val shade = ByteArray(n)
    private val edge = ByteArray(n)
    private val buf = IntArray(n)
    private val frames = arrayOf(
        Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888),
        Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
    )
    private var flip = 0

    init {
        val r = diameter / 2f
        val t = SphereMath.TILT_DEG * PI_F / 180f
        val ct = cos(t)
        val st = sin(t)
        var lx = -0.40f
        var ly = -0.48f
        var lz = 0.78f
        val ll = sqrt(lx * lx + ly * ly + lz * lz)
        lx /= ll
        ly /= ll
        lz /= ll
        var y = 0
        while (y < diameter) {
            val sy = ((y + 0.5f) - r) / r
            var x = 0
            while (x < diameter) {
                val sx = ((x + 0.5f) - r) / r
                val i = y * diameter + x
                val d2 = sx * sx + sy * sy
                if (d2 >= 1f) {
                    edge[i] = 0
                } else {
                    val z = sqrt(1f - d2)
                    val nx = sx
                    val ny = -sy
                    val nz = z
                    val py = ny * ct - nz * st
                    val pz = ny * st + nz * ct
                    val lat = asin(py.coerceIn(-1f, 1f))
                    val lon = atan2(nx, pz)
                    var u = lon / (2f * PI_F) + 0.5f
                    if (u < 0f) u += 1f
                    if (u >= 1f) u -= 1f
                    val ui = ((u * texW).toInt()) and mask
                    uBase[i] = ui.toShort()
                    val vi = ((0.5f - lat / PI_F) * texH).toInt().coerceIn(0, texH - 1)
                    vIdx[i] = vi.toShort()
                    val diff = (nx * lx + ny * ly + nz * lz).coerceAtLeast(0f)
                    val lit = (0.18f + 0.95f * diff).coerceIn(0f, 1f)
                    shade[i] = (lit * 255f).toInt().toByte()
                    val a = ((1f - sqrt(d2)) * r / 1.6f).coerceIn(0f, 1f)
                    edge[i] = (a * 255f).toInt().toByte()
                }
                x++
            }
            y++
        }
    }

    fun render(viewLon: Float): ImageBitmap {
        val shift = ((viewLon / 360f) * texW).roundToInt()
        var i = 0
        while (i < n) {
            val a = edge[i].toInt() and 0xFF
            if (a == 0) {
                buf[i] = 0
            } else {
                val col = tex[vIdx[i].toInt() * texW + ((uBase[i].toInt() + shift) and mask)]
                val sh = shade[i].toInt() and 0xFF
                val rr = (((col shr 16) and 0xFF) * sh) shr 8
                val gg = (((col shr 8) and 0xFF) * sh) shr 8
                val bb = ((col and 0xFF) * sh) shr 8
                buf[i] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
            i++
        }
        val bmp = frames[flip]
        flip = 1 - flip
        bmp.setPixels(buf, 0, diameter, 0, 0, diameter, diameter)
        return bmp.asImageBitmap()
    }
}

@Composable
fun Globe(
    modifier: Modifier = Modifier,
    from: City? = null,
    to: City? = null,
    progress: Float = 0f,
    centerLon: Float? = null,
    spin: Boolean = true,
    showRoute: Boolean = true,
    sphereFill: Float = 0.94f,
    accent: Color = Color(0xFFF5D06F)
) {
    BoxWithConstraints(modifier) {
        val sideDp = min(maxWidth.value, maxHeight.value) * sphereFill
        val density = LocalDensity.current
        val diameter = remember(sideDp) {
            with(density) { sideDp.dp.toPx() }.roundToInt().coerceIn(140, 720)
        }
        val baseLon = centerLon ?: if (from != null && to != null) SphereMath.midLon(from, to) else 108f
        val renderer = remember(diameter) { SphereRenderer(diameter) }
        var image by remember(renderer) { mutableStateOf<ImageBitmap?>(null) }
        var viewLon by remember(renderer, baseLon) { mutableFloatStateOf(baseLon - 10f) }

        LaunchedEffect(renderer, spin, baseLon) {
            var last = 0L
            var drawn = Float.NaN
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else (now - last) / 1000000000f
                    last = now
                    if (spin) {
                        var next = viewLon - dt * 2.4f
                        while (next < -180f) next += 360f
                        while (next > 180f) next -= 360f
                        viewLon = next
                    }
                    if (drawn.isNaN() || abs(viewLon - drawn) > 0.45f) {
                        image = renderer.render(viewLon)
                        drawn = viewLon
                    }
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3A4EA8FF), Color(0x1A0E4B8A), Color(0x00000000)),
                    center = center,
                    radius = radius * 1.28f
                ),
                radius = radius * 1.28f,
                center = center
            )
            val img = image
            if (img != null) {
                drawImage(
                    image = img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
                    dstSize = IntSize(diameter, diameter),
                    filterQuality = FilterQuality.Low
                )
            }
            if (showRoute && from != null && to != null) {
                drawGreatCircle(from, to, viewLon, radius, center, progress, accent)
            }
        }
    }
}

private fun DrawScope.drawGreatCircle(
    from: City,
    to: City,
    viewLon: Float,
    radius: Float,
    center: Offset,
    progress: Float,
    accent: Color
) {
    val la1 = Math.toRadians(from.lat)
    val lo1 = Math.toRadians(from.lon)
    val la2 = Math.toRadians(to.lat)
    val lo2 = Math.toRadians(to.lon)
    val h = sin((la2 - la1) / 2) * sin((la2 - la1) / 2) +
            cos(la1) * cos(la2) * sin((lo2 - lo1) / 2) * sin((lo2 - lo1) / 2)
    val d = 2 * asin(min(1.0, sqrt(h)))
    val safeD = if (d < 1e-6) 1e-6 else d
    val path = Path()
    var started = false
    var plane = Offset.Zero
    var prev = Offset.Zero
    var next = Offset.Zero
    var i = 0
    while (i <= 48) {
        val f = i / 48.0
        val a = sin((1 - f) * safeD) / sin(safeD)
        val b = sin(f * safeD) / sin(safeD)
        val cx = a * cos(la1) * cos(lo1) + b * cos(la2) * cos(lo2)
        val cy = a * cos(la1) * sin(lo1) + b * cos(la2) * sin(lo2)
        val cz = a * sin(la1) + b * sin(la2)
        val lat = Math.toDegrees(asin(cz.coerceIn(-1.0, 1.0))).toFloat()
        val lon = Math.toDegrees(atan2(cy, cx)).toFloat()
        val p = SphereMath.project(lat, lon, viewLon, radius, center)
        if (p == null) {
            started = false
        } else {
            if (started) path.lineTo(p.x, p.y) else path.moveTo(p.x, p.y)
            started = true
        }
        if (abs(f - progress) < 0.028) {
            if (p != null) plane = p
        }
        if (abs(f - (progress - 0.03f)) < 0.028 && p != null) prev = p
        if (abs(f - (progress + 0.03f)) < 0.028 && p != null) next = p
        i++
    }
    drawPath(path, accent.copy(alpha = 0.5f), style = Stroke(width = radius * 0.014f, cap = StrokeCap.Round))
    val fromP = SphereMath.project(from.lat.toFloat(), from.lon.toFloat(), viewLon, radius, center)
    if (fromP != null) {
        drawCircle(accent.copy(alpha = 0.22f), radius * 0.07f, fromP)
        drawCircle(accent, radius * 0.026f, fromP)
    }
    val toP = SphereMath.project(to.lat.toFloat(), to.lon.toFloat(), viewLon, radius, center)
    if (toP != null) {
        drawCircle(accent.copy(alpha = 0.22f), radius * 0.09f, toP)
        drawCircle(Color.White, radius * 0.026f, toP)
    }
    if (plane != Offset.Zero) {
        val dir = if (next != Offset.Zero && prev != Offset.Zero) next - prev else Offset(1f, 0f)
        val deg = Math.toDegrees(atan2(dir.y.toDouble(), dir.x.toDouble())).toFloat()
        rotate(deg, pivot = plane) {
            val s2 = radius * 0.058f
            val p2 = Path()
            p2.moveTo(plane.x + s2, plane.y)
            p2.lineTo(plane.x - s2 * 0.72f, plane.y + s2 * 0.66f)
            p2.lineTo(plane.x - s2 * 0.34f, plane.y)
            p2.lineTo(plane.x - s2 * 0.72f, plane.y - s2 * 0.66f)
            p2.close()
            drawPath(p2, Color.White)
        }
        drawCircle(accent.copy(alpha = 0.3f), radius * 0.1f, plane)
    }
}
