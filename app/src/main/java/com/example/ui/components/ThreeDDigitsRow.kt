package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.filament.Renderer
import com.google.android.filament.View
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
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
    spacing: Float = 0.8f
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
    
    // We need to load all necessary model instances
    val modelInstances = digits.map { digit ->
        rememberModelInstance(modelLoader, "models/digit_$digit.glb")
    }

    Box(
        modifier = modifier.interactive3D(interactionState, enablePitch = true),
        contentAlignment = Alignment.Center
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            renderer = renderer,
            view = view,
            environment = environment,
            isOpaque = false,
            renderQuality = RenderQuality.Performance,
            surfaceType = SurfaceType.TextureSurface
        ) {
            val totalWidth = (digits.size - 1) * spacing
            val startX = -totalWidth / 2f

            digits.forEachIndexed { index, _ ->
                modelInstances.getOrNull(index)?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = scaleToUnits,
                        position = Position(x = startX + index * spacing),
                        rotation = Rotation(
                            x = interactionState.pitch,
                            y = interactionState.yaw,
                            z = 0f
                        )
                    )
                }
            }
        }
    }
}
