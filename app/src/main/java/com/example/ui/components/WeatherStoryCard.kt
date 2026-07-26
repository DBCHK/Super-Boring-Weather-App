package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.ui.theme.LocalThemePalette
import com.example.util.rememberDropletPlayers
import kotlinx.coroutines.delay

/**
 * Rotating witty weather story — the personality layer Not Boring is known for.
 */
@Composable
fun WeatherStoryCard(
    condition: WeatherCondition,
    tempC: Float,
    humidity: Int,
    precipChance: Int,
    windMph: Float,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()
    var seed by remember { mutableIntStateOf(0) }

    LaunchedEffect(condition, tempC) {
        while (true) {
            delay(6500)
            seed++
        }
    }

    val story = remember(condition, tempC, humidity, precipChance, windMph, seed) {
        NotBoringCopy.conditionStory(condition, tempC, humidity, precipChance, windMph)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .bouncyClick {
                feedback.bubble()
                seed++
            }
            .padding(18.dp)
    ) {
        Text(
            text = "THE WEATHER, BUT MAKE IT INTERESTING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.9.sp,
            color = palette.secondaryText
        )
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedContent(
            targetState = story,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 4 }) togetherWith
                    (fadeOut() + slideOutVertically { -it / 4 })
            },
            label = "story"
        ) { text ->
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = palette.primaryText,
                lineHeight = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "tap for another take · auto-rotates",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = palette.tertiaryText
        )
    }
}
