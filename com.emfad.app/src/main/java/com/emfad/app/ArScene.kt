package com.emfad.app.ar

import android.content.Context
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.emfad.app.models.HeatmapGrid
import com.emfad.app.utils.Logger

/**
 * Manages the basic ARCore scene and its initialization.
 * This includes setting up the AR session, handling frame updates for plane detection,
 * and providing a function to add virtual objects.
 */
class ArScene(private val context: Context, private val sceneView: SceneView) {

    private var arSession: Session? = null
    private var planeDetected = false
    private val heatmapRenderer = HeatmapRenderer(context)
    private var isHeatmapVisible = false
    private var heatmapAnchorNode: AnchorNode? = null

    /**
     * Sets up the ARCore session.
     */
    fun setupSession(session: Session) {
        arSession = session
        // Configure the session if needed, e.g., light estimation, cloud anchors.
        val config = com.google.ar.core.Config(session)
        config.updateMode = com.google.ar.core.Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        session.configure(config)
        sceneView.setupSession(session)
    }

    /**
     * Handles frame updates for plane detection and other ARCore processing.
     */
    fun onUpdate(frameTime: Long) {
        val frame = arSession?.update()
        if (frame != null) {
            if (!planeDetected) {
                for (plane in frame.getUpdatedTrackables(Plane::class.java)) {
                    if (plane.trackingState == TrackingState.TRACKING) {
                        planeDetected = true
                        Logger.i("ArScene", "Plane detected!")
                        break
                    }
                }
            }
        }
    }

    /**
     * Adds a virtual object to the AR scene at a specific hit result.
     */
    fun addVirtualObject(hitResult: HitResult, renderable: ModelRenderable) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(sceneView.scene)

        val transformableNode = TransformableNode(sceneView.transformationSystem)
        transformableNode.setParent(anchorNode)
        transformableNode.renderable = renderable
        transformableNode.select()
    }

    /**
     * Handles tap gestures on the AR surface for object placement.
     */
    fun onSurfaceTap(x: Float, y: Float) {
        val frame = arSession?.update()
        if (frame != null) {
            if (frame.camera.trackingState == TrackingState.TRACKING) {
                for (hit in frame.hitTest(x, y)) {
                    val trackable = hit.trackable
                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                        // If heatmap is not yet anchored, anchor it to the first detected plane
                        if (heatmapAnchorNode == null) {
                            val anchor = hit.createAnchor()
                            heatmapAnchorNode = AnchorNode(anchor)
                            heatmapAnchorNode?.setParent(sceneView.scene)
                            Logger.i("ArScene", "Heatmap anchored to plane.")
                        }
                        // Example: Load a default model and place it
                        ModelRenderable.builder()
                            .setSource(context, com.google.ar.sceneform.R.raw.sceneform_hand_cup)
                            .build()
                            .thenAccept { renderable ->
                                addVirtualObject(hit, renderable)
                            }
                            .exceptionally { throwable ->
                                Logger.e("ArScene", "Unable to load renderable: ${throwable.message}", throwable)
                                null
                            }
                        return
                    }
                }
            }
        }
    }

    fun toggleHeatmapVisibility() {
        isHeatmapVisible = !isHeatmapVisible
        if (!isHeatmapVisible) {
            heatmapRenderer.clearHeatmap()
        }
        Logger.i("ArScene", "Heatmap visibility toggled to: $isHeatmapVisible")
    }

    fun updateHeatmap(heatmapGrid: HeatmapGrid) {
        if (isHeatmapVisible && heatmapAnchorNode != null) {
            heatmapRenderer.renderHeatmap(heatmapGrid, heatmapAnchorNode!!)
        } else if (isHeatmapVisible && heatmapAnchorNode == null) {
            Logger.w("ArScene", "Heatmap is visible but no anchor node found. Tap on a plane to anchor.")
        }
    }

    fun clearNodes() {
        sceneView.scene.children.forEach { node ->
            if (node != sceneView.scene.camera && node != sceneView.scene.sun) {
                node.setParent(null)
            }
        }
        heatmapAnchorNode = null
        heatmapRenderer.clearHeatmap()
        Logger.i("ArScene", "Cleared all nodes from the scene.")
    }

    /**
     * Returns information about the current tracking state for the AR data overlay.
     */
    fun getTrackingStateInfo(): String {
        return when (arSession?.cameraConfig?.trackingState) {
            TrackingState.TRACKING -> if (planeDetected) "Tracking and Plane Detected" else "Tracking, searching for planes"
            TrackingState.PAUSED -> "Paused"
            TrackingState.STOPPED -> "Stopped"
            else -> "Initializing"
        }
    }
}