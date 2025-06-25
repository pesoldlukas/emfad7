package com.emfad.app.services

import android.graphics.Color
import com.emfad.app.models.EMFADMeasurement
import com.emfad.app.models.HeatmapGrid
import com.emfad.app.models.HeatmapVoxel
import com.emfad.app.models.MaterialType
import com.google.ar.sceneform.math.Vector3
import kotlin.math.roundToInt

class HeatmapGenerator {

    fun generateHeatmap(measurements: List<EMFADMeasurement>, resolution: Float = 0.1f): HeatmapGrid {
        if (measurements.isEmpty()) {
            return HeatmapGrid(1, 1, 1, resolution)
        }

        // Determine grid dimensions based on measurement positions
        val minX = measurements.minOf { it.x }
        val minY = measurements.minOf { it.y }
        val minZ = measurements.minOf { it.z }
        val maxX = measurements.maxOf { it.x }
        val maxY = measurements.maxOf { it.y }
        val maxZ = measurements.maxOf { it.z }

        val xDim = ((maxX - minX) / resolution).roundToInt() + 1
        val yDim = ((maxY - minY) / resolution).roundToInt() + 1
        val zDim = ((maxZ - minZ) / resolution).roundToInt() + 1

        val heatmapGrid = HeatmapGrid(xDim, yDim, zDim, resolution)

        measurements.forEach { measurement ->
            val gridX = ((measurement.x - minX) / resolution).roundToInt()
            val gridY = ((measurement.y - minY) / resolution).roundToInt()
            val gridZ = ((measurement.z - minZ) / resolution).roundToInt()

            val materialType = measurement.materialType
            val confidence = measurement.confidence

            val color = getColorForMaterialType(materialType)
            val alpha = getAlphaForConfidence(confidence)

            val voxel = HeatmapVoxel(
                position = Vector3(measurement.x, measurement.y, measurement.z),
                value = confidence.toDouble(), // Use confidence as the value for heatmap
                color = color,
                alpha = alpha,
                label = "${materialType.name}: ${String.format("%.2f", confidence)}"
            )
            heatmapGrid.setVoxel(gridX, gridY, gridZ, voxel)
        }

        heatmapGrid.normalizeValues()
        return heatmapGrid
    }

    private fun getColorForMaterialType(materialType: MaterialType): Color {
        return when (materialType) {
            MaterialType.FERROUS_METAL -> Color.rgb(255, 0, 0) // Red
            MaterialType.NON_FERROUS_METAL -> Color.rgb(0, 255, 0) // Green
            MaterialType.CAVITY -> Color.rgb(0, 0, 255) // Blue
            MaterialType.CRYSTAL -> Color.rgb(255, 0, 255) // Magenta
            MaterialType.WATER -> Color.rgb(0, 255, 255) // Cyan
            MaterialType.UNKNOWN -> Color.rgb(128, 128, 128) // Gray
            else -> Color.rgb(128, 128, 128) // Default to gray for any other new types
        }
    }

    private fun getAlphaForConfidence(confidence: Float): Float {
        // Transparency based on confidence (0.0-1.0), higher confidence means less transparent
        // We want higher confidence to be more opaque, so 1.0 - (1.0 - confidence) * 0.5
        // This means confidence 0.0 -> 0.5 alpha, confidence 1.0 -> 1.0 alpha
        return 0.5f + (confidence * 0.5f)
    }
}
}