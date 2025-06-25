package com.emfad.app.ar

import android.content.Context
import android.graphics.Color
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.math.Vector3
import com.emfad.app.models.EMFADMeasurement
import com.emfad.app.models.MaterialType
import java.util.concurrent.CompletableFuture

/**
 * Responsible for creating specific 3D models or nodes for the Augmented Reality scene
 * based on analyzed EMFAD data.
 */
class ArNodeFactory(private val context: Context) {

    /**
     * Creates a renderable for a given material type.
     * This is a placeholder and should be expanded to load different models/materials.
     */
    private fun createMaterialRenderable(materialType: MaterialType): CompletableFuture<ModelRenderable> {
        val color: com.google.ar.sceneform.rendering.Color
        val transparency: Float

        when (materialType) {
            MaterialType.FERROUS_METAL -> {
                color = com.google.ar.sceneform.rendering.Color(Color.RED)
                transparency = 0.1f // Less transparent
            }
            MaterialType.NON_FERROUS_METAL -> {
                color = com.google.ar.sceneform.rendering.Color(Color.BLUE)
                transparency = 0.1f
            }
            MaterialType.CAVITY -> {
                color = com.google.ar.sceneform.rendering.Color(Color.CYAN)
                transparency = 0.6f // More transparent
            }
            MaterialType.CRYSTAL -> {
                color = com.google.ar.sceneform.rendering.Color(Color.GREEN)
                transparency = 0.8f // Very transparent
            }
            MaterialType.WATER -> {
                color = com.google.ar.sceneform.rendering.Color(Color.parseColor("#ADD8E6")) // Light blue
                transparency = 0.5f
            }
            MaterialType.UNKNOWN -> {
                color = com.google.ar.sceneform.rendering.Color(Color.GRAY)
                transparency = 0.3f
            }
        }

        return MaterialFactory.makeOpaqueWithColor(context, color)
            .thenApply { material ->
                material.setFloat("transparency", transparency)
                ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.0f, 0.0f), material)
            }
    }

    /**
     * Creates an AR node for a single EMFAD measurement.
     */
    fun createMeasurementNode(measurement: EMFADMeasurement): CompletableFuture<MaterialNode> {
        return createMaterialRenderable(measurement.materialType).thenApply {
            val materialNode = MaterialNode(
                context,
                measurement.materialType,
                Vector3(measurement.x, measurement.y, measurement.z),
                measurement.confidence,
                measurement.clusterId,
                "${measurement.materialType.name}: ${String.format("%.2f", measurement.confidence)}",
                it, // Renderable
                it.material // Material
            )
            materialNode.localPosition = Vector3(measurement.x, measurement.y, measurement.z)
            // Scale based on confidence, for example
            val scale = 0.5f + (measurement.confidence * 0.5f) // Scale from 0.5 to 1.0
            materialNode.localScale = Vector3(scale, scale, scale)
            materialNode
        }
    }

    /**
     * Creates an AR node to represent a cluster of measurements.
     */
    fun createClusterNode(clusterId: Int, measurements: List<EMFADMeasurement>): CompletableFuture<Node> {
        // For clusters, we can create a larger, perhaps semi-transparent sphere
        // at the centroid of the cluster.
        val avgX = measurements.map { it.x }.average().toFloat()
        val avgY = measurements.map { it.y }.average().toFloat()
        val avgZ = measurements.map { it.z }.average().toFloat()

        val centroidPosition = Vector3(avgX, avgY, avgZ)

        return MaterialFactory.makeTransparentWithColor(context, com.google.ar.sceneform.rendering.Color(Color.argb(100, 255, 255, 0))) // Semi-transparent yellow
            .thenApply { material ->
                val clusterSphere = ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.0f, 0.0f), material)
                val node = Node()
                node.renderable = clusterSphere
                node.localPosition = centroidPosition
                node.name = "Cluster $clusterId"
                node
            }
    }
}