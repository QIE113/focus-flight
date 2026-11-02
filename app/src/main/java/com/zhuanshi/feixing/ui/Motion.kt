package com.zhuanshi.feixing.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 按下就缩一下：让每个按钮都有“手感”，松手回弹。 */
fun Modifier.pressScale(source: MutableInteractionSource, scaleDown: Float = 0.9f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f),
        label = "press"
    )
    graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** 点一下 + 缩一下，自带 interactionSource，省得到处 remember。 */
fun Modifier.tapScale(scaleDown: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    this.pressScale(src, scaleDown).clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** 进场：淡入 + 上浮 + 轻微放大，delay 用来做依次出现的节奏。 */
@Composable
fun Appear(
    visible: Boolean = true,
    delayMillis: Int = 0,
    riseDp: Float = 14f,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            if (delayMillis > 0) delay(delayMillis.toLong())
            shown = true
        }
    }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "appear"
    )
    val rise = with(LocalDensity.current) { riseDp.dp.toPx() }
    Box(
        Modifier.graphicsLayer {
            alpha = p
            translationY = (1f - p) * rise
            val sc = 0.97f + 0.03f * p
            scaleX = sc
            scaleY = sc
        }
    ) {
        content()
    }
}