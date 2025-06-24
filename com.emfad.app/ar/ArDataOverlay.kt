package com.emfad.app.ar

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Responsible for displaying measurement data and analysis results as overlays
 * in the Augmented Reality view.
 */
class ArDataOverlay(private val context: Context) {

    private val overlayLayout: LinearLayout
    private val trackingStateTextView: TextView
    private val measurementInfoTextView: TextView

    init {
        overlayLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(Color.argb(150, 0, 0, 0)) // Semi-transparent black background
            setPadding(16, 16, 16, 16)
        }

        trackingStateTextView = TextView(context).apply {
            text = "Tracking State: Initializing"
            textSize = 18f
            setTextColor(Color.WHITE)
        }

        measurementInfoTextView = TextView(context).apply {
            text = "Measurement Info: None"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 0)
        }

        overlayLayout.addView(trackingStateTextView)
        overlayLayout.addView(measurementInfoTextView)
    }

    /**
     * Returns the LinearLayout that serves as the overlay.
     */
    fun getOverlayView(): LinearLayout {
        return overlayLayout
    }

    /**
     * Updates the content of the overlay with new data.
     */
    fun updateOverlay(trackingState: String, measurementData: String = "") {
        trackingStateTextView.text = "Tracking State: $trackingState"
        measurementInfoTextView.text = "Measurement Info: $measurementData"
    }
}