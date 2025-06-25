package com.emfad.app.models

data class MaterialAnalysis(
    val materialType: MaterialType,
    val confidence: Float,
    val properties: Map<String, Any>? = null
)