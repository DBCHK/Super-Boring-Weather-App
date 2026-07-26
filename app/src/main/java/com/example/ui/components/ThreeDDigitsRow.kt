package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

/**
 * 3D temperature digits with stable Filament lifecycle.
 *
 * Glitch fixes:
 * - One directional light only (two LightNodes both typed DIRECTIONAL shared one
 *   remember(engine, type) key and fought over intensity/direction every frame).
 * - Fixed 3 composition slots for digit models (no map-of-remember that shifts slots).
 * - Separate ModelInstance per slot so repeated digits (e.g. 55) never share a root entity.
 * - Parent [Node] owns pitch/yaw + stick orbit; child ModelNodes keep fixed local X only
 *   so rotation does not thrash per-digit transform + scaleToUnits.
 * - autoAnimate = false (digit meshes should not play glTF clips while spinning).
 */
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
    enableGestures: Boolean = true,
    /**
     * When false, digits do not self-rotate. Prefer true for real Filament 3D depth.
     */
    applyInteractionRotation: Boolean = true,
    /**
     * Signed stick arm for rigid hero motion (− = bottom of stick). Orbits in 3D with pitch/yaw.
     */
    stickArmY: Float = 0f
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

    // Always 1–3 digits for weather temps; pad composition with fixed slot count.
    val digits = remember(number) {
        number.coerceIn(-99, 999).toString()
            .filter { it.isDigit() }
            .map { it.digitToInt() }
            .ifEmpty { listOf(0) }
            .take(3)
    }
    val digitCount = digits.size

    // Fixed composition slots — always call remember the same number of times.
    // Each slot owns its own ModelInstance so "55" works (cannot share one instance twice).
    val path0 = "models/digit_${digits.getOrElse(0) { 0 }}.glb"
    val path1 = "models/digit_${digits.getOrElse(1) { 0 }}.glb"
    val path2 = "models/digit_${digits.getOrElse(2) { 0 }}.glb"
    val instance0 = rememberModelInstance(modelLoader, path0)
    val instance1 = rememberModelInstance(modelLoader, path1)
    val instance2 = rememberModelInstance(modelLoader, path2)

    // Tint only when theme or digit asset changes — never on rotation frames.
    LaunchedEffect(path0, fillColor, shadeColor, instance0) {
        instance0?.applyThemeTint(fillColor, shadeColor)
    }
    LaunchedEffect(path1, fillColor, shadeColor, instance1) {
        if (digitCount > 1) instance1?.applyThemeTint(fillColor, shadeColor)
    }
    LaunchedEffect(path2, fillColor, shadeColor, instance2) {
        if (digitCount > 2) instance2?.applyThemeTint(fillColor, shadeColor)
    }

    val yaw = if (applyInteractionRotation) interactionState.renderYaw else 0f
    val pitch = if (applyInteractionRotation) interactionState.renderPitch else 0f
    val stick = stickRigidDelta(pitch, yaw, stickArmY)

    // Local X positions for visible slots only (centered row).
    val localXs = remember(digitCount, spacing) {
        val totalWidth = (digitCount - 1) * spacing
        val startX = -totalWidth / 2f
        FloatArray(3) { i -> startX + i * spacing }
    }

    // Soft key light color from highlight (stable object to avoid needless light SideEffect noise)
    val keyColor = remember(highlightColor) {
        colorOf(
            Color(
                highlightColor.red.coerceAtLeast(0.70f),
                highlightColor.green.coerceAtLeast(0.72f),
                highlightColor.blue.coerceAtLeast(0.75f)
            )
        )
    }

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
        // Soft ground shadow — shifts with yaw so it reads as 3D contact
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f + yaw * 1.1f
            val cy = size.height * 0.78f + pitch * 0.35f
            val depthScale = (1f - stick.z * 0.12f).coerceIn(0.82f, 1.15f)
            val shadowW = size.width * 0.38f * depthScale
            val shadowH = size.height * 0.10f * depthScale
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        shadowColor.copy(alpha = 0.42f),
                        shadowColor.copy(alpha = 0.12f),
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
            // SINGLE directional light — never add a second LightNode of the same type.
            // SceneView's LightNode is remembered as remember(engine, type), so two
            // DIRECTIONAL lights alias one node and thrash intensity/direction every frame.
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                intensity = 145_000f,
                color = keyColor,
                direction = Direction(0.35f, -1f, -0.55f)
            )

            // Parent carries all rotation + stick orbit; children stay locally fixed.
            Node(
                position = Position(x = stick.x, y = stick.y, z = stick.z),
                rotation = Rotation(x = pitch, y = yaw, z = 0f)
            ) {
                if (digitCount > 0) {
                    instance0?.let { inst ->
                        ModelNode(
                            modelInstance = inst,
                            autoAnimate = false,
                            scaleToUnits = scaleToUnits,
                            position = Position(x = localXs[0], y = 0f, z = 0f)
                        )
                    }
                }
                if (digitCount > 1) {
                    instance1?.let { inst ->
                        ModelNode(
                            modelInstance = inst,
                            autoAnimate = false,
                            scaleToUnits = scaleToUnits,
                            position = Position(x = localXs[1], y = 0f, z = 0f)
                        )
                    }
                }
                if (digitCount > 2) {
                    instance2?.let { inst ->
                        ModelNode(
                            modelInstance = inst,
                            autoAnimate = false,
                            scaleToUnits = scaleToUnits,
                            position = Position(x = localXs[2], y = 0f, z = 0f)
                        )
                    }
                }
            }
        }
    }
}
