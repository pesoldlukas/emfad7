package com.emfad.app.models

import android.graphics.Color
import com.google.ar.sceneform.math.Vector3
import kotlin.math.max
import kotlin.math.min

class HeatmapGrid(
    private val xDim: Int,
    private val yDim: Int,
    private val zDim: Int,
    private val resolution: Float // Size of each voxel in meters
) {

    private val voxels: Array<Array<Array<HeatmapVoxel?>>>

    init {
        voxels = Array(xDim) { Array(yDim) { arrayOfNulls(zDim) } }
    }

    fun setVoxel(x: Int, y: Int, z: Int, voxel: HeatmapVoxel) {
        if (x in 0 until xDim && y in 0 until yDim && z in 0 until zDim) {
            voxels[x][y][z] = voxel
        }
    }

    fun getVoxel(x: Int, y: Int, z: Int): HeatmapVoxel? {
        return if (x in 0 until xDim && y in 0 until yDim && z in 0 until zDim) {
            voxels[x][y][z]
        } else {
            null
        }
    }

    fun getMaxValue(): Double {
        var maxValue = 0.0
        voxels.forEach { plane ->
            plane.forEach { row ->
                row.forEach { voxel ->
                    voxel?.let { maxValue = max(maxValue, it.value) }
                }
            }
        }
        return maxValue
    }

    fun getMinValue(): Double {
        var minValue = Double.MAX_VALUE
        voxels.forEach { plane ->
            plane.forEach { row ->
                row.forEach { voxel ->
                    voxel?.let { minValue = min(minValue, it.value) }
                }
            }
        }
        return if (minValue == Double.MAX_VALUE) 0.0 else minValue
    }

    fun normalizeValues() {
        val maxValue = getMaxValue()
        if (maxValue > 0) {
            voxels.forEachIndexed { x, plane ->
                plane.forEachIndexed { y, row ->
                    row.forEachIndexed { z, voxel ->
                        voxel?.let { 
                            val normalizedValue = it.value / maxValue
                            // Re-calculate color and alpha based on normalized value if needed
                            // For now, we just update the value itself
                            voxels[x][y][z] = it.copy(value = normalizedValue)
                        }
                    }
                }
            }
        }
    }

    fun getSlice(zIndex: Int): List<HeatmapVoxel> {
        val sliceVoxels = mutableListOf<HeatmapVoxel>()
        if (zIndex in 0 until zDim) {
            for (x in 0 until xDim) {
                for (y in 0 until yDim) {
                    voxels[x][y][zIndex]?.let { sliceVoxels.add(it) }
                }
            }
        }
        return sliceVoxels
    }

    fun getAllVoxels(): List<HeatmapVoxel> {
        val allVoxels = mutableListOf<HeatmapVoxel>()
        voxels.forEach { plane ->
            plane.forEach { row ->
                row.forEach { voxel ->
                    voxel?.let { allVoxels.add(it) }
                }
            }
        }
        return allVoxels
    }

    fun getDimensions(): Triple<Int, Int, Int> {
        return Triple(xDim, yDim, zDim)
    }

    fun getResolution(): Float {
        return resolution
    }
}