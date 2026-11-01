package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.globe.GlobeCanvas
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun FlightSessionScreen(
    from: City,
    to: City,
    onFinish: (FlightRecord) -> Unit
) {
    val totalMinutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }
    val totalSeconds = totalMinutes * 60

    var remain by remember { mutableIntStateOf(totalSeconds) }
    var running by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remain > 0 && !finished) {
            if (running) {
                delay(1000)
                remain -= 1
            } else {
                delay(200)
            }
        }
        if (remain <= 0 && !finished) {
            finished = true
            onFinish(
                FlightRecord(
                    fromCode = from.code,
                    fromName = from.name,
                    toCode = to.code,
                    toName = to.name,
                    minutes = totalMinutes,
                    finishedAt = System.currentTimeMillis()
                )
            )
        }
    }

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
            Spacer(Modifier.height(18.dp))

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
                from.code + " · " + to.code,
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
                    Text(
                        if (running) "暂停" else "继续",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.width(14.dp))

                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(TextPrimary)
                        .clickable {
                            if (!finished) {
                                finished = true
                                val elapsedMin = (totalSeconds - remain) / 60
                                if (elapsedMin >= 1) {
                                    onFinish(
                                        FlightRecord(
                                            fromCode = from.code,
                                            fromName = from.name,
                                            toCode = to.code,
                                            toName = to.name,
                                            minutes = elapsedMin,
                                            finishedAt = System.currentTimeMillis()
                                        )
                                    )
                                } else {
                                    onFinish(
                                        FlightRecord(
                                            fromCode = from.code,
                                            fromName = from.name,
                                            toCode = to.code,
                                            toName = to.name,
                                            minutes = 0,
                                            finishedAt = 0L
                                        )
                                    )
                                }
                            }
                        }
                        .padding(horizontal = 26.dp, vertical = 13.dp)
                ) {
                    Text("结束旅程", color = NightBg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "完成整段航程后自动记入「记录」",
                color = TextDim,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(26.dp))
        }
    }
}
