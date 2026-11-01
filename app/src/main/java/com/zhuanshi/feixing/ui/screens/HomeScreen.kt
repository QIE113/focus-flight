package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.GoldButton
import com.zhuanshi.feixing.ui.GhostButton
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.globe.Globe
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    from: City,
    to: City,
    minutes: Int,
    km: Double,
    onPickRoute: () -> Unit,
    onRecords: () -> Unit,
    onTrend: () -> Unit,
    onWorld: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "专注飞行",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "把这段时间交给天空",
                    color = TextDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Text("· " + Geo.greeting(9), color = Gold.copy(alpha = 0.8f), fontSize = 11.sp)
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Globe(
                modifier = Modifier.fillMaxSize(),
                from = from,
                to = to,
                progress = 0f,
                spin = true,
                sphereFill = 0.9f
            )
            Text(
                text = "拖动不了，但它一直在转",
                color = TextDim.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            )
        }

        Appear(delayMillis = 120) {
            RouteCard(from = from, to = to, minutes = minutes, km = km)
        }

        Appear(delayMillis = 220) {
            Box(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                GoldButton(text = "选择目的地 · 出发", onClick = onPickRoute)
            }
        }

        Appear(delayMillis = 300) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) { GhostButton("世界", Modifier.fillMaxWidth(), onWorld) }
                Box(Modifier.weight(1f)) { GhostButton("记录", Modifier.fillMaxWidth(), onRecords) }
                Box(Modifier.weight(1f)) { GhostButton("趋势", Modifier.fillMaxWidth(), onTrend) }
            }
        }
    }
}

@Composable
private fun RouteCard(from: City, to: City, minutes: Int, km: Double) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .background(Color(0x99101419), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF1E242E), RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(from.code, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                Text(from.name, color = TextDim, fontSize = 10.sp)
            }
            Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("✈", color = Gold, fontSize = 15.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(to.code, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                Text(to.name, color = TextDim, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Geo.formatDuration(minutes), color = Gold, fontSize = 12.sp)
            Text(Geo.formatDistance(km), color = TextDim, fontSize = 12.sp)
        }
    }
}
