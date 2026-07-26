package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Bounce press that fires [onClick] with a soft spring release (Not Boring chrome feel).
 */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.90f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bouncyClick"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = if (pressed) 1.5f else 0f
        }
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
}

/**
 * Staggered entrance for hero / list items — fade + rise.
 */
@Composable
fun rememberEntranceProgress(delayMs: Int = 0, durationMs: Int = 520): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        anim.animateTo(
            1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
    }
    return anim.value
}

fun Modifier.entrance(progress: Float, risePx: Float = 28f): Modifier =
    graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * risePx
        val s = 0.96f + 0.04f * progress
        scaleX = s
        scaleY = s
    }
