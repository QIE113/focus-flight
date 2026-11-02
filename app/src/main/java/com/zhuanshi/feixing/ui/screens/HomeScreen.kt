package com.zhuanshi.feixing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.Cities
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.map.Camera
import com.zhuanshi.feixing.map.TileMap
import com.zhuanshi.feixing.map.TileSource
import com.zhuanshi.feixing.ui.pressScale
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 拼音表。视频里 2.1s 输入 teluomu 出「特罗姆瑟」，所以按前缀匹配，
 * 打到一半（telu）不该出结果，跟视频一致。
 */
private val PINYIN: Map<String, String> = linkedMapOf(
    "HGH" to "hangzhou", "TOS" to "teluomu", "PEK" to "beijing", "PVG" to "shanghai",
    "CAN" to "guangzhou", "SZX" to "shenzhen", "CTU" to "chengdu", "CKG" to "chongqing",
    "XIY" to "xian", "KMG" to "kunming", "XMN" to "xiamen", "TAO" to "qingdao",
    "HKG" to "xianggang", "TPE" to "taibei", "ICN" to "shouer", "NRT" to "dongjing",
    "SIN" to "xinjiapo", "BKK" to "mangu", "DXB" to "dibai", "LHR" to "lundun",
    "CDG" to "bali", "JFK" to "niuyue", "LAX" to "luoshanji", "SYD" to "xini",
    "SVO" to "mosike"
)

private fun matchCity(q: String): City? {
    val raw = q.trim()
    if (raw.isEmpty()) return null
    val s = raw.lowercase()
    val pool = Cities.all
    pool.firstOrNull { it.code.lowercase().startsWith(s) }?.let { return it }
    pool.firstOrNull { it.name.contains(raw) }?.let { return it }
    val code = PINYIN.entries.firstOrNull { it.value.startsWith(s) }?.key ?: return null
    return pool.firstOrNull { it.code == code }
}

private fun kmBetween(a: City, b: City): Double {
    val p = Math.PI / 180.0
    val la1 = a.lat * p
    val lo1 = a.lon * p
    val la2 = b.lat * p
    val lo2 = b.lon * p
    val h = sin((la2 - la1) / 2) * sin((la2 - la1) / 2) +
            cos(la1) * cos(la2) * sin((lo2 - lo1) / 2) * sin((lo2 - lo1) / 2)
    return 2 * 6371.0 * asin(min(1.0, sqrt(h)))
}

/** 顶栏小圆钮。 */
@Composable
private fun CircleBtn(glyph: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(34.dp)
            .pressScale(src, 0.9f)
            .background(Color(0x66090D13), CircleShape)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = Color(0xFFE9EFF6), fontSize = 13.sp)
    }
}

@Composable
fun HomeScreen(
    from: City,
    to: City,
    minutes: Int,
    km: Double,
    onRoute: (City, City) -> Unit,
    onPickRoute: () -> Unit,
    onRecords: () -> Unit,
    onTrend: () -> Unit,
    onWorld: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val hit = remember(query) { matchCity(query) }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        // 底图：视频里搜索页是压得很暗的、几乎只剩陆地轮廓的世界图
        TileMap(
            camera = Camera(34.0, 108.0, 3.0f),
            source = TileSource.AMAP_VEC,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color(0xE6070A10)))

        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleBtn("‹") { query = "" }
            Spacer(Modifier.weight(1f))
            Box {
                CircleBtn("⚠") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("记录") }, onClick = { menu = false; onRecords() })
                    DropdownMenuItem(text = { Text("趋势") }, onClick = { menu = false; onTrend() })
                    DropdownMenuItem(text = { Text("世界") }, onClick = { menu = false; onWorld() })
                }
            }
            Spacer(Modifier.width(9.dp))
            CircleBtn("⇄") { onRoute(to, from) }
            Spacer(Modifier.width(9.dp))
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .height(34.dp)
                    .pressScale(src, 0.96f)
                    .background(Color(0xFFF3F5F7), CircleShape)
                    .clickable(interactionSource = src, indication = null) { query = "" }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⌕ 搜索", color = Color(0xFF11151B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 搜索区放在屏高约 32% 处，结果卡与输入框重叠
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val topPad = maxHeight * 0.32f
            Column(Modifier.fillMaxWidth().padding(top = topPad)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .height(52.dp)
                        .background(Color(0xF20C1016), RoundedCornerShape(15.dp))
                        .border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(15.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", color = Color(0xFF8E97A3), fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFFF2F6FA), fontSize = 15.sp),
                        cursorBrush = SolidColor(Color(0xFFF2F6FA)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text("搜索城市 / 机场代码 / 拼音", color = Color(0xFF6B7480), fontSize = 13.sp)
                                }
                                inner()
                            }
                        }
                    )
                }

                if (hit != null) {
                    Column(Modifier.fillMaxWidth().offset(y = (-14).dp).padding(horizontal = 22.dp)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xF214171D), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            // 卡顶黄色徽章
                            Box(
                                Modifier
                                    .background(Color(0xFFF5CE3A), RoundedCornerShape(7.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("✈ " + hit.code, color = Color(0xFF15130A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                hit.name,
                                color = Color(0xFFF5F8FB),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                from.code + " → " + hit.code,
                                color = Color(0xFF7E8794),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(top = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(Geo.formatDistance(kmBetween(from, hit)), color = TextDim, fontSize = 12.sp)
                                Text(durationOf(kmBetween(from, hit)), color = TextDim, fontSize = 12.sp)
                            }
                        }
                        Box(
                            Modifier
                                .padding(top = 16.dp)
                                .background(Color(0xFFF4F6F8), CircleShape)
                                .clickable { onRoute(hit, to); onPickRoute() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "选择这趟航班",
                                color = Color(0xFF0A0E15),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else if (query.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.66f)
                                .height(42.dp)
                                .background(Color(0x1FFFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("没有可选的航班", color = Color(0xFFB4BBC4), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Text(
            text = TileSource.AMAP_VEC.attribution,
            color = Color(0x66E8EEF6),
            fontSize = 7.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 10.dp)
        )
        Text(
            text = "当前航线 " + from.code + " → " + to.code + " · " + Geo.formatDistance(km) + " / " + Geo.formatDuration(minutes),
            color = Color(0x66E8EEF6),
            fontSize = 7.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 10.dp)
        )
    }
}

/** 视频登机牌上是 9h 21m 这种写法，这里按 820 km/h 估。 */
private fun durationOf(km: Double): String {
    val totalMin = (km / 820.0 * 60.0).toInt().coerceAtLeast(1)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) (h.toString() + "h " + m.toString() + "m") else (m.toString() + "m")
}
