package com.emfad.app.ar

import android.content.Context
import android.widget.TextView
import com.emfad.app.models.MaterialType
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import java.util.concurrent.CompletableFuture

class MaterialNode(
    private val context: Context,
    val materialType: MaterialType,
    val position: Vector3,
    val confidence: Float,
    val clusterId: Int = -1,
    val label: String = ""
) : Node() {

    private var labelRenderable: ViewRenderable? = null

    init {
        // Create a simple sphere for visualization
        MaterialFactory.makeOpaqueWithColor(context, Color(getColorForMaterial(materialType)))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.0f, 0.0f), material)
                this.renderable = sphere
            }

        // Set position
        this.localPosition = position

        // Set scale based on confidence
        val scale = 0.05f * (1.0f + confidence) // Example scaling
        this.localScale = Vector3(scale, scale, scale)

        // Create and attach label
        createLabelRenderable(label).thenAccept { renderable ->
            labelRenderable = renderable
            val labelNode = Node()
            labelNode.setParent(this)
            labelNode.localPosition = Vector3(0.0f, 0.1f, 0.0f) // Offset above the sphere
            labelNode.renderable = labelRenderable
        }
    }

    fun updateVisuals(newMaterialType: MaterialType, newConfidence: Float) {
        // Update color based on new material type
        MaterialFactory.makeOpaqueWithColor(context, Color(getColorForMaterial(newMaterialType)))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.0f, 0.0f), material)
                this.renderable = sphere
            }

        // Update size based on confidence
        val scale = 0.05f * (1.0f + newConfidence) // Example scaling
        this.localScale = Vector3(scale, scale, scale)
    }

    fun updateLabel(newLabel: String) {
        // Recreate or update the label renderable
        createLabelRenderable(newLabel).thenAccept { renderable ->
            labelRenderable = renderable
            // Assuming the label node is already a child, just update its renderable
            this.children.find { it.renderable == null }?.renderable = labelRenderable // This is a simplified way, might need more robust identification
        }
    }

    private fun getColorForMaterial(materialType: MaterialType): Color {
        return when (materialType) {
            MaterialType.FERROUS_METAL -> Color(1.0f, 0.0f, 0.0f, 1.0f) // Red
            MaterialType.NON_FERROUS_METAL -> Color(0.0f, 1.0f, 0.0f, 1.0f) // Green
            MaterialType.CAVITY -> Color(0.0f, 0.0f, 1.0f, 0.5f) // Blue, transparent
            MaterialType.CRYSTAL -> Color(0.0f, 1.0f, 1.0f, 0.3f) // Cyan, highly transparent
            MaterialType.WATER -> Color(0.0f, 0.5f, 1.0f, 0.7f) // Light Blue, semi-transparent
            MaterialType.UNKNOWN -> Color(0.5f, 0.5f, 0.5f, 1.0f) // Gray
        }
    }

    private fun createLabelRenderable(text: String): CompletableFuture<ViewRenderable> {
        return ViewRenderable.builder()
            .setView(context, TextView(context).apply {
                setText(text)
                setTextColor(android.graphics.Color.WHITE)
                setTextSize(10f)
            })
            .build()
    }
}