package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.FlightRecord
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

@Composable
fun RecordScreen(records: List<FlightRecord>) {
    val valid = records.filter { it.minutes > 0 }.reversed()

    Column(Modifier.fillMaxWidth()) {
        Text("记录", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        if (valid.isEmpty()) {
            Text("还没有飞行记录", color = Gold, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Text("完成一次旅程后，航线会出现在这里。", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text("所有记录只存在这台手机上。", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            return@Column
        }

        val totalMin = valid.sumOf { it.minutes }
        Text(
            "共 " + valid.size + " 次 · " + Geo.formatDuration(totalMin),
            color = TextDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        valid.take(30).forEach { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TextPrimary.copy(alpha = 0.045f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        r.fromName + " → " + r.toName,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        Geo.formatDate(r.finishedAt),
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Text(
                    Geo.formatDuration(r.minutes),
                    color = Gold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
