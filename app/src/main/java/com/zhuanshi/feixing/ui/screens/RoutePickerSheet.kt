package com.zhuanshi.feixing.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
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
    var editing by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val anchor = if (editing == 0) to else from
    val excluded = if (editing == 0) to.code else from.code

    val options = remember(from.code, to.code, editing, query) {
        Cities.all
            .filter { it.code != excluded }
            .filter {
                val q = query.trim()
                if (q.isEmpty()) true
                else it.name.contains(q) || it.code.contains(q.uppercase())
            }
            .sortedBy { Geo.distanceKm(anchor, it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NightBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "选择航线",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "取消",
                color = TextDim,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onDismiss() }
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "先点「出发」定起点，再点「到达」定终点",
            color = TextDim,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EndpointCard(
                label = "出发",
                city = from,
                active = editing == 0,
                modifier = Modifier.weight(1f),
                onClick = { editing = 0 }
            )
            Text(
                "⇄",
                color = Gold,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable {
                        val tmp = from
                        from = to
                        to = tmp
                    }
            )
            EndpointCard(
                label = "到达",
                city = to,
                active = editing == 1,
                modifier = Modifier.weight(1f),
                onClick = { editing = 1 }
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text("搜索城市或三字码", color = TextDim, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(Gold),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(options) { city ->
                val chosen = city.code == if (editing == 0) from.code else to.code
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (chosen) Gold.copy(alpha = 0.14f)
                            else TextPrimary.copy(alpha = 0.045f)
                        )
                        .clickable {
                            if (editing == 0) {
                                from = city
                                editing = 1
                            } else {
                                to = city
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(city.name, color = TextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            city.code + "　" + Geo.formatDistance(Geo.distanceKm(anchor, city)),
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Geo.formatDuration(Geo.durationMinutes(anchor, city)),
                            color = Gold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            Geo.durationHoursText(anchor, city) + " 小时",
                            color = TextDim,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(
                from.code + " → " + to.code + "　·　" + Geo.formatDistance(Geo.distanceKm(from, to))
                    + "　·　真实航程 " + Geo.formatDuration(Geo.durationMinutes(from, to)),
                color = TextPrimary.copy(alpha = 0.82f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "按大圆距离自动计算 " + Geo.durationHoursText(from, to) + " 小时（巡航 840 km/h + 起降 26 分钟）",
                color = TextDim,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(TextPrimary)
                    .clickable(enabled = from.code != to.code) { onConfirm(from, to) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "确认航线 · 去撕登机牌",
                    color = NightBg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EndpointCard(
    label: String,
    city: City,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) Gold.copy(alpha = 0.16f) else PanelBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = if (active) Gold else TextDim, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(city.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(city.code, color = TextDim, fontSize = 11.sp)
    }
}
