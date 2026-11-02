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
