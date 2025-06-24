package com.emfad.app.models

import com.google.ar.sceneform.math.Vector3
import android.graphics.Color

data class HeatmapVoxel(
    val position: Vector3,
    val value: Double,
    val color: Color,
    val alpha: Float,
    val label: String? = null
)