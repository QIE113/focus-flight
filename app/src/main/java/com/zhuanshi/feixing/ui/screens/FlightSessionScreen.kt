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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.globe.GlobeMath
import com.zhuanshi.feixing.ui.globe.GlobeMap
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class Stage { SEAT, SCENE, TICKET, GATE, FLY }

private val SEAT_COLUMNS = listOf("A", "C", "D", "F")
private val SCENES = listOf("专注", "锻炼", "冥想", "阅读", "工作")

@Composable
fun FlightSessionScreen(
    from: City,
    to: City,
    onFinish: (FlightRecord) -> Unit
) {
    val totalMinutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }
    val totalSeconds = totalMinutes * 60
    val distance = remember(from.code, to.code) { Geo.distanceKm(from, to) }

    var stage by remember { mutableStateOf(Stage.SEAT) }
    var seat by remember { mutableStateOf("06F") }
    var scene by remember { mutableStateOf("专注") }
    var showMap by remember { mutableStateOf(false) }
    var remain by remember { mutableIntStateOf(totalSeconds) }
    var running by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
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

    LaunchedEffect(stage) {
        if (stage != Stage.FLY) return@LaunchedEffect
        while (remain > 0 && !finished) {
            if (running) {
                delay(1000)
                remain -= 1
            } else {
                delay(200)
            }
        }
        if (remain <= 0 && !finished) finishWith(totalMinutes)
    }

    val progress = if (totalSeconds == 0) 0f else {
        ((totalSeconds - remain).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        when (stage) {
            Stage.SEAT -> SeatStage(
                from = from,
                to = to,
                seat = seat,
                onSeat = { seat = it },
                onNext = { stage = Stage.SCENE }
            )
            Stage.SCENE -> SceneStage(
                seat = seat,
                scene = scene,
                onScene = { scene = it },
                onBack = { stage = Stage.SEAT },
                onNext = { stage = Stage.TICKET }
            )
            Stage.TICKET -> TicketStage(
                from = from,
                to = to,
                seat = seat,
                scene = scene,
                distance = distance,
                minutes = totalMinutes,
                onBoarded = { stage = Stage.GATE }
            )
            Stage.GATE -> GateStage(
                from = from,
                to = to,
                scene = scene,
                onDepart = { stage = Stage.FLY }
            )
            Stage.FLY -> FlyingStage(
                from = from,
                to = to,
                distance = distance,
                minutes = totalMinutes,
                remain = remain,
                progress = progress,
                running = running,
                muted = muted,
                onTogglePause = { running = !running },
                onToggleMute = { muted = !muted },
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
                progress = if (stage == Stage.FLY) progress else -1f,
                onClose = { showMap = false }
            )
        }
    }
}

@Composable
private fun SeatStage(
    from: City,
    to: City,
    seat: String,
    onSeat: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("选座", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                from.code + " → " + to.code,
                color = TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("已选 " + seat, color = Gold, fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("A", color = TextDim, fontSize = 11.sp)
            Text("C", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.width(28.dp))
            Text("D", color = TextDim, fontSize = 11.sp)
            Text("F", color = TextDim, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))

        for (row in 1..10) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in SEAT_COLUMNS.indices) {
                    val code = (if (row < 10) "0" else "") + row + SEAT_COLUMNS[i]
                    SeatCell(
                        code = code,
                        taken = (row * 7 + i * 3) % 5 == 0,
                        selected = seat == code,
                        onClick = { onSeat(code) }
                    )
                    if (i == 1) {
                        Text(
                            if (row < 10) "0" + row else row.toString(),
                            color = TextDim.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.width(28.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(TextPrimary)
                .clickable { onNext() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("确认", color = NightBg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SeatCell(code: String, taken: Boolean, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp, 38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    selected -> Color(0xFFE9EEF5)
                    taken -> Color(0xFF161A22)
                    else -> Color(0xFF1E2430)
                }
            )
            .clickable(enabled = !taken) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(code, color = NightBg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SceneStage(
    seat: String,
    scene: String,
    onScene: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            "座位 " + seat,
            color = TextDim,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "你想要在什么场景下专注？",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(22.dp))

        val rows = SCENES.chunked(3)
        for (group in rows) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (s in group) {
                    val active = scene == s
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color(0xFFE9EEF5) else Color(0xFF1B2029))
                            .clickable { onScene(s) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s,
                            color = if (active) NightBg else TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF1B2029))
                    .clickable { onBack() }
                    .padding(horizontal = 26.dp, vertical = 15.dp)
            ) {
                Text("返回", color = TextPrimary, fontSize = 14.sp)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(TextPrimary)
                    .clickable { onNext() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("确认", color = NightBg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TicketStage(
    from: City,
    to: City,
    seat: String,
    scene: String,
    distance: Double,
    minutes: Int,
    onBoarded: () -> Unit
) {
    val cam = remember(from.code, to.code) { GlobeMath.routeCamera(from, to) }

    Box(Modifier.fillMaxSize()) {
        GlobeMap(
            from = from,
            to = to,
            modifier = Modifier.fillMaxSize(),
            scale = 1.05f,
            rotLon = cam[0],
            rotLat = cam[1],
            interactive = true,
            stars = true
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(14.dp))
            Text("BOARDING PASS", color = Gold, fontSize = 10.sp, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))

            BoardingPass(
                from = from,
                to = to,
                seat = seat,
                scene = scene,
                distance = distance,
                minutes = minutes,
                onBoarded = onBoarded
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "按住右侧票根往右拖，划一点撕一点",
                color = TextDim,
                fontSize = 11.sp
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BoardingPass(
    from: City,
    to: City,
    seat: String,
    scene: String,
    distance: Double,
    minutes: Int,
    onBoarded: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val maxTear = with(density) { 150.dp.toPx() }
    val stubWidth = 76.dp

    var tear by remember { mutableFloatStateOf(0f) }
    var lastTick by remember { mutableIntStateOf(-1) }
    var boarded by remember { mutableStateOf(false) }

    val frac = (tear / maxTear).coerceIn(0f, 1f)

    fun tick() {
        val step = (frac * 12f).toInt()
        if (step != lastTick) {
            lastTick = step
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(206.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = stubWidth)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE6151A22))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            from.code,
                            color = Color(0xFFEDEFF2),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(from.name, color = TextDim, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(6.dp))
                        Text("✈", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            Geo.formatDuration(minutes),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            to.code,
                            color = Color(0xFFEDEFF2),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(to.name, color = TextDim, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PassCell("座位", seat)
                    PassCell("登机时间", "现在")
                    PassCell("场景", scene)
                    PassCell("距离", Geo.formatDistance(distance))
                }
                Spacer(Modifier.height(10.dp))
                Barcode(Modifier.fillMaxWidth().height(30.dp))
            }
        }

        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(stubWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = tear
                    rotationZ = frac * 9f
                }
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE6151A22))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change, drag ->
                            change.consume()
                            if (boarded) return@detectDragGestures
                            tear = (tear + drag.x).coerceIn(0f, maxTear)
                            tick()
                            if (tear / maxTear >= 0.75f) {
                                boarded = true
                                onBoarded()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Barcode(Modifier.width(20.dp).height(96.dp), vertical = true)
                Spacer(Modifier.height(10.dp))
                Text(seat, color = Gold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("撕 →", color = TextDim, fontSize = 10.sp)
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val x = size.width - stubWidth.toPx() - tear
            drawLine(
                color = Color(0xFF6B7484).copy(alpha = 0.55f),
                start = Offset(x, 12f),
                end = Offset(x, size.height - 12f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 9f), 0f)
            )
        }
    }
}

@Composable
private fun PassCell(label: String, value: String) {
    Column {
        Text(label, color = TextDim, fontSize = 9.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color(0xFFE7EBF1), fontSize = 12.sp)
    }
}

@Composable
private fun Barcode(modifier: Modifier = Modifier, vertical: Boolean = false) {
    Canvas(modifier) {
        val rnd = Random(20260304)
        if (vertical) {
            var y = 0f
            while (y < size.height) {
                val wBar = 1f + rnd.nextFloat() * 2.2f
                val gap = 1f + rnd.nextFloat() * 2f
                if (rnd.nextFloat() > 0.35f) {
                    drawRect(
                        color = Color(0xFFD7DCE4),
                        topLeft = Offset((size.width - wBar) / 2f, y),
                        size = androidx.compose.ui.geometry.Size(wBar, gap)
                    )
                }
                y += gap
            }
        } else {
            var x = 0f
            while (x < size.width) {
                val wBar = 1.2f + rnd.nextFloat() * 2.6f
                val gap = 1.2f + rnd.nextFloat() * 2.4f
                if (rnd.nextFloat() > 0.3f) {
                    drawRect(
                        color = Color(0xFFD7DCE4),
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(wBar, size.height)
                    )
                }
                x += wBar + gap
            }
        }
    }
}

@Composable
private fun GateStage(
    from: City,
    to: City,
    scene: String,
    onDepart: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NightBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("舱门关闭，", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("飞行即将开始", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(18.dp))
        Text(
            from.name + " → " + to.name + "　·　" + scene,
            color = TextDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(56.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(TextPrimary)
                .clickable { onDepart() }
                .padding(horizontal = 42.dp, vertical = 14.dp)
        ) {
            Text("出发！", color = NightBg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
    progress: Float,
    running: Boolean,
    muted: Boolean,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenMap: () -> Unit,
    onFinishEarly: () -> Unit
) {
    val cam = remember(from.code, to.code) { GlobeMath.routeCamera(from, to) }
    var zoom by remember(from.code, to.code) { mutableFloatStateOf(1.35f) }
    var lon by remember(from.code, to.code) { mutableFloatStateOf(cam[0]) }
    var lat by remember(from.code, to.code) { mutableFloatStateOf(cam[1]) }

    Box(Modifier.fillMaxSize().background(Color(0xFF04060B))) {
        GlobeMap(
            from = from,
            to = to,
            modifier = Modifier.fillMaxSize(),
            progress = progress,
            scale = zoom,
            rotLon = lon,
            rotLat = lat,
            interactive = true,
            stars = true,
            onCamera = { z, lo, la ->
                zoom = z
                lon = lo
                lat = la
            }
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("专注飞行", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                from.code + " → " + to.code,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                (progress * 100f).roundToInt().toString() + "%",
                color = TextDim,
                fontSize = 11.sp
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoundButton(if (running) "❚❚" else "▶") { onTogglePause() }
            RoundButton(if (muted) "🔇" else "🔊", active = !muted) { onToggleMute() }
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoundButton("✆") { onFinishEarly() }
            RoundButton("✈", active = true) {
                zoom = 1.35f
                lon = cam[0]
                lat = cam[1]
            }
            RoundButton("▦") { onOpenMap() }
            RoundButton("◷") {
                zoom = (zoom * 1.18f).coerceAtMost(6f)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("剩余飞行时间", color = TextDim, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Geo.formatDuration((remain + 59) / 60),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("剩余飞行距离", color = TextDim, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Geo.formatDistance(distance * (1.0 - progress)),
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (active) Color(0xFFE9EEF5) else Color(0xCC1A1F29))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) NightBg else TextPrimary,
            fontSize = 15.sp
        )
    }
}
