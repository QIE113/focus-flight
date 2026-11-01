package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.TopBar
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun TrendScreen(records: List<FlightRecord>, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val day = 24L * 60L * 60L * 1000L
    val buckets = LongArray(7)
    records.forEach { r ->
        val idx = ((now - r.finishedAt) / day).toInt();
        if (idx in 0..6) buckets[6 - idx] += r.minutes.toLong();
    }
    val maxMinutes = buckets.maxOrNull() ?: 0L
    val total = buckets.sum()
    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "最近七天",
            sub = if (total == 0L) "还没有数据" else "累计 " + Geo.formatDuration(total.toInt()),
            onBack = onBack
        )
        Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 24.dp, vertical = 12.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val barW = size.width / 9f
                var i = 0
                while (i < 7) {
                    val v = buckets[i].toFloat();
                    val ratio = if (maxMinutes == 0L) 0f else v / maxMinutes.toFloat();
                    val h = (size.height - 24f) * ratio;
                    val x = size.width / 2f + (i - 3) * (barW + 8f) - barW / 2f;
                    drawRoundRect(
                        color = if (v > 0f) Gold else Color(0x22FFFFFF),
                        topLeft = Offset(x, size.height - 18f - h),
                        size = Size(barW, h.coerceAtLeast(3f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    );
                    i++
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            val labels = listOf("6天前", "5", "4", "3", "2", "昨天", "今天");
            labels.forEach { l ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(l, color = TextDim, fontSize = 9.sp);
                }
            }
        }
        Text(
            text = "每天飞一段，把注意力交还给天空",
            color = TextDim.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        );
    }
}
