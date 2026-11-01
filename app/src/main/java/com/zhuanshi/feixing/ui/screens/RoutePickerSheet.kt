package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.GoldButton
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun RoutePickerSheet(
    initialFrom: City,
    initialTo: City,
    onDismiss: () -> Unit,
    onConfirm: (City, City) -> Unit
) {
    var from by remember { mutableStateOf(initialFrom) }
    var to by remember { mutableStateOf(initialTo) }
    var step by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NightBg.copy(alpha = 0.98f))
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (step == 0) "从哪里出发" else "飞去哪里",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (step == 0) "先选一个出发机场" else from.code + " " + from.name + " 出发",
                    color = TextDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            val closeSrc = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .pressScale(closeSrc, 0.86f)
                    .border(1.dp, Color(0xFF262D39), RoundedCornerShape(30.dp))
                    .clickable(interactionSource = closeSrc, indication = null, onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) { Text("关闭", color = TextPrimary.copy(alpha = 0.8f), fontSize = 12.sp) }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill(text = from.code + "  " + from.name, active = step == 0) { step = 0 }
            Text("→", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp));
            Pill(text = to.code + "  " + to.name, active = step == 1) { step = 1 }
        }

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(Cities.all) { city ->
                CityPickRow(
                    city = city,
                    active = if (step == 0) city.code == from.code else city.code == to.code,
                    onClick = {
                        if (step == 0) {
                            from = city
                            step = 1
                        } else {
                            if (city.code != from.code) to = city
                        }
                    }
                )
            }
        }

        Appear(delayMillis = 80) {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Geo.formatDuration(Geo.durationMinutes(from, to)) + "  ·  " + Geo.formatDistance(Geo.distanceKm(from, to)),
                    color = TextDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                GoldButton(text = "下一步 · 选座位", onClick = { onConfirm(from, to) })
            }
        }
    }
}

@Composable
private fun Pill(text: String, active: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(src, 0.94f)
            .background(if (active) Gold.copy(alpha = 0.18f) else Color(0x14FFFFFF), RoundedCornerShape(30.dp))
            .border(1.dp, if (active) Gold.copy(alpha = 0.65f) else Color(0xFF232A36), RoundedCornerShape(30.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (active) Gold else TextPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun CityPickRow(city: City, active: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(src, 0.985f)
            .background(if (active) Gold.copy(alpha = 0.1f) else Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = city.code,
            color = if (active) Gold else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 14.dp)
        )
        Text(city.name, color = TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (active) Text("已选", color = Gold, fontSize = 11.sp)
    }
}
