package com.zhuanshi.feixing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.FlightStore
import com.zhuanshi.feixing.ui.screens.FlightSessionScreen
import com.zhuanshi.feixing.ui.screens.HomeScreen
import com.zhuanshi.feixing.ui.screens.RoutePickerSheet
import com.zhuanshi.feixing.ui.theme.NightBg

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
    var pickerStartsFlight by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf(emptyList<FlightRecord>()) }

    LaunchedEffect(Unit) {
        records = store.records()
        val savedTo = store.destinationCode()
        if (savedTo.isNotEmpty()) {
            to = Cities.byCode(savedTo)
        }
        val savedFrom = store.originCode()
        if (savedFrom.isNotEmpty()) {
            from = Cities.byCode(savedFrom)
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
                onChangeRoute = {
                    pickerStartsFlight = false
                    picker = true
                },
                onPickDestination = { city ->
                    to = city
                    store.saveDestination(city.code)
                    tab = -1
                },
                onStart = {
                    pickerStartsFlight = true
                    picker = true
                }
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
            modifier = Modifier.fillMaxSize()
        ) {
            RoutePickerSheet(
                initialFrom = from,
                initialTo = to,
                onDismiss = {
                    picker = false
                    pickerStartsFlight = false
                },
                onConfirm = { start, end ->
                    from = start
                    to = end
                    store.saveOrigin(start.code)
                    store.saveDestination(end.code)
                    picker = false
                    if (pickerStartsFlight) {
                        pickerStartsFlight = false
                        stage = Stage.SESSION
                    }
                }
            )
        }
    }
}
