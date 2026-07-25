package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.filament.Renderer
import com.google.android.filament.View
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
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
    scaleToUnits: Float = 1.0f
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelPath)
    val renderer = rememberRenderer(engine)
    val view = rememberView(engine)
    // Create an environment with NO skybox for pure transparency
    val environment = rememberEnvironment(engine) {
        Environment(indirectLight = null, skybox = null)
    }

    LaunchedEffect(view, renderer) {
        // Disable Temporal Anti-Aliasing (TAA) which causes ghosting/trailing
        view.antiAliasing = View.AntiAliasing.FXAA
        // Disable Dithering which can cause visible patterns in transparent areas
        view.dithering = View.Dithering.NONE
        
        // Sr. Dev Fix: Force clear the buffer every frame with transparent black
        // This eliminates the "Hall of Mirrors" / "Brush" trail effect
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        }

        // Disable post-processing artifacts that create the "subtle square"
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = false }
        view.bloomOptions = view.bloomOptions.apply { enabled = false }
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
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = scaleToUnits,
                position = Position(y = offsetY),
                rotation = Rotation(
                    x = interactionState.pitch,
                    y = interactionState.yaw,
                    z = 0f
                )
            )
        }
    }
}
