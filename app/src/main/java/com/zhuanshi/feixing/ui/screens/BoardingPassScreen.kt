package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.HairLine
import com.zhuanshi.feixing.ui.TopBar
import com.zhuanshi.feixing.ui.globe.Globe
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.launch

private val CARD_TOP = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val STUB_BOTTOM = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 26.dp, bottomEnd = 26.dp)

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
    val fullDrag = with(density) { 150.dp.toPx() }
    val threshold = 0.30f

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
                    listOf(Color(0xEE05070C), Color(0x8805070C), Color(0xF205070C))
                )
            )
        )
        TopBar(
            title = "登机牌",
            sub = from.code + " → " + to.code + " · " + scene + " · " + seat,
            onBack = if (fall.value > 0.1f) ({}) else onBack
        )

        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(CARD_TOP)
                    .background(Color(0xF40A0E15))
                    .border(1.dp, Color(0xFF232A36), CARD_TOP)
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BOARDING PASS", color = TextDim, fontSize = 9.sp, letterSpacing = 2.5.sp)
                    Text("专注飞行", color = Gold.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = from.code,
                        color = TextPrimary,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                    Text("  ✈  ", color = Gold, fontSize = 15.sp)
                    Text(
                        text = to.code,
                        color = TextPrimary,
                        fontSize = 42.sp,
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
                Spacer(Modifier.height(16.dp))
                HairLine()
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("座位", seat.ifEmpty { "--" })
                    MiniStat("时长", Geo.formatDuration(minutes))
                    MiniStat("距离", Geo.formatDistance(km))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("场景", scene)
                    MiniStat("登机口", "FOCUS-" + to.code)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .graphicsLayer {
                        translationX = -gap.value * fullDrag * 0.55f
                        translationY = gap.value * 14f + fall.value * 1100f
                        rotationZ = -gap.value * 9f - fall.value * 22f
                        alpha = 1f - fall.value
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                hint = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val next = (gap.value - drag.x / fullDrag).coerceIn(0f, 1f)
                                scope.launch { gap.snapTo(next) }
                                if (next > threshold && !torn) {
                                    torn = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                if (next <= threshold) torn = false
                            },
                            onDragEnd = {
                                if (gap.value > threshold) {
                                    scope.launch {
                                        gap.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                                        fall.animateTo(1f, tween(640, easing = FastOutSlowInEasing))
                                        onBoarded()
                                    }
                                } else {
                                    torn = false
                                    scope.launch { gap.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                                }
                            },
                            onDragCancel = {
                                torn = false
                                scope.launch { gap.animateTo(0f, tween(220, easing = FastOutSlowInEasing)) }
                            }
                        )
                    }
                    .clip(STUB_BOTTOM)
                    .background(Color(0xFF121924))
                    .border(1.dp, Color(0xFF2A3240), STUB_BOTTOM)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Barcode(Modifier.fillMaxWidth().height(34.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("撕下票根", color = TextDim.copy(alpha = 0.75f), fontSize = 9.sp, letterSpacing = 1.4.sp)
                        Text(seat.ifEmpty { "--" }, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Appear(delayMillis = 160) {
                Text(
                    text = if (hint) "按住底部票根，向左划开" else if (torn) "松手就撕开了" else "再往左一点",
                    color = if (torn) Gold else TextDim,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(key: String, value: String) {
    Column {
        Text(key, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
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
