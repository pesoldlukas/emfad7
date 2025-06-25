package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.ar.ArNodeFactory
import com.emfad.app.ar.ArScene
import com.emfad.app.models.*
import com.emfad.app.services.AnalysisService
import com.emfad.app.services.ExportService
import com.emfad.app.services.HeatmapGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analysisService: AnalysisService,
    private val arNodeFactory: ArNodeFactory,
    private val arScene: ArScene,
    private val heatmapGenerator: HeatmapGenerator,
    private val exportService: ExportService
) : ViewModel() {

    private val _currentMeasurements = MutableStateFlow<List<EMFADMeasurement>>(emptyList())
    val currentMeasurements: StateFlow<List<EMFADMeasurement>> = _currentMeasurements

    private val _materialAnalysisResult = MutableStateFlow<MaterialAnalysis?>(null)
    val materialAnalysisResult: StateFlow<MaterialAnalysis?> = _materialAnalysisResult

    private val _analysisErrorMessage = MutableStateFlow<String?>(null)
    val analysisErrorMessage: StateFlow<String?> = _analysisErrorMessage

    // Profile Management
    private val _currentProfile = MutableStateFlow<EMFADProfile?>(null)
    val currentProfile: StateFlow<EMFADProfile?> = _currentProfile.asStateFlow()

    private val _profileViewMode = MutableStateFlow("2D")
    val profileViewMode: StateFlow<String> = _profileViewMode.asStateFlow()

    fun addMeasurement(measurement: EMFADMeasurement) {
        val updatedList = _currentMeasurements.value.toMutableList().apply { add(measurement) }
        _currentMeasurements.value = updatedList
        // Trigger analysis whenever a new measurement is added
        analyzeAndVisualize(updatedList)
    }

    private fun analyzeAndVisualize(measurements: List<EMFADMeasurement>) {
        _analysisErrorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            if (measurements.isNotEmpty()) {
                try {
                    // Perform Material Analysis
                    val materialAnalysis = analysisService.analyzeMaterial(measurements)
                    _materialAnalysisResult.value = materialAnalysis

                    // Visualize Material Analysis
                    val latestMeasurement = measurements.last()
                    val materialNode = arNodeFactory.createMeasurementNode(latestMeasurement)
                    arScene.addNode(materialNode)
                    arScene.addNode(materialNode)

                    // Perform Cluster Analysis
                    val clusteredMeasurements = analysisService.analyzeClusters(measurements)
                    clusteredMeasurements.forEachIndexed { clusterId, cluster ->
                        cluster.forEach { measurement ->
                            // Update the clusterId for each measurement in the cluster
                            val updatedMeasurement = measurement.copy(clusterId = clusterId)
                            val clusterNode = arNodeFactory.createMeasurementNode(updatedMeasurement)
                            arScene.addNode(clusterNode)
                        }
                    }

                    // Perform Symmetry Analysis
                    val symmetryAnalysis = analysisService.analyzeSymmetry(measurements)
                    // TODO: Add AR visualization for symmetry analysis if needed

                    // Generate and update Heatmap
                    val heatmapGrid = heatmapGenerator.generateHeatmap(measurements)
                    arScene.updateHeatmap(heatmapGrid)
                } catch (e: Exception) {
                    Log.e("AnalysisViewModel", "Error during analysis and visualization: ${e.message}", e)
                    _analysisErrorMessage.value = "Analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            } else {
                _analysisErrorMessage.value = "No measurements to analyze."
            }
        }
    }

    fun toggleHeatmapVisibility() {
        arScene.toggleHeatmapVisibility()
    }

    // Profile Management Methods
    fun createProfile(name: String, measurements: List<EMFADMeasurementData>) {
        val profile = EMFADProfile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            measurements = measurements,
            startTime = measurements.minOfOrNull { it.timestamp } ?: System.currentTimeMillis(),
            endTime = measurements.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis(),
            scanArea = EMFADScanArea(
                startPosition = com.google.ar.sceneform.math.Vector3.zero(),
                endPosition = com.google.ar.sceneform.math.Vector3.one()
            )
        )
        _currentProfile.value = profile
    }

    fun generate3DProfile() {
        _profileViewMode.value = "3D"
        // Generate 3D visualization
    }

    fun resetProfileView() {
        _profileViewMode.value = "2D"
    }

    fun exportProfile() {
        viewModelScope.launch {
            _currentProfile.value?.let { profile ->
                exportService.exportProfile(profile)
            }
        }
    }

    fun exportAsEGD() {
        viewModelScope.launch {
            _currentProfile.value?.let { profile ->
                exportService.exportAsFormat(profile, EMFADExportFormat.EGD)
            }
        }
    }

    fun exportAsESD() {
        viewModelScope.launch {
            _currentProfile.value?.let { profile ->
                exportService.exportAsFormat(profile, EMFADExportFormat.ESD)
            }
        }
    }

    fun exportAsFADS() {
        viewModelScope.launch {
            _currentProfile.value?.let { profile ->
                exportService.exportAsFormat(profile, EMFADExportFormat.FADS)
            }
        }
    }

    fun exportAsPDF() {
        viewModelScope.launch {
            _currentProfile.value?.let { profile ->
                exportService.exportAsFormat(profile, EMFADExportFormat.PDF)
            }
        }
    }

    fun clearMeasurements() {
        _currentMeasurements.value = emptyList()
        _materialAnalysisResult.value = null
        _analysisErrorMessage.value = null
        arScene.clearNodes()
    }
}