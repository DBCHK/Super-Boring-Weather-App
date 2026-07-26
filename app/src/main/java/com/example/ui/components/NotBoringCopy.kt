package com.example.ui.components

import com.example.data.model.WeatherCondition
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random

/** Daypart greeting + Not Boring–style witty weather copy. */
object NotBoringCopy {

    /** Signature brand line — always front-and-center on yellow stage. */
    const val TAGLINE = "Life's too short to waste on boring apps."

    const val BRAND = "NOT BORING WEATHER"

    fun dayPartGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String =
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Night owl mode"
        }

    fun conditionStory(
        condition: WeatherCondition,
        tempC: Float,
        humidity: Int,
        precipChance: Int,
        windMph: Float
    ): String {
        val base = when (condition) {
            WeatherCondition.SUNNY -> listOf(
                "The sun is doing unpaid overtime. Sunglasses are non-negotiable.",
                "Blue sky, zero notes. A rare W for the atmosphere.",
                "Vitamin D is free today. Take as directed."
            )
            WeatherCondition.CLEAR -> listOf(
                "Clear skies. The universe said 'say less.'",
                "Night clear enough to overthink under. Iconic.",
                "No clouds. Just vibes and existential starlight."
            )
            WeatherCondition.PARTLY_CLOUDY -> listOf(
                "Mostly clear with a side of drama. Classic.",
                "Clouds dabbling. The sun still has main-character energy.",
                "Partly cloudy — nature's soft filter."
            )
            WeatherCondition.MOSTLY_CLOUDY, WeatherCondition.CLOUDY -> listOf(
                "Cloud cover: maximum coziness potential.",
                "The sky put on a grey sweater. Mood.",
                "Clouds doing the most. Again."
            )
            WeatherCondition.RAINY -> listOf(
                "Rain has entered the chat. Umbrella diplomacy recommended.",
                "Liquid sunshine (the fake kind). Puddle hopping optional.",
                "It's raining — your playlist just got better."
            )
            WeatherCondition.HEAVY_RAIN -> listOf(
                "Heavy rain. Stay inside and romanticize it.",
                "The clouds are stress-crying. Bring a real jacket.",
                "Rain so extra it needs its own publicist."
            )
            WeatherCondition.THUNDERSTORM -> listOf(
                "Thunder is freestyling. Nature's bass drop.",
                "Lightning said 'watch this.' We are watching.",
                "Storm mode unlocked. Charge devices, cancel plans."
            )
            WeatherCondition.SNOWY -> listOf(
                "Snow day energy whether work agrees or not.",
                "Flakes falling. Soft-launching winter cosplay.",
                "It's snowing — time to become a soup person."
            )
            WeatherCondition.HAZE -> listOf(
                "Hazy. The world is on soft focus today.",
                "Mist mode. Main character fog walk available.",
                "Visibility low, aesthetic high."
            )
            WeatherCondition.WINDY -> listOf(
                "Wind is rearranging hairstyles without consent.",
                "Gusty. Hold onto your hat and your dignity.",
                "The air is speedrunning past your face."
            )
        }
        val extra = when {
            precipChance >= 70 -> " Rain odds are spicy ($precipChance%)."
            tempC >= 32f -> " Heat is not playing."
            tempC <= 2f -> " Bundle up like you mean it."
            windMph >= 20f -> " Wind's being dramatic (${windMph.roundToInt()} mph)."
            humidity >= 80 -> " Humidity: sticky notes included."
            else -> ""
        }
        return base[Random(condition.ordinal + tempC.roundToInt()).nextInt(base.size)] + extra
    }

    /**
     * 0–100 "vibe score" — playful composite of temp comfort, humidity, wind, rain.
     */
    fun vibeScore(tempC: Float, humidity: Int, windMph: Float, precipChance: Int, uv: Float): Int {
        // Ideal outdoor comfort ~18–24°C
        val tempScore = when {
            tempC in 18f..24f -> 100f
            tempC in 14f..28f -> 85f
            tempC in 8f..32f -> 65f
            tempC in 0f..36f -> 45f
            else -> 25f
        }
        val humidityScore = when {
            humidity in 35..60 -> 100f
            humidity in 25..75 -> 80f
            else -> 50f
        }
        val windScore = when {
            windMph < 8f -> 100f
            windMph < 15f -> 85f
            windMph < 25f -> 60f
            else -> 35f
        }
        val rainScore = (100 - precipChance).toFloat()
        val uvScore = when {
            uv <= 5f -> 100f
            uv <= 8f -> 80f
            else -> 55f
        }
        val raw =
            tempScore * 0.35f +
                humidityScore * 0.15f +
                windScore * 0.15f +
                rainScore * 0.25f +
                uvScore * 0.10f
        return raw.roundToInt().coerceIn(0, 100)
    }

    fun vibeLabel(score: Int): String = when {
        score >= 90 -> "ELITE VIBES"
        score >= 75 -> "PRETTY PERFECT"
        score >= 60 -> "SOLID DAY"
        score >= 45 -> "MID ENERGY"
        score >= 30 -> "SURVIVAL MODE"
        else -> "PLOT TWIST WEATHER"
    }

    fun vibeTip(score: Int, condition: WeatherCondition): String = when {
        score >= 85 -> "Go outside. Touch grass. Be legendary."
        score >= 70 -> "A walk wouldn't hurt. Maybe a coffee."
        score >= 50 -> "Windows open energy. Soft plans only."
        condition == WeatherCondition.RAINY || condition == WeatherCondition.HEAVY_RAIN ->
            "Indoor main character arc. Hot drink required."
        score < 40 -> "Remote day if you can. Blanket fortress approved."
        else -> "Dress in layers. Expect plot."
    }
}
