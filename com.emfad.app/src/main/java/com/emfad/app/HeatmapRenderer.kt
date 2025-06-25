package com.emfad.app.ar

import android.content.Context
import com.emfad.app.models.HeatmapGrid
import com.emfad.app.models.HeatmapVoxel
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import java.util.concurrent.CompletableFuture

class HeatmapRenderer(private val context: Context) {

    private val voxelNodes = mutableListOf<Node>()

    fun renderHeatmap(heatmapGrid: HeatmapGrid, parentNode: Node) {
        // Clear existing nodes
        voxelNodes.forEach { it.setParent(null) }
        voxelNodes.clear()

        val (xDim, yDim, zDim) = heatmapGrid.getDimensions()
        val resolution = heatmapGrid.getResolution()

        // Offset to center the heatmap grid around the origin of the parentNode
        val offsetX = (xDim * resolution) / 2f
        val offsetY = (yDim * resolution) / 2f
        val offsetZ = (zDim * resolution) / 2f

        heatmapGrid.getAllVoxels().forEach { voxel ->
            createVoxelRenderable(voxel).thenAccept { renderable ->
                val voxelNode = Node()
                voxelNode.setParent(parentNode)
                voxelNode.renderable = renderable

                // Adjust position to account for the center of the voxel and the grid offset
                voxelNode.localPosition = Vector3(
                    voxel.position.x - offsetX + resolution / 2,
                    voxel.position.y - offsetY + resolution / 2,
                    voxel.position.z - offsetZ + resolution / 2
                )
                voxelNodes.add(voxelNode)
            }
        }
    }

    private fun createVoxelRenderable(voxel: HeatmapVoxel): CompletableFuture<ModelRenderable> {
        val color = com.google.ar.sceneform.rendering.Color(voxel.color)
        return MaterialFactory.makeTransparentWithColor(context, color)
            .thenApply { material ->
                material.setFloat("transparency", voxel.alpha)
                ShapeFactory.makeCube(Vector3(voxel.alpha, voxel.alpha, voxel.alpha), Vector3.zero(), material)
            }
    }

    fun clearHeatmap() {
        voxelNodes.forEach { it.setParent(null) }
        voxelNodes.clear()
    }
}