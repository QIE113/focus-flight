package com.zhuanshi.feixing.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.screens.BoardingPassScreen
import com.zhuanshi.feixing.ui.screens.CabinScreen
import com.zhuanshi.feixing.ui.screens.DoneScreen
import com.zhuanshi.feixing.ui.screens.FlightScreen
import com.zhuanshi.feixing.ui.screens.HomeScreen
import com.zhuanshi.feixing.ui.screens.RecordScreen
import com.zhuanshi.feixing.ui.screens.RoutePickerSheet
import com.zhuanshi.feixing.ui.screens.SceneScreen
import com.zhuanshi.feixing.ui.screens.SeatScreen
import com.zhuanshi.feixing.ui.screens.TrendScreen
import com.zhuanshi.feixing.ui.screens.WorldScreen
import com.zhuanshi.feixing.ui.theme.NightBg

enum class Stage { Home, Seat, Scene, Ticket, Cabin, Fly, Done, Records, Trend, World }

@Composable
fun FlightApp() {
    val context = LocalContext.current
    val store = remember { FlightStore(context) }
    var records by remember { mutableStateOf(store.records()) }
    var stage by remember { mutableStateOf(Stage.Home) }
    var from by remember { mutableStateOf(Cities.BEIJING) }
    var to by remember { mutableStateOf(Cities.SHANGHAI) }
    var seat by remember { mutableStateOf("") }
    var scene by remember { mutableStateOf("专注") }
    var picker by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val minutes = remember(from, to) { Geo.durationMinutes(from, to) }
    val km = remember(from, to) { Geo.distanceKm(from, to) }

    LaunchedEffect(stage) {
        if (stage == Stage.Done && !saved) {
            saved = true
            store.add(
                FlightRecord(
                    fromCode = from.code,
                    fromName = from.name,
                    toCode = to.code,
                    toName = to.name,
                    minutes = minutes,
                    finishedAt = System.currentTimeMillis()
                )
            )
            records = store.records()
        }
    }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        StarField(Modifier.fillMaxSize(), 130)
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(340)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "stage"
        ) { st ->
            when (st) {
                Stage.Home -> HomeScreen(
                    from = from,
                    to = to,
                    minutes = minutes,
                    km = km,
                    onPickRoute = { picker = true },
                    onRecords = { stage = Stage.Records },
                    onTrend = { stage = Stage.Trend },
                    onWorld = { stage = Stage.World }
                )
                Stage.Seat -> SeatScreen(
                    from = from,
                    to = to,
                    selected = seat,
                    onBack = { stage = Stage.Home },
                    onNext = { seat = it; stage = Stage.Scene }
                )
                Stage.Scene -> SceneScreen(
                    from = from,
                    to = to,
                    selected = scene,
                    onBack = { stage = Stage.Seat },
                    onNext = { scene = it; stage = Stage.Ticket }
                )
                Stage.Ticket -> BoardingPassScreen(
                    from = from,
                    to = to,
                    seat = seat,
                    scene = scene,
                    minutes = minutes,
                    km = km,
                    onBack = { stage = Stage.Scene },
                    onBoarded = { stage = Stage.Cabin }
                )
                Stage.Cabin -> CabinScreen(
                    from = from,
                    to = to,
                    onGo = { stage = Stage.Fly }
                )
                Stage.Fly -> FlightScreen(
                    from = from,
                    to = to,
                    seat = seat,
                    scene = scene,
                    minutes = minutes,
                    km = km,
                    onArrived = { stage = Stage.Done }
                )
                Stage.Done -> DoneScreen(
                    from = from,
                    to = to,
                    minutes = minutes,
                    km = km,
                    onAgain = {
                        saved = false
                        seat = ""
                        stage = Stage.Home
                    }
                )
                Stage.Records -> RecordScreen(
                    records = records,
                    onBack = { stage = Stage.Home }
                )
                Stage.Trend -> TrendScreen(
                    records = records,
                    onBack = { stage = Stage.Home }
                )
                Stage.World -> WorldScreen(
                    from = from,
                    onPick = { c ->
                        to = c
                        picker = false
                        stage = Stage.Seat
                    },
                    onBack = { stage = Stage.Home }
                )
            }
        }
        if (picker) {
            RoutePickerSheet(
                initialFrom = from,
                initialTo = to,
                onDismiss = { picker = false },
                onConfirm = { f, t ->
                    from = f
                    to = t
                    picker = false
                    stage = Stage.Seat
                }
            )
        }
    }
}
