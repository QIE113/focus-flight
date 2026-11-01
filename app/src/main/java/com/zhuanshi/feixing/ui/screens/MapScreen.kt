package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.EarthTexture
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private fun unit(lat: Double, lon: Double): DoubleArray {
    val la = Math.toRadians(lat)
    val lo = Math.toRadians(lon)
    return doubleArrayOf(cos(la) * sin(lo), sin(la), cos(la) * cos(lo))
}

private fun arc(from: City, to: City, steps: Int): List<DoubleArray> {
    val a = unit(from.lat, from.lon)
    val b = unit(to.lat, to.lon)
    var dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    dot = dot.coerceIn(-1.0, 1.0)
    val omega = Math.acos(dot)
    val out = ArrayList<DoubleArray>(steps + 1)
    if (omega < 1e-6) {
        out.add(a)
        return out
    }
    val so = sin(omega)
    for (i in 0..steps) {
        val t = i.toDouble() / steps
        val ka = sin((1 - t) * omega) / so
        val kb = sin(t * omega) / so
        val x = a[0] * ka + b[0] * kb
        val y = a[1] * ka + b[1] * kb
        val z = a[2] * ka + b[2] * kb
        val lat = Math.toDegrees(Math.asin(y.coerceIn(-1.0, 1.0)))
        val lon = Math.toDegrees(Math.atan2(x, z))
        out.add(doubleArrayOf(lat, lon))
    }
    return out
}

private fun lonDelta(lon: Double, center: Double): Double {
    var d = lon - center
    while (d > 180) d -= 360
    while (d < -180) d += 360
    return d
}

/**
 * 全屏地图：像视频里那样是铺开的卫星底图，双指缩放、单指平移，
 * 底图来自内置的地球纹理，全程不联网。
 */
@Composable
fun MapScreen(
    from: City,
    to: City,
    progress: Float = -1f,
    onClose: () -> Unit
) {
    val cam = remember(from.code, to.code) {
        val mid = arc(from, to, 2)[1]
        floatArrayOf(mid[0].toFloat(), mid[1].toFloat())
    }
    var zoom by remember { mutableFloatStateOf(2.6f) }
    var centerLat by remember { mutableFloatStateOf(cam[0]) }
    var centerLon by remember { mutableFloatStateOf(cam[1]) }

    val distance = remember(from.code, to.code) { Geo.distanceKm(from, to) }
    val minutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }
    val route = remember(from.code, to.code) { arc(from, to, 128) }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val pxPerDeg = size.width * zoom / 360f
                        zoom = (zoom * gestureZoom).coerceIn(1f, 24f)
                        centerLon -= pan.x / pxPerDeg
                        centerLat += pan.y / pxPerDeg
                        while (centerLon > 180f) centerLon -= 360f
                        while (centerLon < -180f) centerLon += 360f
                        centerLat = centerLat.coerceIn(-85f, 85f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val worldW = w * zoom
            val worldH = worldW / 2f
            val originX = (centerLon + 180f) / 360f * worldW - w / 2f
            val originY = (90f - centerLat) / 180f * worldH - h / 2f

            val tex = EarthTexture.bitmap
            val tw = EarthTexture.WIDTH.toFloat()
            val th = EarthTexture.HEIGHT.toFloat()

            for (k in -1..1) {
                val left = -originX + k * worldW
                val top = -originY
                val dstLeft = maxOf(0f, left)
                val dstRight = minOf(w, left + worldW)
                val dstTop = maxOf(0f, top)
                val dstBottom = minOf(h, top + worldH)
                if (dstRight - dstLeft < 1f || dstBottom - dstTop < 1f) continue
                val srcLeft = ((dstLeft - left) / worldW * tw)
                val srcRight = ((dstRight - left) / worldW * tw)
                val srcTop = ((dstTop - top) / worldH * th)
                val srcBottom = ((dstBottom - top) / worldH * th)
                drawImage(
                    image = tex,
                    srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
                    srcSize = IntSize(
                        (srcRight - srcLeft).toInt().coerceAtLeast(1),
                        (srcBottom - srcTop).toInt().coerceAtLeast(1)
                    ),
                    dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                    dstSize = IntSize(
                        (dstRight - dstLeft).toInt().coerceAtLeast(1),
                        (dstBottom - dstTop).toInt().coerceAtLeast(1)
                    ),
                    filterQuality = FilterQuality.Medium
                )
            }

            val pxPerDeg = worldW / 360f
            fun sx(lon: Double): Float = (w / 2f + lonDelta(lon, centerLon.toDouble()).toFloat() * pxPerDeg)
            fun sy(lat: Double): Float = (h / 2f - (lat - centerLat) * pxPerDeg)

            val path = Path()
            var first = true
            for (p in route) {
                val x = sx(p[1])
                val y = sy(p[0])
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFF9FE8FF).copy(alpha = 0.55f),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            for (city in Cities.all) {
                val dd = abs(lonDelta(city.lon, centerLon.toDouble()))
                if (dd > 90) continue
                val x = sx(city.lon)
                val y = sy(city.lat)
                if (x < -20 || x > w + 20 || y < -20 || y > h + 20) continue
                val isEnd = city.code == from.code || city.code == to.code
                drawCircle(
                    color = if (isEnd) Color.White else Color.White.copy(alpha = 0.45f),
                    radius = if (isEnd) 4.5f else 2f,
                    center = Offset(x, y)
                )
                if (isEnd || zoom > 4f) {
                    val paint = android.graphics.Paint()
                    paint.isAntiAlias = true
                    paint.color = if (isEnd) Color.White.toArgb() else Color(0xFFBFD3E6).toArgb()
                    paint.textSize = if (isEnd) 30f else 24f
                    paint.textAlign = android.graphics.Paint.Align.LEFT
                    drawContext.canvas.nativeCanvas.drawText(city.name, x + 10f, y + 8f, paint)
                }
            }

            if (progress >= 0f) {
                val t = (progress.coerceIn(0f, 1f) * (route.size - 1)).toInt()
                val p = route[t]
                val x = sx(p[1])
                val y = sy(p[0])
                drawCircle(
                    color = Color(0xFF2B7BD6).copy(alpha = 0.35f),
                    radius = 22f,
                    center = Offset(x, y)
                )
                drawCircle(color = Color.White, radius = 7f, center = Offset(x, y))
                drawCircle(
                    color = Color(0xFF2B7BD6),
                    radius = 11f,
                    center = Offset(x, y),
                    style = Stroke(width = 2.5f)
                )
            }

            val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
            val graticule = Color.White.copy(alpha = 0.08f)
            var gl = -180
            while (gl <= 180) {
                val x = sx(gl.toDouble())
                if (x >= 0 && x <= w) {
                    drawLine(graticule, Offset(x, 0f), Offset(x, h), 1f, pathEffect = dashed)
                }
                gl += 30
            }
            var ga = -80
            while (ga <= 80) {
                val y = sy(ga.toDouble())
                if (y >= 0 && y <= h) {
                    drawLine(graticule, Offset(0f, y), Offset(w, y), 1f, pathEffect = dashed)
                }
                ga += 30
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "返回",
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PanelBg)
                    .clickable { onClose() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Text(
                from.code + " → " + to.code + "　" + Geo.formatDuration(minutes),
                color = TextDim,
                fontSize = 11.sp
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapButton("+") { zoom = (zoom * 1.5f).coerceAtMost(24f) }
            MapButton("−") { zoom = (zoom / 1.5f).coerceAtLeast(1f) }
            MapButton("◎") {
                zoom = 2.6f
                centerLat = cam[0]
                centerLon = cam[1]
            }
            MapButton("看") {
                zoom = 6f
                centerLat = from.lat.toFloat()
                centerLon = from.lon.toFloat()
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PanelBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("剩余飞行时间", color = TextDim, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Geo.formatDuration(if (progress < 0f) minutes else (minutes * (1f - progress)).roundToInt().coerceAtLeast(0)),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("剩余飞行距离", color = TextDim, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Geo.formatDistance(if (progress < 0f) distance else distance * (1.0 - progress)),
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MapButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(PanelBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 15.sp)
    }
}
