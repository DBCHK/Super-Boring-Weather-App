package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.View
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.colorOf
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberView

@Composable
fun ThreeDDigitsRow(
    number: Int,
    modifier: Modifier = Modifier,
    interactionState: Interactive3DState,
    scaleToUnits: Float = 1.0f,
    spacing: Float = 0.8f,
    /** Primary digit color — white on dark theme, charcoal on light. */
    fillColor: Color = Color(0xFF1C1C1E),
    shadeColor: Color = Color(0xFF3A3A3C),
    shadowColor: Color = Color(0xFFAEAEB2),
    highlightColor: Color = Color(0xFF636366),
    /** When false, parent owns drag/tilt so weather + digits move together. */
    enableGestures: Boolean = true
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val renderer = rememberRenderer(engine)
    val view = rememberView(engine)

    val environment = rememberEnvironment(engine) {
        Environment(indirectLight = null, skybox = null)
    }

    LaunchedEffect(view, renderer) {
        view.antiAliasing = View.AntiAliasing.FXAA
        view.dithering = View.Dithering.NONE
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        }
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = false }
        view.bloomOptions = view.bloomOptions.apply { enabled = false }
    }

    val digits = number.toString().map { it.digitToInt() }

    val modelInstances = digits.map { digit ->
        rememberModelInstance(modelLoader, "models/digit_$digit.glb")
    }

    // IMPORTANT: key on number + colors only — never the list identity (recreated each frame).
    // Re-tinting every rotation frame was the dark-mode digit flicker.
    LaunchedEffect(number, fillColor, shadeColor) {
        modelInstances.forEach { inst ->
            inst?.applyThemeTint(fillColor, shadeColor)
        }
    }

    // Shared rotation with weather hero — full linked pitch + yaw (pitch already clamped)
    val yaw = interactionState.renderYaw
    val pitch = interactionState.renderPitch

    Box(
        modifier = if (enableGestures) {
            modifier.interactive3D(
                interactionState,
                enablePitch = false,
                enableDeviceTilt = true
            )
        } else {
            modifier
        },
        contentAlignment = Alignment.Center
    ) {
        // Soft ground shadow for depth / readability
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.78f
            val shadowW = size.width * 0.38f
            val shadowH = size.height * 0.10f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        shadowColor.copy(alpha = 0.40f),
                        shadowColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(cx + 4f, cy + 2f),
                    radius = shadowW
                ),
                topLeft = Offset(cx - shadowW, cy - shadowH),
                size = androidx.compose.ui.geometry.Size(shadowW * 2f, shadowH * 2f)
            )
        }

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            renderer = renderer,
            view = view,
            environment = environment,
            isOpaque = false,
            renderQuality = RenderQuality.Default,
            surfaceType = SurfaceType.TextureSurface
        ) {
            // Stable lighting — no per-frame tint
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 95_000f,
                color = colorOf(Color.White),
                direction = Direction(0.35f, -1f, -0.55f)
            )
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 36_000f,
                color = colorOf(
                    Color(
                        highlightColor.red.coerceAtLeast(0.55f),
                        highlightColor.green.coerceAtLeast(0.58f),
                        highlightColor.blue.coerceAtLeast(0.62f)
                    )
                ),
                direction = Direction(-0.55f, -0.25f, 0.45f)
            )

            val totalWidth = (digits.size - 1) * spacing
            val startX = -totalWidth / 2f

            digits.forEachIndexed { index, _ ->
                modelInstances.getOrNull(index)?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = scaleToUnits,
                        position = Position(x = startX + index * spacing),
                        rotation = Rotation(
                            x = pitch,
                            y = yaw,
                            z = 0f
                        )
                    )
                }
            }
        }
    }
}
