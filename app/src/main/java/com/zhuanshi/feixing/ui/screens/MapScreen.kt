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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.data.City
import com.zhuanshi.feixing.data.Geo
import com.zhuanshi.feixing.ui.globe.GlobeMap
import com.zhuanshi.feixing.ui.globe.GlobeMath
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.PanelBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * 全屏地图：双指自由缩放（1× - 16×）、单指拖动旋转，另配 +/−/复位 三个按钮。
 * 底图是内置的世界陆地轮廓，完全离线，不联网、不要 Google 地图 SDK。
 */
@Composable
fun MapScreen(
    from: City,
    to: City,
    progress: Float = -1f,
    onClose: () -> Unit
) {
    val cam = remember(from.code, to.code) { GlobeMath.routeCamera(from, to) }
    var scale by remember { mutableFloatStateOf(2.4f) }
    var lon by remember { mutableFloatStateOf(cam[0]) }
    var lat by remember { mutableFloatStateOf(cam[1]) }

    val distance = remember(from.code, to.code) { Geo.distanceKm(from, to) }
    val minutes = remember(from.code, to.code) { Geo.durationMinutes(from, to) }

    Box(Modifier.fillMaxSize().background(NightBg)) {
        GlobeMap(
            from = from,
            to = to,
            modifier = Modifier.fillMaxSize(),
            progress = progress,
            scale = scale,
            rotLon = lon,
            rotLat = lat,
            interactive = true,
            onCamera = { s, lo, la ->
                scale = s
                lon = lo
                lat = la
            }
        )

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("航线地图", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "放大 " + zoomLabel(scale) + "　·　双指缩放 / 单指拖动",
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
            Text(
                "完成",
                color = Gold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PanelBg)
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ZoomButton("+") {
                scale = (scale * 1.6f).coerceAtMost(16f)
            }
            ZoomButton("−") {
                scale = (scale / 1.6f).coerceAtLeast(1f)
            }
            ZoomButton("◎") {
                scale = 2.4f
                lon = cam[0]
                lat = cam[1]
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PanelBg)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    from.code + "  →  " + to.code,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    Geo.formatDuration(minutes),
                    color = Gold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                from.name + " → " + to.name + "　·　" + Geo.formatDistance(distance)
                    + "　·　" + Geo.durationHoursText(from, to) + " 小时",
                color = TextDim,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip("回航线") {
                    scale = 2.4f
                    lon = cam[0]
                    lat = cam[1]
                }
                MiniChip("看" + from.name) {
                    scale = 6f
                    lon = -from.lon.toFloat()
                    lat = from.lat.toFloat()
                }
                MiniChip("看" + to.name) {
                    scale = 6f
                    lon = -to.lon.toFloat()
                    lat = to.lat.toFloat()
                }
            }
        }
    }
}

private fun zoomLabel(scale: Float): String {
    val v = (scale * 10f).roundToInt() / 10f
    return "×" + v
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(PanelBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 17.sp)
    }
}

@Composable
private fun MiniChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = TextPrimary.copy(alpha = 0.9f),
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(TextPrimary.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
