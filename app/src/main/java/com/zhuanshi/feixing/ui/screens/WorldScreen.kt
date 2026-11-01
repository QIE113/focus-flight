package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.TopBar
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun WorldScreen(from: City, onPick: (City) -> Unit, onBack: () -> Unit) {
    val others = remember(from) { Cities.all.filter { it.code != from.code } }
    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "去哪里",
            sub = "从 " + from.code + " 出发 · 共 " + others.size + " 个目的地",
            onBack = onBack
        )
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(others) { city ->
                val src = remember(city.code) { MutableInteractionSource() }
                Appear(delayMillis = 30) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressScale(src, 0.985f)
                            .background(Color(0x66101419), RoundedCornerShape(16.dp))
                            .clickable(interactionSource = src, indication = null) { onPick(city) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city.code,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(end = 14.dp)
                        );
                        Text(city.name, color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.weight(1f));
                        Text(
                            text = Geo.formatDistance(Geo.distanceKm(from, city)),
                            color = Gold.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        );
                    }
                }
            }
        }
    }
}
