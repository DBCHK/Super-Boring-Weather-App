package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.google.android.filament.MaterialInstance
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.renderableEntities
import io.github.sceneview.model.renderableManager

/**
 * Tints glTF materials toward [fill] so models read correctly on light/dark themes.
 */
fun ModelInstance.applyThemeTint(fill: Color, shade: Color = fill) {
    try {
        val materials = mutableListOf<MaterialInstance>()
        renderableEntities.forEach { entity ->
            val inst = renderableManager.getInstance(entity)
            val count = renderableManager.getPrimitiveCount(inst)
            for (i in 0 until count) {
                materials += renderableManager.getMaterialInstanceAt(inst, i)
            }
        }
        materials.forEach { mi -> tintMaterial(mi, fill) }
    } catch (_: Exception) {
        // Asset materials may not expose tint params
    }
    // Keep shade available for callers / future layered tints
    shade.alpha
}

private fun tintMaterial(mi: MaterialInstance, fill: Color) {
    val r = fill.red
    val g = fill.green
    val b = fill.blue
    val a = fill.alpha.coerceIn(0.85f, 1f)
    val attempts = listOf(
        { mi.setParameter("baseColorFactor", r, g, b, a) },
        { mi.setParameter("baseColor", r, g, b, a) },
        { mi.setParameter("color", r, g, b, a) }
    )
    for (apply in attempts) {
        try {
            apply()
            try {
                mi.setParameter("metallicFactor", 0.15f)
            } catch (_: Exception) {
            }
            try {
                mi.setParameter("roughnessFactor", 0.52f)
            } catch (_: Exception) {
            }
            return
        } catch (_: Exception) {
            // next
        }
    }
}
