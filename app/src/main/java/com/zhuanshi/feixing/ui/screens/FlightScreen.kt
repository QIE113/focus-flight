package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.map.Camera
import com.zhuanshi.feixing.map.Projection
import com.zhuanshi.feixing.map.TileMap
import com.zhuanshi.feixing.map.TileSource
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.NightBg
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
    return doubleArrayOf(
        Math.toDegrees(asin(z.coerceIn(-1.0, 1.0))),
        Math.toDegrees(atan2(y, x))
    )
}

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * 视频 20.6s → 38.8s 的镜头，按毫秒复刻：
 *   0     – 1600    zoom 4.0  → 16.0   推到出发机场，看得见跑道和航站楼
 *   1600  – 2600    zoom 16.0          停在机场
 *   2600  – 18200   zoom 16.0 → 3.0    匀速拉远，一直拉到全球
 *   18200 +         zoom 3.0
 *
 * 终点为什么是 3 不是 2：实测高德 z2 返回纯白空瓦片，
 * 收到 z2 会让动画最后一帧整屏刷白。z3 有内容，是能用的最小级别。
 */
fun cinematicZoom(ms: Float): Float = when {
    ms <= 1600f -> 4.0f + 12.0f * smoothstep(ms / 1600f)
    ms <= 2600f -> 16.0f
    ms <= 18200f -> 16.0f - 13.0f * ((ms - 2600f) / 15600f).coerceIn(0f, 1f)
    else -> 3.0f
}

/** 舱门关闭 → 出发。背景是压暗的卫星地形，不是纯黑。 */
@Composable
fun CabinScreen(from: City, to: City, onGo: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        TileMap(
            camera = Camera(30.0, 100.0, 3.0f),
            source = TileSource.AMAP_SAT,
            modifier = Modifier.fillMaxSize()
        )
        // 压暗 + 星点 + 暗角
        Box(Modifier.fillMaxSize().background(Color(0xE604070C)))
        Canvas(Modifier.fillMaxSize()) {
            var seed = 20260830L
            fun rnd(): Float {
                seed = (seed * 6364136223846793005L + 1442695040888963407L)
                return ((seed ushr 33).toInt() and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
            }
            for (i in 0 until 150) {
                val x = rnd() * size.width
                val y = rnd() * size.height * 0.75f
                val r = 0.6f + rnd() * 1.1f
                drawCircle(Color(0x66FFFFFF), r, Offset(x, y))
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    0.15f to Color(0x00000000),
                    0.75f to Color(0xAA04070C),
                    1f to Color(0xF203050A)
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 起落架 + 短横线
            Appear(riseDp = 8f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏠", color = Color(0xFFF2F6FA), fontSize = 22.sp)
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(width = 22.dp, height = 1.5.dp)
                            .background(Color(0xFFF2F6FA))
                    )
                }
            }
            Spacer(Modifier.height(96.dp))
            Appear(delayMillis = 140, riseDp = 12f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("舱门关闭，", color = Color(0xFFF0F4F8), fontSize = 17.sp, letterSpacing = 1.sp)
                    Text(
                        "飞行即将开始",
                        color = Color(0xFFF0F4F8),
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(110.dp))
            Appear(delayMillis = 320, riseDp = 16f) {
                val src = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .pressScale(src, 0.94f)
                        .background(Color(0xFFF4F6F8), CircleShape)
                        .clickable(interactionSource = src, indication = null, onClick = onGo)
                        .padding(horizontal = 30.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "出发！",
                        color = Color(0xFF0A0E15),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/** 视频里的圆钮：半透明深底 + 细描边 + 单色字形。 */
@Composable
private fun HudButton(glyph: String, filled: Boolean = false, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(38.dp)
            .pressScale(src, 0.88f)
            .background(if (filled) Color(0xFFF4F6F8) else Color(0x59070C14), CircleShape)
            .border(1.dp, if (filled) Color(0x00FFFFFF) else Color(0x3DFFFFFF), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            glyph,
            color = if (filled) Color(0xFF0A0E15) else Color(0xFFEAF0F7),
            fontSize = 13.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
        )
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
    var source by remember { mutableStateOf(TileSource.AMAP_VEC) }

    // 镜头时钟：按视频跑完 18.2 秒运镜
    var cine by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (cine < 18200f) {
            withFrameMillis { now ->
                cine = (now - start).toFloat().coerceIn(0f, 18200f)
            }
        }
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

    val flightPoint = greatCirclePoint(from, to, progress.toDouble())
    // 前 1.6 秒钉在出发机场（推近），之后中心逐步交给航程点
    val blend = smoothstep((cine - 1600f) / 1000f).toDouble()
    val camLat = from.lat + (flightPoint[0] - from.lat) * blend
    val camLon = from.lon + (flightPoint[1] - from.lon) * blend
    val camera = Camera(camLat, camLon, cinematicZoom(cine))

    Box(Modifier.fillMaxSize().background(NightBg)) {
        TileMap(
            camera = camera,
            source = source,
            modifier = Modifier.fillMaxSize()
        ) { proj ->
            drawFlightRoute(proj, from, to, progress)
            drawEndpointPins(proj, from, to)
        }

        // 左侧三格
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 52.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudButton(if (paused) "▶" else "❚❚") { paused = !paused }
            HudButton("✈") { }
            HudButton("↺") { }
        }

        // 右侧三格：最上面是白色实心定位
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp, top = 52.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudButton("➤", filled = true) { }
            HudButton("▤") {
                source = if (source === TileSource.AMAP_VEC) TileSource.AMAP_SAT else TileSource.AMAP_VEC
            }
            HudButton("⌂") { }
        }

        // 底部两栏
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text("剩余飞行时间", color = Color(0xF2E8EEF6), fontSize = 11.sp)
                Text(
                    text = Geo.formatClock(remainSec),
                    color = Color(0xFFF6F9FC),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("剩余飞行距离", color = Color(0xF2E8EEF6), fontSize = 11.sp)
                Text(
                    text = Geo.formatDistance(remainKm),
                    color = Color(0xFFF6F9FC),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        // 署名：字号极小、常驻
        Text(
            text = source.attribution,
            color = Color(0x8CE8EEF6),
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 8.dp)
        )
        Text(
            text = seat + " · " + scene,
            color = Color(0x8CE8EEF6),
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 8.dp)
        )
    }
}

/** 起点/终点：黄色圆角方块 + 白色飞机剪影，视频里两端各一个。 */
private fun DrawScope.drawEndpointPins(proj: Projection, from: City, to: City) {
    for (c in listOf(from, to)) {
        val p = proj.toScreen(c.lat, c.lon)
        if (p.x < -60f || p.y < -60f || p.x > size.width + 60f || p.y > size.height + 60f) continue
        val w = 20f
        val h = 14f
        drawRoundRect(
            color = Color(0xFFF5CE3A),
            topLeft = Offset(p.x - w / 2f, p.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // 简化飞机：一条中轴 + 两片翼
        drawLine(Color(0xFF101008), Offset(p.x - 4f, p.y), Offset(p.x + 4f, p.y), 1.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFF101008), Offset(p.x - 1f, p.y - 3f), Offset(p.x + 2f, p.y), 1.3f, cap = StrokeCap.Round)
        drawLine(Color(0xFF101008), Offset(p.x - 1f, p.y + 3f), Offset(p.x + 2f, p.y), 1.3f, cap = StrokeCap.Round)
    }
}
