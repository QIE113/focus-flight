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
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.Appear
import com.zhuanshi.feixing.ui.TopBar
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun RecordScreen(records: List<FlightRecord>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "飞行记录",
            sub = "共 " + records.size + " 段 · 累计 " + Geo.formatDuration(records.sumOf { it.minutes }),
            onBack = onBack
        )
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有落地记录，先飞一趟", color = TextDim, fontSize = 12.sp);
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records.reversed()) { r ->
                    Appear(delayMillis = 20) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0x66101419), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.fromCode + " → " + r.toCode, color = TextPrimary, fontSize = 14.sp, letterSpacing = 1.sp);
                                Text(
                                    text = r.fromName + " · " + r.toName,
                                    color = TextDim,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                );
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Geo.formatDuration(r.minutes), color = Gold, fontSize = 12.sp);
                                Text(Geo.formatDate(r.finishedAt), color = TextDim, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp));
                            }
                        }
                    }
                }
            }
        }
    }
}
