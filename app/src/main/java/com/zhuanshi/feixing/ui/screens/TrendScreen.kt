package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import java.util.Calendar

@Composable
fun TrendScreen(records: List<FlightRecord>) {
    val valid = records.filter { it.minutes > 0 }.reversed().take(7)

    Column(Modifier.fillMaxWidth()) {
        Text("趋势", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        if (valid.isEmpty()) {
            Text("还没有数据", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                "完成 1 次旅程后，这里会显示最近 7 次的时长。",
                color = TextDim,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(18.dp))
            return@Column
        }

        val peak = valid.maxOf { it.minutes }
        Text(
            "最长 " + Geo.formatDuration(peak),
            color = TextDim,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(22.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            valid.forEach { r ->
                val ratio = if (peak == 0) 0.08f else {
                    (r.minutes.toFloat() / peak.toFloat()).coerceIn(0.08f, 1f)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight(ratio * 0.76f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Gold.copy(alpha = 0.72f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        dayLabel(r.finishedAt),
                        color = TextDim,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        r.minutes.toString(),
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun dayLabel(epoch: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epoch
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "一"
        Calendar.TUESDAY -> "二"
        Calendar.WEDNESDAY -> "三"
        Calendar.THURSDAY -> "四"
        Calendar.FRIDAY -> "五"
        Calendar.SATURDAY -> "六"
        else -> "日"
    }
}
