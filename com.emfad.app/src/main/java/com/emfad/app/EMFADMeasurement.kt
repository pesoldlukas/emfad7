package com.emfad.app.models

import com.google.ar.sceneform.math.Vector3

data class EMFADMeasurement(
    val timestamp: Long,
    val position: Vector3,
    val conductivity: Float,
    val materialType: MaterialType = MaterialType.UNKNOWN, // Added materialType
    val confidence: Float = 1.0f,
    val clusterId: Int = -1
)