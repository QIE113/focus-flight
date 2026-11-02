package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.map.Camera
import com.zhuanshi.feixing.map.Mercator
import com.zhuanshi.feixing.map.Projection
import com.zhuanshi.feixing.map.TileMap
import com.zhuanshi.feixing.map.TileSource
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private fun wrapLon(v: Double): Double {
    var x = (v + 540.0) % 360.0
    if (x < 0) x += 360.0
    return x - 180.0
}

/**
 * 按视频做法画航线：真实大圆插值，白色/金色描边，终点带光圈，
 * progress 位置放飞机。地图缩放级别越高线越细。
 */
fun DrawScope.drawFlightRoute(
    proj: Projection,
    from: City,
    to: City,
    progress: Float,
    planeAlpha: Float = 1f
) {
    val la1 = Math.toRadians(from.lat)
    val lo1 = Math.toRadians(from.lon)
    val la2 = Math.toRadians(to.lat)
    val lo2 = Math.toRadians(to.lon)
    val h = sin((la2 - la1) / 2) * sin((la2 - la1) / 2) +
            cos(la1) * cos(la2) * sin((lo2 - lo1) / 2) * sin((lo2 - lo1) / 2)
    val d = 2.0 * asin(min(1.0, sqrt(h)))
    val safe = if (d < 1e-6) 1e-6 else d

    val path = Path()
    var plane = Offset.Unspecified
    var i = 0
    while (i <= 128) {
        val f = i / 128.0
        val a = sin((1 - f) * safe) / sin(safe)
        val b = sin(f * safe) / sin(safe)
        val cx = a * cos(la1) * cos(lo1) + b * cos(la2) * cos(lo2)
        val cy = a * cos(la1) * sin(lo1) + b * cos(la2) * sin(lo2)
        val cz = a * sin(la1) + b * sin(la2)
        val latD = Math.toDegrees(asin(cz.coerceIn(-1.0, 1.0)))
        val lonD = Math.toDegrees(atan2(cy, cx))
        val p = proj.toScreen(latD, lonD)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        if (abs(f - progress) < 0.005) plane = p
        i++
    }

    drawPath(path, Color(0x66FFFFFF), style = Stroke(width = 3.4f, cap = StrokeCap.Round))
    drawPath(path, Gold.copy(alpha = 0.92f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

    val start = proj.toScreen(from.lat, from.lon)
    drawCircle(Gold.copy(alpha = 0.22f), 12f, start)
    drawCircle(Gold, 4f, start)

    val end = proj.toScreen(to.lat, to.lon)
    drawCircle(Color.White.copy(alpha = 0.16f), 16f, end)
    drawCircle(Color.White.copy(alpha = 0.85f), 4.5f, end)

    if (plane != Offset.Unspecified && planeAlpha > 0.01f) {
        drawCircle(Gold.copy(alpha = 0.30f * planeAlpha), 15f, plane)
        drawCircle(Color.White.copy(alpha = planeAlpha), 5.5f, plane)
        drawCircle(Gold.copy(alpha = planeAlpha), 2.4f, plane)
    }
}

/** 比例尺。zoom 越大条越短。 */
fun DrawScope.drawScaleBar(proj: Projection, anchor: Offset) {
    val mpp = proj.metersPerPixel()
    val targetPx = 96.0
    val meters = mpp * targetPx
    val nice = niceRound(meters)
    val px = (nice / mpp).toFloat()
    if (px < 8f || px > size.width * 0.7f) return
    val c = Color(0x99FFFFFF)
    drawLine(c, anchor, Offset(anchor.x + px, anchor.y), 2f)
    drawLine(c, Offset(anchor.x, anchor.y - 5f), Offset(anchor.x, anchor.y + 5f), 2f)
    drawLine(c, Offset(anchor.x + px, anchor.y - 5f), Offset(anchor.x + px, anchor.y + 5f), 2f)
}

private fun niceRound(meters: Double): Double {
    if (meters <= 0.0) return 0.0
    val exp = Math.floor(Math.log10(meters))
    val base = Math.pow(10.0, exp)
    val n = meters / base
    val mult = when {
        n < 1.5 -> 1.0
        n < 3.5 -> 2.0
        n < 7.5 -> 5.0
        else -> 10.0
    }
    return mult * base
}

@Composable
fun MapScreen(
    from: City,
    to: City,
    progress: Float,
    remainingSeconds: Int,
    remainingKm: Double,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val zoom = remember { Animatable(5.4f) }
    var centerLat by remember { mutableStateOf(from.lat) }
    var centerLon by remember { mutableStateOf(from.lon) }
    var source by remember { mutableStateOf(TileSource.SATELLITE) }

    val camera = Camera(centerLat, centerLon, zoom.value)

    Box(Modifier.fillMaxSize().background(Color(0xFF05070C))) {
        TileMap(
            camera = camera,
            source = source,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val mpp = Mercator.metersPerPixel(centerLat, zoom.value)
                        val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.10)
                        centerLat = (centerLat + pan.y * mpp / 110540.0).coerceIn(-84.0, 84.0)
                        centerLon = wrapLon(centerLon - pan.x * mpp / (111320.0 * cosLat))
                        if (zoomChange != 1f) {
                            val next = (zoom.value + log2(zoomChange)).coerceIn(2.6f, 16.4f)
                            scope.launch { zoom.snapTo(next) }
                        }
                    }
                }
        ) { proj ->
            drawFlightRoute(proj, from, to, progress)
            drawScaleBar(proj, Offset(24f, size.height - 132f))
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = from.code + "  →  " + to.code,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = String.format("%.1f", zoom.value) + "x · " + source.label + "底图",
                        color = TextDim,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                val src = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .pressScale(src, 0.88f)
                        .border(1.dp, Color(0xFF262D39), RoundedCornerShape(30.dp))
                        .clickable(interactionSource = src, indication = null) {
                            source = if (source === TileSource.SATELLITE) TileSource.DARK else TileSource.SATELLITE
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("换底图", color = TextPrimary.copy(alpha = 0.85f), fontSize = 11.sp) }
                val src2 = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .pressScale(src2, 0.88f)
                        .border(1.dp, Color(0xFF262D39), RoundedCornerShape(30.dp))
                        .clickable(interactionSource = src2, indication = null, onClick = onClose)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("收起", color = TextPrimary.copy(alpha = 0.85f), fontSize = 11.sp) }
            }
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ZoomButton("＋") { scope.launch { zoom.animateTo((zoom.value + 1.2f).coerceIn(2.6f, 16.4f), tween(260, easing = FastOutSlowInEasing)) } }
            ZoomButton("－") { scope.launch { zoom.animateTo((zoom.value - 1.2f).coerceIn(2.6f, 16.4f), tween(260, easing = FastOutSlowInEasing)) } }
            ZoomButton("◎") {
                scope.launch {
                    centerLat = from.lat
                    centerLon = from.lon
                    zoom.animateTo(13.5f, tween(700, easing = FastOutSlowInEasing))
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        ) {
            Text(
                text = source.attribution,
                color = TextDim.copy(alpha = 0.55f),
                fontSize = 8.sp,
                modifier = Modifier.padding(start = 22.dp, bottom = 6.dp)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp)
                    .background(Color(0xCC0A0E15), RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0xFF1B212B), RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("剩余时间", color = TextDim, fontSize = 10.sp)
                        Text(
                            Geo.formatClock(remainingSeconds),
                            color = TextPrimary,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("剩余距离", color = TextDim, fontSize = 10.sp)
                        Text(
                            Geo.formatDistance(remainingKm),
                            color = Gold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomButton(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(42.dp)
            .pressScale(src, 0.86f)
            .background(Color(0xCC0A0E15), CircleShape)
            .border(1.dp, Color(0xFF262D39), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = TextPrimary, fontSize = 15.sp) }
}
