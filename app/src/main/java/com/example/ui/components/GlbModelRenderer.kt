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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rigid-stick offset in model space: end sits at local (0, [stickArmY], 0) on a stick
 * pivoted at the origin. Returns delta from that rest pose so models stay framed at rest
 * but orbit in true 3D (Z depth) as pitch/yaw change.
 *
 * @param stickArmY + for top of stick (weather), − for bottom (digits).
 */
fun stickRigidDelta(
    pitchDeg: Float,
    yawDeg: Float,
    stickArmY: Float
): Position {
    if (stickArmY == 0f) return Position(0f)
    val pitch = Math.toRadians(pitchDeg.toDouble())
    val yaw = Math.toRadians(yawDeg.toDouble())
    // Rest local: (0, stickArmY, 0). Apply pitch (X) then yaw (Y).
    val y1 = stickArmY * cos(pitch)
    val z1 = stickArmY * sin(pitch)
    val x2 = z1 * sin(yaw)
    val z2 = z1 * cos(yaw)
    val y2 = y1
    return Position(
        x = x2.toFloat(),
        y = (y2 - stickArmY).toFloat(),
        z = z2.toFloat()
    )
}

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
    enableLighting: Boolean = true,
    /**
     * When false, model stays fixed relative to its parent.
     * Prefer true so Filament rotates the mesh (real 3D depth + lighting).
     */
    applyInteractionRotation: Boolean = true,
    /**
     * Signed arm length along the hero stick (+ top / weather, − bottom / digits).
     * Combined with pitch/yaw for true 3D rigid-body orbit (not flat layer tilt).
     */
    stickArmY: Float = 0f
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

    // Read each frame so ModelNode pitch/yaw/position update in Filament (true 3D)
    val rotX = if (applyInteractionRotation) interactionState.renderPitch else 0f
    val rotY = if (applyInteractionRotation) interactionState.renderYaw else 0f
    val stick = stickRigidDelta(rotX, rotY, stickArmY)

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        renderer = renderer,
        view = view,
        environment = environment,
        isOpaque = false,
        // Default quality: better shading / depth on hero models
        renderQuality = RenderQuality.Default,
        surfaceType = SurfaceType.TextureSurface
    ) {
        if (enableLighting) {
            // ONE directional light only. SceneView remembers LightNode by
            // (engine, type) — multiple DIRECTIONAL nodes alias one entity and
            // thrash intensity/direction every recomposition (visible spin glitch).
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 150_000f,
                color = colorOf(Color(1f, 0.98f, 0.94f)),
                direction = Direction(0.40f, -1f, -0.55f)
            )
        }

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                scaleToUnits = scaleToUnits,
                position = Position(
                    x = stick.x,
                    y = offsetY + stick.y,
                    z = stick.z
                ),
                rotation = Rotation(
                    x = rotX,
                    y = rotY,
                    z = 0f
                )
            )
        }
    }
}
