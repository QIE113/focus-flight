package com.zhuanshi.feixing.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.globe.GlobeCanvas
import com.zhuanshi.feixing.ui.theme.BarBg
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import java.util.Calendar

private data class TabSpec(val title: String, val sub: String)

@Composable
fun HomeScreen(
    from: City,
    to: City,
    records: List<FlightRecord>,
    tab: Int,
    onTab: (Int) -> Unit,
    onChangeRoute: () -> Unit,
    onPickDestination: (City) -> Unit,
    onStart: () -> Unit
) {
    val hour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    val greeting = remember(hour) { Geo.greeting(hour) }
    val distance = remember(from.code, to.code) { Geo.distanceKm(from, to) }
    val minutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }

    val tabs = listOf(
        TabSpec("我的", "记录"),
        TabSpec("趋势", lastSevenLabel(records)),
        TabSpec("世界", "航线")
    )

    Box(Modifier.fillMaxSize().background(NightBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp)) {
                Text(greeting, color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        from.name,
                        color = TextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "更换",
                        color = Gold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(bottom = 7.dp)
                            .clickable { onChangeRoute() }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(from.code + " → " + to.code, color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(Geo.formatDuration(minutes), color = TextPrimary.copy(alpha = 0.78f), fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(Geo.formatDistance(distance), color = TextPrimary.copy(alpha = 0.78f), fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "更改",
                        color = Gold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onChangeRoute() }
                    )
                }
            }

            GlobeCanvas(
                from = from,
                to = to,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(TextPrimary)
                        .clickable { onStart() }
                        .padding(horizontal = 38.dp, vertical = 15.dp)
                ) {
                    Text("开始旅程", color = NightBg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击后先选「从哪到哪」，时长按真实航程自动生成",
                    color = TextDim,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BarBg)
                    .padding(vertical = 4.dp)
            ) {
                tabs.forEachIndexed { idx, spec ->
                    val active = tab == idx
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onTab(if (tab == idx) -1 else idx) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            spec.title,
                            color = if (active) Gold else TextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                        Text(
                            spec.sub,
                            color = TextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 15.dp, top = 3.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = tab >= 0,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp, max = 520.dp)
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(PanelBg)
                        .clickable(enabled = false) {}
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 26.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "收起",
                            color = TextDim,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onTab(-1) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        when (tab) {
                            0 -> RecordScreen(records)
                            1 -> TrendScreen(records)
                            else -> WorldScreen(from, onPickDestination)
                        }
                    }
                }
            }
        }
    }
}

private fun lastSevenLabel(records: List<FlightRecord>): String {
    if (records.isEmpty()) return "还没有"
    val n = if (records.size >= 7) 7 else records.size
    return "最近 " + n + " 次"
}
