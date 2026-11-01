package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun WorldScreen(from: City, onPick: (City) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("世界", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "从 " + from.name + " 出发",
            color = TextDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        Cities.popular.filter { it.code != from.code }.forEach { c ->
            val km = Geo.distanceKm(from, c)
            val min = Geo.durationMinutes(from, c)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TextPrimary.copy(alpha = 0.045f))
                    .clickable { onPick(c) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(c.name, color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        from.code + " → " + c.code + "　" + Geo.formatDistance(km),
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Text(
                    Geo.formatDuration(min),
                    color = Gold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))
    }
}
