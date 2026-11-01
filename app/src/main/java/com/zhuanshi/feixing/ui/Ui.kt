package com.zhuanshi.feixing.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuanshi.feixing.ui.theme.Gold
import com.zhuanshi.feixing.ui.theme.NightBg
import com.zhuanshi.feixing.ui.theme.TextDim
import com.zhuanshi.feixing.ui.theme.TextPrimary

/** 星空背景：固定随机种子，只画一次，不参与动画。 */
@Composable
fun StarField(modifier: Modifier = Modifier, count: Int = 120) {
    val stars = remember(count) {
        val rnd = java.util.Random(17L)
        val list = ArrayList<FloatArray>(count)
        var i = 0
        while (i < count) {
            list.add(floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 0.3f + rnd.nextFloat() * 0.7f))
            i++
        }
        list
    }
    Canvas(modifier) {
        var i = 0
        while (i < stars.size) {
            val st = stars[i]
            val r = st[2] * 1.7f + 0.5f
            drawCircle(
                color = Color.White.copy(alpha = 0.1f + st[2] * 0.5f),
                radius = r,
                center = Offset(st[0] * size.width, st[1] * size.height)
            )
            i++
        }
    }
}

@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(src, 0.95f)
            .background(if (enabled) Gold else Gold.copy(alpha = 0.22f), RoundedCornerShape(40.dp))
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 34.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) NightBg else TextDim,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(src, 0.94f)
            .border(1.dp, TextDim.copy(alpha = 0.5f), RoundedCornerShape(40.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp, letterSpacing = 0.6.sp)
    }
}

/** 飞行页的圆形操作钮：按下缩小 + 选中发光。 */
@Composable
fun RoundAction(
    label: String,
    sub: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(58.dp)
                .pressScale(src, 0.88f)
                .background(
                    if (active) Gold.copy(alpha = 0.92f) else Color(0xFF11151D),
                    CircleShape
                )
                .border(1.dp, if (active) Gold else Color(0xFF232A36), CircleShape)
                .clickable(interactionSource = src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (active) NightBg else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = sub,
            color = TextDim,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun TopBar(title: String, sub: String, onBack: () -> Unit, right: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val src = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(38.dp)
                .pressScale(src, 0.86f)
                .border(1.dp, Color(0xFF262D39), CircleShape)
                .clickable(interactionSource = src, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("←", color = TextPrimary, fontSize = 17.sp) }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        right()
    }
}

@Composable
fun HairLine(modifier: Modifier = Modifier, alpha: Float = 0.12f) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = alpha)))
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, color = TextDim, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().background(NightBg)) { content() }
}
