package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.globe.GlobeMap
import com.zhuanshi.feixing.ui.globe.GlobeMath
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun FlightSessionScreen(
    from: City,
    to: City,
    onFinish: (FlightRecord) -> Unit
) {
    val totalMinutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }
    val totalSeconds = totalMinutes * 60
    val distance = remember(from.code, to.code) { Geo.distanceKm(from, to) }

    var boarded by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var remain by remember { mutableIntStateOf(totalSeconds) }
    var running by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    fun finishWith(minutesSpent: Int) {
        if (finished) return
        finished = true
        onFinish(
            FlightRecord(
                fromCode = from.code,
                fromName = from.name,
                toCode = to.code,
                toName = to.name,
                minutes = minutesSpent,
                finishedAt = if (minutesSpent > 0) System.currentTimeMillis() else 0L
            )
        )
    }

    LaunchedEffect(boarded) {
        if (!boarded) return@LaunchedEffect
        while (remain > 0 && !finished) {
            if (running) {
                delay(1000)
                remain -= 1
            } else {
                delay(200)
            }
        }
        if (remain <= 0 && !finished) {
            finishWith(totalMinutes)
        }
    }

    val progress = if (totalSeconds == 0) 0f else {
        ((totalSeconds - remain).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        if (!boarded) {
            TicketStage(
                from = from,
                to = to,
                distance = distance,
                minutes = totalMinutes,
                onBoarded = { boarded = true },
                onOpenMap = { showMap = true }
            )
        } else {
            FlyingStage(
                from = from,
                to = to,
                distance = distance,
                minutes = totalMinutes,
                remain = remain,
                totalSeconds = totalSeconds,
                progress = progress,
                running = running,
                onTogglePause = { running = !running },
                onOpenMap = { showMap = true },
                onFinishEarly = {
                    val elapsed = (totalSeconds - remain) / 60
                    finishWith(if (elapsed >= 1) elapsed else 0)
                }
            )
        }

        if (showMap) {
            MapScreen(
                from = from,
                to = to,
                progress = if (boarded) progress else -1f,
                onClose = { showMap = false }
            )
        }
    }
}

@Composable
private fun FlyingStage(
    from: City,
    to: City,
    distance: Double,
    minutes: Int,
    remain: Int,
    totalSeconds: Int,
    progress: Float,
    running: Boolean,
    onTogglePause: () -> Unit,
    onOpenMap: () -> Unit,
    onFinishEarly: () -> Unit
) {
    val cam = remember(from.code, to.code) { GlobeMath.routeCamera(from, to) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))
        Text("飞行中", color = Gold, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            from.name + "  →  " + to.name,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            from.code + " · " + to.code + "　" + Geo.formatDuration(minutes)
                + "　" + Geo.formatDistance(distance),
            color = TextDim,
            fontSize = 11.sp
        )

        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(216.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Gold.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.52f
                    ),
                    radius = size.minDimension * 0.52f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )

                drawArc(
                    color = Color(0xFF1A1F29),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Gold.copy(alpha = 0.35f), Gold, Color(0xFFFFF3CE))
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    Geo.formatClock(remain),
                    color = TextPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "剩余 " + Geo.formatDuration(if (remain <= 0) 0 else (remain + 59) / 60),
                    color = TextDim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    (progress * 100f).roundToInt().toString() + "%",
                    color = Gold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF070B12))
                .clickable { onOpenMap() }
        ) {
            GlobeMap(
                from = from,
                to = to,
                modifier = Modifier.fillMaxSize(),
                progress = progress,
                scale = 1.75f,
                rotLon = cam[0],
                rotLat = cam[1],
                interactive = false
            )
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("点开地图 · 可自由放大", color = TextDim, fontSize = 10.sp)
                Text(
                    (progress * 100f).roundToInt().toString() + "% 航程",
                    color = Gold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF12161F))
                    .clickable { onTogglePause() }
                    .padding(horizontal = 24.dp, vertical = 13.dp)
            ) {
                Text(if (running) "暂停" else "继续", color = TextPrimary, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(TextPrimary)
                    .clickable { onFinishEarly() }
                    .padding(horizontal = 24.dp, vertical = 13.dp)
            ) {
                Text("结束旅程", color = NightBg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("完成整段航程后自动记入「记录」", color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TicketStage(
    from: City,
    to: City,
    distance: Double,
    minutes: Int,
    onBoarded: () -> Unit,
    onOpenMap: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("BOARDING PASS", color = Gold, fontSize = 10.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text("撕票登机", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "按住右侧票根往右拖，划一点撕一点，撕到 80% 就起飞",
            color = TextDim,
            fontSize = 11.sp
        )

        Spacer(Modifier.weight(1f))

        BoardingPass(
            from = from,
            to = to,
            distance = distance,
            minutes = minutes,
            onBoarded = onBoarded
        )

        Spacer(Modifier.height(14.dp))

        Text(
            Geo.formatDistance(distance) + "　·　" + Geo.formatDuration(minutes)
                + "（" + Geo.durationHoursText(from, to) + " 小时）",
            color = TextDim,
            fontSize = 11.sp
        )

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                "看地图",
                color = TextPrimary.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { onOpenMap() }
            )
            Text(
                "不想撕票，直接放飞 →",
                color = Gold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onBoarded() }
            )
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun BoardingPass(
    from: City,
    to: City,
    distance: Double,
    minutes: Int,
    onBoarded: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val maxTear = with(density) { 176.dp.toPx() }
    val stubWidth = 92.dp

    var tear by remember { mutableFloatStateOf(0f) }
    var lastTick by remember { mutableIntStateOf(-1) }
    var boarded by remember { mutableStateOf(false) }

    val frac = (tear / maxTear).coerceIn(0f, 1f)

    fun tickIfNeeded() {
        val step = (frac * 12f).toInt()
        if (step != lastTick) {
            lastTick = step
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(222.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = stubWidth)
                .clip(TornEdgeShape(TornSide.RIGHT, 2.4.dp, 7.dp, 7))
                .background(PanelBg)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BOARDING PASS", color = TextDim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text("专注飞行", color = Gold, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        from.code,
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("  →  ", color = Gold, fontSize = 16.sp)
                    Text(
                        to.code,
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(from.name + " 出发　·　" + to.name + " 到达", color = TextDim, fontSize = 11.sp)

                Spacer(Modifier.weight(1f))

                Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(
                        color = Color(0xFF2A3040),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TicketCell("距离", Geo.formatDistance(distance))
                    TicketCell("真实航程", Geo.formatDuration(minutes))
                    TicketCell("登机口", "A12")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (frac <= 0.01f) "← 往右拖票根" else "已撕开 " + (frac * 100).roundToInt() + "%",
                    color = if (frac <= 0.01f) TextDim else Gold,
                    fontSize = 10.sp
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(stubWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = tear
                    rotationZ = frac * 14f
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .clip(TornEdgeShape(TornSide.LEFT, 2.4.dp, 7.dp, 7))
                .background(PanelBg)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change, drag ->
                            change.consume()
                            if (boarded) return@detectDragGestures
                            tear = (tear + drag.x).coerceIn(0f, maxTear)
                            tickIfNeeded()
                            if (tear / maxTear >= 0.8f) {
                                boarded = true
                                onBoarded()
                            }
                        }
                    )
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("座位", color = TextDim, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                Text("08A", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text("登机口", color = TextDim, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                Text("A12", color = TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text("撕 →", color = Gold, fontSize = 10.sp)
            }
        }

        if (tear <= 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width - stubWidth.toPx()
                drawLine(
                    color = Gold.copy(alpha = 0.35f),
                    start = Offset(x, 14f),
                    end = Offset(x, size.height - 14f),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                )
            }
        }
    }
}

@Composable
private fun TicketCell(label: String, value: String) {
    Column {
        Text(label, color = TextDim, fontSize = 9.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = TextPrimary, fontSize = 12.sp)
    }
}

private enum class TornSide { LEFT, RIGHT }

/** 锯齿状撕口：左右两半用同一个随机种子，撕开后边缘能严丝合缝地对上 */
private class TornEdgeShape(
    private val side: TornSide,
    private val amplitude: Dp,
    private val tooth: Dp,
    private val seed: Int
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val amp = with(density) { amplitude.toPx() }
        val step = with(density) { tooth.toPx() }.coerceAtLeast(1f)
        val rnd = Random(seed)
        val offsets = ArrayList<Float>(64)
        var y = 0f
        while (y < size.height) {
            offsets.add((rnd.nextFloat() - 0.5f) * 2f * amp)
            y += step
        }

        val path = Path()
        var yy = 0f
        if (side == TornSide.RIGHT) {
            path.moveTo(0f, 0f)
            for (o in offsets) {
                path.lineTo(size.width + o, yy)
                yy += step
            }
            path.lineTo(size.width + offsets.lastOrNull().let { it ?: 0f }, size.height)
            path.lineTo(0f, size.height)
        } else {
            path.moveTo(size.width, 0f)
            for (o in offsets) {
                path.lineTo(o, yy)
                yy += step
            }
            path.lineTo(offsets.lastOrNull().let { it ?: 0f }, size.height)
            path.lineTo(size.width, size.height)
        }
        path.close()
        return Outline.Generic(path)
    }
}
