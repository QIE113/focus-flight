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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** 自转速度：度 / 秒。 */
private const val SPIN_DEG = 2.4f

/** 原始贴图宽高。 */
private const val TEX_W = 1024
private const val TEX_H = 512

/** 贴图横向放大倍数。越大，旋转的最小步进越小，越不容易看出「顿」。 */
private const val TEX_SCALE = 4

/** 放大后的贴图宽度，必须是 2 的幂，方便按位取模。 */
private const val TEX_W2 = TEX_W * TEX_SCALE

/** TEX_W2 的二进制位宽，用于行号乘法。 */
private const val TEX_SHIFT = 12

/**
 * 球面几何工具：把经纬度正交投影到屏幕上（和贴图渲染使用同一套倾斜角）。
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
 * 预计算 + 逐帧查表的地球渲染器。
 *
 * 三个关键点：
 * 1. 预计算、逐帧渲染都跑在 Dispatchers.Default，主线程只负责把现成位图画出去，
 *    所以再怎么算都不会掉帧；
 * 2. 贴图先横向放大 4 倍（4096 分度），旋转按 4096 取模，屏幕上每次位移只有 0.35 像素，
 *    肉眼是连续的。旧版按 1024 分度，每秒只跳 5 次，那才是看着「卡」的真原因；
 * 3. 三缓冲，避免正在显示的那张被下一帧改写。
 */
class SphereRenderer(val diameter: Int) {
    private val size = diameter.coerceAtMost(720)
    private val n = size * size
    private val mask = TEX_W2 - 1

    private val base = ShortArray(n)
    private val row = ShortArray(n)
    private val shade = ByteArray(n)
    private val alpha = ByteArray(n)
    private val buf = IntArray(n)

    private val frames = Array(3) { Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888) }
    private var flip = 0

    @Volatile
    private var tex: IntArray? = null

    /** 一周被切成多少份，调用方用它判断画面是否需要重画。 */
    val steps: Int get() = TEX_W2

    /** 准备贴图与查找表。必须在后台线程调用。 */
    suspend fun prepare() {
        if (tex != null) return
        withContext(Dispatchers.Default) {
            val up = upsample()
            buildTable()
            tex = up
        }
    }

    /** 把 1024 宽的贴图横向双线性放大到 4096 宽。 */
    private fun upsample(): IntArray {
        val src = EarthTexture.pixels
        val out = IntArray(TEX_W2 * TEX_H)
        val wMask = TEX_W - 1
        var y = 0
        while (y < TEX_H) {
            val s = y * TEX_W
            val d = y * TEX_W2
            var x = 0
            while (x < TEX_W2) {
                val i0 = x shr 2
                val f = x and 3
                val c0 = src[s + i0]
                if (f == 0) {
                    out[d + x] = c0
                } else {
                    val c1 = src[s + ((i0 + 1) and wMask)]
                    val w0 = (4 - f) shl 6
                    val w1 = f shl 6
                    val r0 = (c0 shr 16) and 0xFF
                    val g0 = (c0 shr 8) and 0xFF
                    val b0 = c0 and 0xFF
                    val r1 = (c1 shr 16) and 0xFF
                    val g1 = (c1 shr 8) and 0xFF
                    val b1 = c1 and 0xFF
                    val rr = (r0 * w0 + r1 * w1) shr 8
                    val gg = (g0 * w0 + g1 * w1) shr 8
                    val bb = (b0 * w0 + b1 * w1) shr 8
                    out[d + x] = (-16777216) or (rr shl 16) or (gg shl 8) or bb
                }
                x++
            }
            y++
        }
        return out
    }

    /** 逐像素算出贴图坐标、光照强度、边缘透明度，之后每帧只查表。 */
    private fun buildTable() {
        val r = size / 2f
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
        while (y < size) {
            val sy = ((y + 0.5f) - r) / r
            var x = 0
            while (x < size) {
                val sx = ((x + 0.5f) - r) / r
                val i = y * size + x
                val d2 = sx * sx + sy * sy
                if (d2 >= 1f) {
                    alpha[i] = 0
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
                    base[i] = (((u * TEX_W2).toInt()) and mask).toShort()
                    val vi = ((0.5f - lat / PI_F) * TEX_H).toInt().coerceIn(0, TEX_H - 1)
                    row[i] = vi.toShort()
                    val diff = (nx * lx + ny * ly + nz * lz).coerceAtLeast(0f)
                    val lit = (0.18f + 0.95f * diff).coerceIn(0f, 1f)
                    shade[i] = (lit * 255f).toInt().toByte()
                    val a = ((1f - sqrt(d2)) * r / 1.6f).coerceIn(0f, 1f)
                    alpha[i] = (a * 255f).toInt().toByte()
                }
                x++
            }
            y++
        }
    }

    /** 按给定视角经度渲染一帧。必须在后台线程调用。 */
    fun render(viewLon: Float): ImageBitmap {
        val t = tex ?: return frames[0].asImageBitmap()
        val shift = ((viewLon / 360f) * TEX_W2).roundToInt()
        var i = 0
        while (i < n) {
            val a = alpha[i].toInt() and 0xFF
            if (a == 0) {
                buf[i] = 0
            } else {
                val c = t[(row[i].toInt() shl TEX_SHIFT) + ((base[i].toInt() + shift) and mask)]
                val sh = shade[i].toInt() and 0xFF
                val rr = (((c shr 16) and 0xFF) * sh) shr 8
                val gg = (((c shr 8) and 0xFF) * sh) shr 8
                val bb = ((c and 0xFF) * sh) shr 8
                buf[i] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
            i++
        }
        val bmp = frames[flip]
        flip = if (flip == 2) 0 else flip + 1
        bmp.setPixels(buf, 0, size, 0, 0, size, size)
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
        val density = LocalDensity.current
        val sideDp = min(maxWidth.value, maxHeight.value) * sphereFill
        val diameter = remember(sideDp) {
            with(density) { sideDp.dp.toPx() }.roundToInt().coerceIn(140, 720)
        }
        val baseLon = centerLon ?: if (from != null && to != null) SphereMath.midLon(from, to) else 108f
        val renderer = remember(diameter) { SphereRenderer(diameter) }
        var image by remember(renderer) { mutableStateOf<ImageBitmap?>(null) }
        var viewLon by remember(renderer, baseLon) { mutableFloatStateOf(baseLon - 10f) }
        var ready by remember(renderer) { mutableStateOf(false) }

        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val radiusPx = diameter / 2f
        val glow = remember(wPx, hPx, radiusPx) {
            if (wPx.isFinite() && hPx.isFinite() && wPx > 0f && hPx > 0f) {
                Brush.radialGradient(
                    colors = listOf(Color(0x3A4EA8FF), Color(0x1A0E4B8A), Color(0x00000000)),
                    center = Offset(wPx / 2f, hPx / 2f),
                    radius = radiusPx * 1.28f
                )
            } else {
                null
            }
        }

        // 预计算放到后台，主线程不会有一次性白卡。
        LaunchedEffect(renderer) {
            renderer.prepare()
            ready = true
        }

        LaunchedEffect(renderer, spin, baseLon, ready) {
            if (!ready) return@LaunchedEffect
            var last = 0L
            var lastStep = Int.MIN_VALUE
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                    last = now
                    if (spin && dt > 0f) {
                        var next = viewLon - dt * SPIN_DEG
                        while (next < -180f) next += 360f
                        while (next > 180f) next -= 360f
                        viewLon = next
                    }
                }
                val step = ((viewLon / 360f) * renderer.steps).roundToInt()
                if (step != lastStep) {
                    lastStep = step
                    val lon = viewLon
                    image = withContext(Dispatchers.Default) { renderer.render(lon) }
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val g = glow
            if (g != null) {
                drawCircle(brush = g, radius = radiusPx * 1.28f, center = center)
            }
            val img = image
            if (img != null) {
                drawImage(
                    image = img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset((center.x - radiusPx).roundToInt(), (center.y - radiusPx).roundToInt()),
                    dstSize = IntSize(diameter, diameter),
                    filterQuality = FilterQuality.Low
                )
            }
            if (showRoute && from != null && to != null) {
                drawGreatCircle(from, to, viewLon, radiusPx, center, progress, accent)
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