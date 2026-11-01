package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    if (!boarded) {
        TicketStage(
            from = from,
            to = to,
            distance = distance,
            minutes = totalMinutes,
            onBoarded = { boarded = true }
        )
    } else {
        val progress = if (totalSeconds == 0) 0f else {
            ((totalSeconds - remain).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }
        Box(Modifier.fillMaxSize().background(NightBg)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text("飞行中", color = Gold, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    from.name + "  →  " + to.name,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    from.code + " · " + to.code + "　" + Geo.formatDistance(distance),
                    color = TextDim,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "真实航程 " + Geo.formatDuration(totalMinutes) + "（" + Geo.durationHoursText(from, to) + " 小时）",
                    color = TextDim,
                    fontSize = 11.sp
                )

                Spacer(Modifier.weight(1f))

                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(248.dp)) {
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
                                listOf(
                                    Gold.copy(alpha = 0.35f),
                                    Gold,
                                    Color(0xFFFFF3CE)
                                )
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
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "剩余 " + Geo.formatDuration(if (remain <= 0) 0 else (remain + 59) / 60),
                            color = TextDim,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xFF12161F))
                            .clickable { running = !running }
                            .padding(horizontal = 26.dp, vertical = 13.dp)
                    ) {
                        Text(if (running) "暂停" else "继续", color = TextPrimary, fontSize = 14.sp)
                    }

                    Spacer(Modifier.width(14.dp))

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(TextPrimary)
                            .clickable {
                                val elapsedMin = (totalSeconds - remain) / 60
                                finishWith(if (elapsedMin >= 1) elapsedMin else 0)
                            }
                            .padding(horizontal = 26.dp, vertical = 13.dp)
                    ) {
                        Text("结束旅程", color = NightBg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("完成整段航程后自动记入「记录」", color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

@Composable
private fun TicketStage(
    from: City,
    to: City,
    distance: Double,
    minutes: Int,
    onBoarded: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(NightBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Text("BOARDING PASS", color = Gold, fontSize = 10.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "撕票登机",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "把右侧票根往右撕开，旅程才会开始",
                color = TextDim,
                fontSize = 12.sp
            )

            Spacer(Modifier.weight(1f))

            BoardingPass(
                from = from,
                to = to,
                distance = distance,
                minutes = minutes,
                onBoarded = onBoarded
            )

            Spacer(Modifier.height(18.dp))
            Text(
                Geo.formatDistance(distance) + "　·　真实航程 " + Geo.formatDuration(minutes)
                    + "（" + Geo.durationHoursText(from, to) + " 小时）",
                color = TextDim,
                fontSize = 11.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                "不想撕票？直接放飞 →",
                color = Gold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onBoarded() }
            )
            Spacer(Modifier.height(26.dp))
        }
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
    val scope = rememberCoroutineScope()
    val tear = remember { Animatable(0f) }
    val density = LocalDensity.current
    val travelPx = with(density) { 150.dp.toPx() }
    val stubWidth = 92.dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(214.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        scope.launch {
                            if (tear.value > 0.42f) {
                                tear.animateTo(1f, tween(220))
                                onBoarded()
                            } else {
                                tear.animateTo(0f, tween(260))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { tear.animateTo(0f, tween(220)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            tear.snapTo((tear.value + dragAmount.x / travelPx).coerceIn(0f, 1f))
                        }
                    }
                )
            }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(stubWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = tear.value * travelPx
                    rotationZ = tear.value * 18f
                    alpha = 1f - tear.value * 0.2f
                }
                .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                .background(PanelBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("座位", color = TextDim, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                Text("08A", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("撕 →", color = TextDim, fontSize = 9.sp)
            }
        }

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(end = stubWidth)
                .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                .background(PanelBg)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BOARDING PASS", color = TextDim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text("专注飞行", color = Gold, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }

                Spacer(Modifier.height(16.dp))

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
                Text(
                    from.name + " 出发　·　" + to.name + " 到达",
                    color = TextDim,
                    fontSize = 11.sp
                )

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

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TicketCell("距离", Geo.formatDistance(distance))
                    TicketCell("真实航程", Geo.formatDuration(minutes))
                    TicketCell("登机口", "A12")
                }
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - tear.value }
        ) {
            val x = size.width - stubWidth.toPx()
            drawLine(
                color = Color(0xFF394052),
                start = Offset(x, 12f),
                end = Offset(x, size.height - 12f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f), 0f)
            )
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
