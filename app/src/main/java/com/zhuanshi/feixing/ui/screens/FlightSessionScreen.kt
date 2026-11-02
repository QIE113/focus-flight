package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.GoldButton
import com.zhuanshi.feixing.ui.HairLine
import com.zhuanshi.feixing.ui.KeyValueRow
import com.zhuanshi.feixing.ui.RoundAction
import com.zhuanshi.feixing.ui.TopBar
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.tapScale
import com.zhuanshi.feixing.ui.globe.Globe
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
private val COLUMNS = listOf("A", "C", "D", "F")

@Composable
fun SeatScreen(
    from: City,
    to: City,
    selected: String,
    onBack: () -> Unit,
    onNext: (String) -> Unit
) {
    var pick by remember { mutableStateOf(selected) }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "选个座位", sub = from.code + " → " + to.code + " · " + Geo.formatDuration(Geo.durationMinutes(from, to)), onBack = onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            COLUMNS.forEach { c ->
                Text(c, color = TextDim, fontSize = 11.sp, modifier = Modifier.size(46.dp), textAlign = TextAlign.Center)
            }
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically)
        ) {
            var row = 1
            while (row <= 10) {
                SeatRow(
                    row = row,
                    pick = pick,
                    onPick = { pick = it }
                )
                row++
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (pick.isEmpty()) "选一个靠窗还是过道？" else "已选 " + pick + " 号座",
                color = if (pick.isEmpty()) TextDim else Gold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            GoldButton(text = "下一步 · 选场景", enabled = pick.isNotEmpty(), onClick = { if (pick.isNotEmpty()) onNext(pick) })
        }
    }
}

@Composable
private fun SeatRow(row: Int, pick: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeatCell(code = COLUMNS[0] + rowName(row), active = pick == COLUMNS[0] + rowName(row), onClick = onPick);
        SeatCell(code = COLUMNS[1] + rowName(row), active = pick == COLUMNS[1] + rowName(row), onClick = onPick);
        Text(rowName(row), color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp)
        SeatCell(code = COLUMNS[2] + rowName(row), active = pick == COLUMNS[2] + rowName(row), onClick = onPick);
        SeatCell(code = COLUMNS[3] + rowName(row), active = pick == COLUMNS[3] + rowName(row), onClick = onPick);
    }
}

private fun rowName(row: Int): String = if (row < 10) "0" + row else "" + row

@Composable
private fun SeatCell(code: String, active: Boolean, onClick: (String) -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(width = 46.dp, height = 38.dp)
            .pressScale(src, 0.86f)
            .background(if (active) Gold else Color(0x0DFFFFFF), RoundedCornerShape(11.dp))
            .border(1.dp, if (active) Gold else Color(0xFF242B37), RoundedCornerShape(11.dp))
            .clickable(interactionSource = src, indication = null) { onClick(code) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = if (active) Color(0xFF05070C) else TextPrimary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private data class SceneItem(val name: String, val glyph: String, val desc: String)

private val SCENES = listOf(
    SceneItem("专注", "◎", "只做一件事，直到落地"),
    SceneItem("阅读", "▤", "翻几页一直想读的书"),
    SceneItem("冥想", "◍", "闭眼，听引擎的白噪音"),
    SceneItem("工作", "▦", "把起飞前的清单清空"),
    SceneItem("锻炼", "△", "拉伸、呼吸、放松肩颈")
)

@Composable
fun SceneScreen(
    from: City,
    to: City,
    selected: String,
    onBack: () -> Unit,
    onNext: (String) -> Unit
) {
    var pick by remember { mutableStateOf(if (selected.isEmpty()) "专注" else selected) }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "这趟飞行做什么", sub = from.code + " → " + to.code, onBack = onBack)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Globe(
                modifier = Modifier.fillMaxSize(),
                from = from,
                to = to,
                spin = true,
                sphereFill = 0.82f
            );
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SCENES.forEachIndexed { index, item ->
                Appear(delayMillis = 60 * index) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (pick == item.name) Gold.copy(alpha = 0.12f) else Color(0x0AFFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (pick == item.name) Gold.copy(alpha = 0.6f) else Color(0xFF1E242E),
                                RoundedCornerShape(16.dp)
                            )
                            .tapScale(0.985f) { pick = item.name }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.glyph, color = if (pick == item.name) Gold else TextDim, fontSize = 16.sp)
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(item.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(item.desc, color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (pick == item.name) Text("已选", color = Gold, fontSize = 10.sp)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
            GoldButton(text = "下一步 · 登机牌", onClick = { onNext(pick) })
        }
    }
}

@Composable
fun BoardingPassScreen(
    from: City,
    to: City,
    seat: String,
    scene: String,
    minutes: Int,
    km: Double,
    onBack: () -> Unit,
    onBoarded: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val gap = remember { Animatable(0f) }
    val fall = remember { Animatable(0f) }
    var torn by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(true) }
    val maxGap = with(density) { 210.dp.toPx() }
    val threshold = maxGap * 0.3f

    Box(Modifier.fillMaxSize()) {
        Globe(
            modifier = Modifier.fillMaxSize(),
            from = from,
            to = to,
            spin = true,
            showRoute = true,
            sphereFill = 0.98f
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xE605070C), Color(0x6605070C), Color(0xEE05070C))
                )
            )
        )
        TopBar(
            title = "登机牌",
            sub = from.code + " → " + to.code + " · " + scene + " · " + seat,
            onBack = if (fall.value > 0.1f) ({}) else onBack
        );

        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(end = 94.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xF40A0E15))
                        .border(1.dp, Color(0xFF232A36), RoundedCornerShape(26.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BOARDING PASS",
                            color = TextDim,
                            fontSize = 9.sp,
                            letterSpacing = 2.5.sp
                        )
                        Text("专注飞行", color = Gold.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = from.code,
                            color = TextPrimary,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "  ✈  ",
                            color = Gold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = to.code,
                            color = TextPrimary,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp
                        )
                    }
                    Text(
                        text = from.name + " → " + to.name,
                        color = TextDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Geo.durationHoursText(from, to),
                            color = Gold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = " 小时",
                            color = TextDim,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HairLine()
                    Spacer(Modifier.height(10.dp))
                    KeyValueRow("座位", seat.ifEmpty { "--" })
                    KeyValueRow("场景", scene)
                    KeyValueRow("距离", Geo.formatDistance(km))
                    KeyValueRow("登机口", "FOCUS-" + to.code)
                    Spacer(Modifier.weight(1f))
                    Barcode(Modifier.fillMaxWidth().height(30.dp).padding(end = 6.dp))
                }

                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(94.dp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = gap.value
                            translationY = gap.value * 0.10f + fall.value * 900f
                            rotationZ = gap.value * 0.05f + fall.value * 36f
                            alpha = 1f - fall.value * 0.92f
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    hint = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val next = (gap.value + drag.x).coerceIn(0f, maxGap)
                                    scope.launch { gap.snapTo(next) }
                                    if (next > threshold && !torn) {
                                        torn = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDragEnd = {
                                    if (gap.value > threshold) {
                                        scope.launch {
                                            gap.animateTo(maxGap * 1.3f, tween(200));
                                            fall.animateTo(1f, tween(640, easing = FastOutSlowInEasing));
                                            onBoarded()
                                        }
                                    } else {
                                        torn = false
                                        scope.launch { gap.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f)) }
                                    }
                                },
                                onDragCancel = {
                                    torn = false
                                    scope.launch { gap.animateTo(0f, tween(220)) }
                                }
                            )
                        }
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF121924))
                        .border(1.dp, Color(0xFF2A3240), RoundedCornerShape(26.dp))
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "撕下票根",
                            color = TextDim.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.rotate(90f)
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = seat.ifEmpty { "--" },
                            color = Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }

            Appear(delayMillis = 200) {
                Text(
                    text = if (hint) "按住右边票根，往右划开" else "松手就会撕开",
                    color = if (torn) Gold else TextDim,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 22.dp)
                );
            }
        }
    }
}

@Composable
private fun Barcode(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        var i = 0
        while (i < 42) {
            val w = if (i % 4 == 0) 2.2.dp else 1.dp
            val hf = 0.45f + ((i * 41) % 55) / 100f
            Box(
                Modifier
                    .width(w)
                    .fillMaxHeight(hf)
                    .background(Color(0xCCFFFFFF))
            )
            Spacer(Modifier.width(2.dp))
            i++
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
    val zoom = remember { Animatable(0f) }

    LaunchedEffect(Unit) { zoom.animateTo(1f, tween(1700, easing = FastOutSlowInEasing)) }
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

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val z = 1f + zoom.value * 3.8f
                scaleX = z
                scaleY = z
                translationX = -zoom.value * 90f
                translationY = -zoom.value * 40f
            }
        ) {
            Globe(
                modifier = Modifier.fillMaxSize(),
                from = from,
                to = to,
                progress = progress,
                spin = false,
                showRoute = true,
                sphereFill = 0.92f
            );
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xF205070C), Color(0x9905070C), Color(0xF705070C))
                )
            )
        )

        val hud = ((zoom.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
        Column(
            Modifier.fillMaxSize().graphicsLayer { alpha = hud }
        ) {
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
                    Text(
                        text = "剩余",
                        color = TextDim,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp
                    )
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
                    RoundAction(label = "归位", sub = "重置", active = false, onClick = {})
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
                    Text(Geo.formatDuration(minutes), color = Gold, fontSize = 10.sp)
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
            }
            Spacer(Modifier.height(24.dp));
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

@Composable
fun DoneScreen(from: City, to: City, minutes: Int, km: Double, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Appear {
            Text("已落地", color = Gold, fontSize = 12.sp, letterSpacing = 6.sp);
        }
        Appear(delayMillis = 120) {
            Text(
                text = to.name,
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 12.dp)
            );
        }
        Appear(delayMillis = 220) {
            Text(
                text = from.code + " → " + to.code + " · " + Geo.formatDuration(minutes) + " · " + Geo.formatDistance(km),
                color = TextDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 14.dp)
            );
        }
        Appear(delayMillis = 320) {
            Box(Modifier.padding(top = 34.dp)) {
                GoldButton(text = "再来一次", onClick = onAgain);
            }
        }
    }
}
