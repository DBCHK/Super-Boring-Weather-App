package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
fun GlbModelRenderer(
    modelPath: String,
    modifier: Modifier = Modifier,
    interactionState: Interactive3DState,
    offsetY: Float = 0f,
    scaleToUnits: Float = 1.0f,
    /** Theme fill — white in dark mode, dark charcoal on light/yellow. */
    tintColor: Color = Color(0xFF1C1C1E),
    shadeColor: Color = Color(0xFF3A3A3C),
    /** When true, add directional lights so models show depth/shading. */
    enableLighting: Boolean = true
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelPath)
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

    // Re-tint only when theme or asset path changes — never every rotation frame
    LaunchedEffect(modelPath, tintColor, shadeColor, modelInstance) {
        modelInstance?.applyThemeTint(tintColor, shadeColor)
    }

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        renderer = renderer,
        view = view,
        environment = environment,
        isOpaque = false,
        renderQuality = RenderQuality.Performance,
        surfaceType = SurfaceType.TextureSurface
    ) {
        if (enableLighting) {
            // Key light — sculpts form
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 95_000f,
                color = colorOf(Color(1f, 0.98f, 0.95f)),
                direction = Direction(0.35f, -1f, -0.55f)
            )
            // Soft fill — readable shadows, not pure black
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 38_000f,
                color = colorOf(Color(0.75f, 0.82f, 1f)),
                direction = Direction(-0.55f, -0.35f, 0.4f)
            )
        }

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = scaleToUnits,
                position = Position(y = offsetY),
                rotation = Rotation(
                    x = interactionState.renderPitch,
                    y = interactionState.renderYaw,
                    z = 0f
                )
            )
        }
    }
}
