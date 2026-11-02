package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.map.Camera
import com.zhuanshi.feixing.map.TileMap
import com.zhuanshi.feixing.map.TileSource
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.GoldButton
import com.zhuanshi.feixing.ui.RoundAction
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 大圆插值：f = 0 在 from，f = 1 在 to。返回 [lat, lon]。 */
private fun greatCirclePoint(from: City, to: City, f: Double): DoubleArray {
    val la1 = Math.toRadians(from.lat)
    val lo1 = Math.toRadians(from.lon)
    val la2 = Math.toRadians(to.lat)
    val lo2 = Math.toRadians(to.lon)
    val h = sin((la2 - la1) / 2) * sin((la2 - la1) / 2) +
            cos(la1) * cos(la2) * sin((lo2 - lo1) / 2) * sin((lo2 - lo1) / 2)
    val d = 2.0 * asin(min(1.0, sqrt(h)))
    if (d < 1e-9) return doubleArrayOf(from.lat, from.lon)
    val a = sin((1.0 - f) * d) / sin(d)
    val b = sin(f * d) / sin(d)
    val x = a * cos(la1) * cos(lo1) + b * cos(la2) * cos(lo2)
    val y = a * cos(la1) * sin(lo1) + b * cos(la2) * sin(lo2)
    val z = a * sin(la1) + b * sin(la2)
    return doubleArrayOf(Math.toDegrees(asin(z.coerceIn(-1.0, 1.0))), Math.toDegrees(atan2(y, x)))
}

/** 舱门关闭 → 出发。视频里是纯黑 + 飞机图标 + 两句文案。 */
@Composable
fun CabinScreen(from: City, to: City, onGo: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF04060A), Color(0xFF03050A))
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Appear(riseDp = 10f) {
                Text("✈", color = Color(0xFFDFE6EF), fontSize = 26.sp)
            }
            Spacer(Modifier.height(28.dp))
            Appear(delayMillis = 140, riseDp = 14f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("舱门关闭，", color = Color(0xFFE8EDF3), fontSize = 19.sp, letterSpacing = 2.sp)
                    Text(
                        "飞行即将开始",
                        color = Color(0xFFE8EDF3),
                        fontSize = 19.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
            Appear(delayMillis = 340, riseDp = 18f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val src = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .pressScale(src, 0.94f)
                            .background(Color(0xFFF2F5F8), RoundedCornerShape(40.dp))
                            .clickable(interactionSource = src, indication = null, onClick = onGo)
                            .padding(horizontal = 46.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "出发！",
                            color = Color(0xFF0A0E15),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        )
                    }
                    Text(
                        text = from.code + " → " + to.code,
                        color = TextDim.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FlightScreen(
    from: City,
    to: City,
    seat: String,
    scene: String,
    minutes: Int,
    km: Double,
    onArrived: () -> Unit
) {
    val total = minutes * 60
    var elapsed by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var mapOpen by remember { mutableStateOf(false) }
    var focusMode by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(TileSource.SATELLITE) }

    val push = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        push.animateTo(1f, tween(2600, easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)))
    }
    LaunchedEffect(paused) {
        while (!paused && elapsed < total) {
            delay(1000)
            elapsed += 1
        }
    }
    LaunchedEffect(elapsed) { if (elapsed >= total) onArrived() }

    val progress = if (total <= 0) 0f else elapsed.toFloat() / total
    val remainSec = (total - elapsed).coerceAtLeast(0)
    val remainKm = km * (1f - progress)

    // 镜头：先按视频从 zoom 3.4 推到机场 14.6，再随时间缓慢拉远看全程
    val tour = (elapsed / 360f).coerceIn(0f, 1f)
    val zoomNow = 14.6f - 9.6f * tour
    val here = greatCirclePoint(from, to, progress.toDouble())
    val camera = Camera(here[0], here[1], zoomNow)

    Box(Modifier.fillMaxSize().background(NightBg)) {
        TileMap(
            camera = camera,
            source = source,
            modifier = Modifier.fillMaxSize()
        ) { proj ->
            drawFlightRoute(proj, from, to, progress)
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xE605070C), Color(0x6605070C), Color(0xF205070C))
                )
            )
        )

        val hud = ((push.value - 0.5f) / 0.5f).coerceIn(0f, 1f)

        Column(Modifier.fillMaxSize().graphicsLayer { alpha = hud }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(from.code, color = TextPrimary, fontSize = 18.sp, letterSpacing = 2.sp)
                    Text(from.name, color = TextDim, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("· " + scene + " ·", color = Gold.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(seat, color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(to.code, color = TextPrimary, fontSize = 18.sp, letterSpacing = 2.sp)
                    Text(to.name, color = TextDim, fontSize = 10.sp)
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("剩余", color = TextDim, fontSize = 10.sp, letterSpacing = 3.sp)
                    Text(
                        text = Geo.formatClock(remainSec),
                        color = TextPrimary,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Text(
                        text = "剩余 " + Geo.formatDistance(remainKm) + "  ·  " + (progress * 100f).toInt() + "%",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Column(
                    Modifier.align(Alignment.CenterStart).padding(start = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    RoundAction(
                        label = if (paused) "继续" else "暂停",
                        sub = "计时",
                        active = paused,
                        onClick = { paused = !paused }
                    )
                    RoundAction(
                        label = if (muted) "有声" else "静音",
                        sub = "环境",
                        active = muted,
                        onClick = { muted = !muted }
                    )
                }
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RoundAction(label = "地图", sub = "航图", active = false, onClick = { mapOpen = true })
                    RoundAction(
                        label = source.label,
                        sub = "底图",
                        active = false,
                        onClick = {
                            source = if (source === TileSource.SATELLITE) TileSource.DARK else TileSource.SATELLITE
                        }
                    )
                    RoundAction(
                        label = if (focusMode) "已锁" else "免扰",
                        sub = "专注",
                        active = focusMode,
                        onClick = { focusMode = !focusMode }
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(Color(0x99101419), RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0xFF1B212B), RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("本趟航程", color = TextDim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(Geo.formatDistance(km) + " · " + Geo.formatDuration(minutes), color = Gold, fontSize = 10.sp)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x22FFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Gold)
                    )
                }
                Text(
                    text = if (paused) "已暂停，时间停在这里" else "已在空中 · 放下手机，做一件事",
                    color = TextDim.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = source.attribution,
                    color = TextDim.copy(alpha = 0.45f),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (mapOpen) {
            MapScreen(
                from = from,
                to = to,
                progress = progress,
                remainingSeconds = remainSec,
                remainingKm = remainKm,
                onClose = { mapOpen = false }
            )
        }
    }
}
