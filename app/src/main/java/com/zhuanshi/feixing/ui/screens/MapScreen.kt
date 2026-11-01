package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.EarthTexture
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MapScreen(
    from: City,
    to: City,
    progress: Float,
    remainingSeconds: Int,
    remainingKm: Double,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1.4f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val texture = remember { EarthTexture.bitmap }

    Box(
        Modifier
            .fillMaxSize()
            .background(NightBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, drag, zoomChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 14f)
                    pan = pan + drag
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val mapW = size.width * scale
            val mapH = mapW * 0.5f
            val left = size.width / 2f + pan.x - mapW / 2f
            val top = size.height / 2f + pan.y - mapH / 2f
            drawImage(
                image = texture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(texture.width, texture.height),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(mapW.roundToInt(), mapH.roundToInt()),
                filterQuality = FilterQuality.Low
            )
            var lon = -180
            while (lon <= 180) {
                val x = left + (lon + 180) / 360f * mapW
                drawLine(Color(0x22FFFFFF), Offset(x, top), Offset(x, top + mapH), strokeWidth = 1f)
                lon += 15
            }
            var lat = -75
            while (lat <= 75) {
                val y = top + (90 - lat) / 180f * mapH
                drawLine(Color(0x22FFFFFF), Offset(left, y), Offset(left + mapW, y), strokeWidth = 1f)
                lat += 15
            }
            val la1 = Math.toRadians(from.lat)
            val lo1 = Math.toRadians(from.lon)
            val la2 = Math.toRadians(to.lat)
            val lo2 = Math.toRadians(to.lon)
            val h = sin((la2 - la1) / 2) * sin((la2 - la1) / 2) +
                    cos(la1) * cos(la2) * sin((lo2 - lo1) / 2) * sin((lo2 - lo1) / 2)
            val d = 2 * asin(min(1.0, sqrt(h)))
            val safe = if (d < 1e-6) 1e-6 else d
            val path = Path()
            var px = 0f
            var py = 0f
            var i = 0
            while (i <= 64) {
                val f = i / 64.0
                val a = sin((1 - f) * safe) / sin(safe)
                val b = sin(f * safe) / sin(safe)
                val cx = a * cos(la1) * cos(lo1) + b * cos(la2) * cos(lo2)
                val cy = a * cos(la1) * sin(lo1) + b * cos(la2) * sin(lo2)
                val cz = a * sin(la1) + b * sin(la2)
                val latD = Math.toDegrees(asin(cz.coerceIn(-1.0, 1.0)))
                val lonD = Math.toDegrees(atan2(cy, cx))
                val x = left + ((lonD + 180) / 360.0).toFloat() * mapW
                val y = top + ((90 - latD) / 180.0).toFloat() * mapH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                if (kotlin.math.abs(f - progress) < 0.012) {
                    px = x
                    py = y
                }
                i++
            }
            drawPath(path, Gold.copy(alpha = 0.75f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            val fx = left + ((from.lon + 180) / 360.0).toFloat() * mapW
            val fy = top + ((90 - from.lat) / 180.0).toFloat() * mapH
            drawCircle(Gold.copy(alpha = 0.25f), 8f, Offset(fx, fy))
            drawCircle(Gold, 3.5f, Offset(fx, fy))
            val tx = left + ((to.lon + 180) / 360.0).toFloat() * mapW
            val ty = top + ((90 - to.lat) / 180.0).toFloat() * mapH
            drawCircle(Color.White.copy(alpha = 0.22f), 10f, Offset(tx, ty))
            drawCircle(Color.White, 4f, Offset(tx, ty))
            if (px != 0f) {
                drawCircle(Gold.copy(alpha = 0.3f), 12f, Offset(px, py))
                drawCircle(Color.White, 4.5f, Offset(px, py))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(from.code + " → " + to.code, color = TextPrimary, fontSize = 15.sp, letterSpacing = 2.sp)
                Text(
                    text = "双指缩放 · 单指平移 · " + scale.toInt() + "x",
                    color = TextDim,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                );
            }
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .pressScale(src, 0.86f)
                    .border(1.dp, Color(0xFF262D39), RoundedCornerShape(30.dp))
                    .clickable(interactionSource = src, indication = null, onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) { Text("收起", color = TextPrimary.copy(alpha = 0.85f), fontSize = 12.sp) }
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ZoomButton("＋") { scale = (scale * 1.6f).coerceIn(1f, 14f) }
            ZoomButton("－") { scale = (scale / 1.6f).coerceIn(1f, 14f) }
            ZoomButton("◎") {
                scale = 1.6f
                pan = Offset.Zero
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp)
                .background(Color(0xCC0A0E15), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF1B212B), RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("剩余时间", color = TextDim, fontSize = 10.sp)
                    Text(Geo.formatClock(remainingSeconds), color = TextPrimary, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("剩余距离", color = TextDim, fontSize = 10.sp)
                    Text(Geo.formatDistance(remainingKm), color = Gold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
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
