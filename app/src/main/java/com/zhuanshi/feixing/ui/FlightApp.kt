package com.zhuanshi.feixing.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.FlightStore
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.screens.FlightSessionScreen
import com.zhuanshi.feixing.ui.screens.HomeScreen
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

private enum class Stage { HOME, SESSION }

@Composable
fun FlightApp() {
    val context = LocalContext.current
    val store = remember { FlightStore(context) }

    var from by remember { mutableStateOf(Cities.BEIJING) }
    var to by remember { mutableStateOf(Cities.SHANGHAI) }
    var stage by remember { mutableStateOf(Stage.HOME) }
    var tab by remember { mutableStateOf(-1) }
    var picker by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf(emptyList<FlightRecord>()) }

    LaunchedEffect(Unit) {
        records = store.records()
        val saved = store.destinationCode()
        if (saved.isNotEmpty()) {
            to = Cities.byCode(saved)
        }
    }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        when (stage) {
            Stage.HOME -> HomeScreen(
                from = from,
                to = to,
                records = records,
                tab = tab,
                onTab = { tab = it },
                onChangeRoute = { picker = true },
                onPickDestination = { city ->
                    to = city
                    store.saveDestination(city.code)
                    tab = -1
                },
                onStart = { stage = Stage.SESSION }
            )

            Stage.SESSION -> FlightSessionScreen(
                from = from,
                to = to,
                onFinish = { rec ->
                    if (rec.minutes > 0) {
                        store.add(rec)
                        records = store.records()
                    }
                    stage = Stage.HOME
                }
            )
        }

        AnimatedVisibility(
            visible = picker,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RoutePicker(
                from = from,
                current = to,
                onClose = { picker = false },
                onPick = { city ->
                    to = city
                    store.saveDestination(city.code)
                    picker = false
                }
            )
        }
    }
}

@Composable
private fun RoutePicker(
    from: City,
    current: City,
    onClose: () -> Unit,
    onPick: (City) -> Unit
) {
    val options = remember(from.code) {
        Cities.all.filter { it.code != from.code }
            .sortedBy { Geo.distanceKm(from, it) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(PanelBg)
            .padding(start = 22.dp, end = 22.dp, top = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("选择目的地", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "关闭",
                color = TextDim,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("从 " + from.name + " 出发", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))

        LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
            items(options) { city ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (city.code == current.code) Gold.copy(alpha = 0.12f)
                            else TextPrimary.copy(alpha = 0.045f)
                        )
                        .clickable { onPick(city) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(city.name, color = TextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            city.code + "　" + Geo.formatDistance(Geo.distanceKm(from, city)),
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        Geo.formatDuration(Geo.durationMinutes(from, city)),
                        color = Gold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
