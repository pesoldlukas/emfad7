package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.models.*
import com.emfad.app.services.DatabaseService
import com.emfad.app.services.ExportService
import com.emfad.app.ui.screens.GPSLocation
import com.emfad.app.ui.screens.LatLng
import com.emfad.app.ui.screens.MeasurementPoint
import com.emfad.app.ui.screens.TrackingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val databaseService: DatabaseService,
    private val exportService: ExportService
) : ViewModel() {

    private val _currentLocation = MutableStateFlow<GPSLocation?>(null)
    val currentLocation: StateFlow<GPSLocation?> = _currentLocation.asStateFlow()

    private val _measurementPoints = MutableStateFlow<List<MeasurementPoint>>(emptyList())
    val measurementPoints: StateFlow<List<MeasurementPoint>> = _measurementPoints.asStateFlow()

    private val _mapMode = MutableStateFlow("Standard")
    val mapMode: StateFlow<String> = _mapMode.asStateFlow()

    private val _isGPSEnabled = MutableStateFlow(false)
    val isGPSEnabled: StateFlow<Boolean> = _isGPSEnabled.asStateFlow()

    private val _trackingMode = MutableStateFlow(TrackingMode.INACTIVE)
    val trackingMode: StateFlow<TrackingMode> = _trackingMode.asStateFlow()

    private val _locationAccuracy = MutableStateFlow(0f)
    val locationAccuracy: StateFlow<Float> = _locationAccuracy.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        loadMeasurementPoints()
        initializeGPS()
    }

    private fun loadMeasurementPoints() {
        viewModelScope.launch {
            try {
                // Load measurement points from database
                val measurements = databaseService.getAllMeasurements()
                val points = measurements.mapNotNull { measurement ->
                    // Convert database measurements to measurement points
                    // This would require GPS coordinates to be stored in the measurement
                    createMeasurementPoint(measurement)
                }
                _measurementPoints.value = points
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun createMeasurementPoint(measurement: EMFADMeasurement): MeasurementPoint? {
        // Convert EMFADMeasurement to MeasurementPoint
        // This is a placeholder - actual implementation would depend on how GPS data is stored
        return try {
            val gpsLocation = GPSLocation(
                latLng = LatLng(0.0, 0.0), // Extract from measurement if available
                altitude = 0.0,
                accuracy = 5f,
                timestamp = measurement.timestamp
            )
            
            val measurementData = EMFADMeasurementData(
                timestamp = measurement.timestamp,
                frequency = EMFADFrequency.FREQ_19KHZ, // Extract from measurement
                signalStrength = measurement.signalStrength,
                depth = measurement.depth,
                conductivity = measurement.conductivity,
                temperature = measurement.temperature,
                materialType = measurement.materialType
            )
            
            MeasurementPoint(
                location = gpsLocation,
                measurement = measurementData
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun initializeGPS() {
        // Initialize GPS services
        // This would integrate with Android's LocationManager
        _isGPSEnabled.value = true // Placeholder
    }

    fun toggleGPS() {
        _isGPSEnabled.value = !_isGPSEnabled.value
        if (_isGPSEnabled.value) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        // Start receiving GPS location updates
        // This would use Android's FusedLocationProviderClient
        viewModelScope.launch {
            // Simulate location updates
            simulateLocationUpdate()
        }
    }

    private fun stopLocationUpdates() {
        // Stop GPS location updates
    }

    private fun simulateLocationUpdate() {
        // Placeholder for actual GPS implementation
        val location = GPSLocation(
            latLng = LatLng(47.6062, -122.3321), // Seattle coordinates as example
            altitude = 56.0,
            accuracy = 3.5f,
            timestamp = System.currentTimeMillis()
        )
        _currentLocation.value = location
        _locationAccuracy.value = location.accuracy
    }

    fun toggleTracking() {
        _trackingMode.value = when (_trackingMode.value) {
            TrackingMode.INACTIVE -> TrackingMode.ACTIVE
            TrackingMode.ACTIVE -> TrackingMode.PAUSED
            TrackingMode.PAUSED -> TrackingMode.ACTIVE
        }
        
        if (_trackingMode.value == TrackingMode.ACTIVE) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    private fun startTracking() {
        // Start continuous GPS tracking and measurement recording
        viewModelScope.launch {
            // Implementation would continuously record GPS + measurement data
        }
    }

    private fun stopTracking() {
        // Stop tracking
    }

    fun centerOnCurrentLocation() {
        // Center map view on current GPS location
        _currentLocation.value?.let { location ->
            // This would trigger map view to center on the location
        }
    }

    fun onMapClick(latLng: LatLng) {
        // Handle map click events
        // Could be used to add manual measurement points
        viewModelScope.launch {
            // Create a manual measurement point at clicked location
            val gpsLocation = GPSLocation(
                latLng = latLng,
                altitude = 0.0,
                accuracy = 0f,
                timestamp = System.currentTimeMillis()
            )
            
            // Add to measurement points if needed
        }
    }

    fun clearTrack() {
        viewModelScope.launch {
            _measurementPoints.value = emptyList()
            // Clear from database if needed
            databaseService.clearAllMeasurements()
        }
    }

    fun exportGPSData() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val points = _measurementPoints.value
                if (points.isNotEmpty()) {
                    // Export GPS data with measurements
                    val exportData = createGPSExportData(points)
                    exportService.exportGPSData(exportData)
                }
            } catch (e: Exception) {
                // Handle export error
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun createGPSExportData(points: List<MeasurementPoint>): Map<String, Any> {
        return mapOf(
            "points" to points.map { point ->
                mapOf(
                    "latitude" to point.location.latLng.latitude,
                    "longitude" to point.location.latLng.longitude,
                    "altitude" to point.location.altitude,
                    "accuracy" to point.location.accuracy,
                    "timestamp" to point.location.timestamp,
                    "signalStrength" to point.measurement.signalStrength,
                    "depth" to point.measurement.depth,
                    "conductivity" to point.measurement.conductivity,
                    "temperature" to point.measurement.temperature,
                    "materialType" to point.measurement.materialType.name
                )
            },
            "exportTime" to System.currentTimeMillis(),
            "totalPoints" to points.size
        )
    }

    fun addMeasurementPoint(measurement: EMFADMeasurementData) {
        _currentLocation.value?.let { location ->
            val measurementPoint = MeasurementPoint(
                location = location,
                measurement = measurement
            )
            
            val currentPoints = _measurementPoints.value.toMutableList()
            currentPoints.add(measurementPoint)
            _measurementPoints.value = currentPoints
            
            // Save to database
            viewModelScope.launch {
                saveMeasurementPoint(measurementPoint)
            }
        }
    }

    private suspend fun saveMeasurementPoint(point: MeasurementPoint) {
        try {
            // Convert to database entity and save
            val measurement = EMFADMeasurement(
                timestamp = point.measurement.timestamp,
                position = point.measurement.position,
                conductivity = point.measurement.conductivity.toFloat(),
                materialType = point.measurement.materialType,
                confidence = point.measurement.confidence.toFloat(),
                clusterId = point.measurement.clusterId
            )
            databaseService.saveMeasurement(measurement)
        } catch (e: Exception) {
            // Handle save error
        }
    }

    fun updateMapMode(mode: String) {
        _mapMode.value = mode
    }

    fun getLocationAccuracy(): Float {
        return _locationAccuracy.value
    }

    fun isLocationAvailable(): Boolean {
        return _currentLocation.value != null && _isGPSEnabled.value
    }

    fun getTrackingStatistics(): Map<String, Any> {
        val points = _measurementPoints.value
        return mapOf(
            "totalPoints" to points.size,
            "trackingDuration" to calculateTrackingDuration(points),
            "averageAccuracy" to calculateAverageAccuracy(points),
            "coverageArea" to calculateCoverageArea(points)
        )
    }

    private fun calculateTrackingDuration(points: List<MeasurementPoint>): Long {
        if (points.size < 2) return 0L
        val firstPoint = points.minByOrNull { it.location.timestamp }
        val lastPoint = points.maxByOrNull { it.location.timestamp }
        return (lastPoint?.location?.timestamp ?: 0L) - (firstPoint?.location?.timestamp ?: 0L)
    }

    private fun calculateAverageAccuracy(points: List<MeasurementPoint>): Float {
        if (points.isEmpty()) return 0f
        return points.map { it.location.accuracy }.average().toFloat()
    }

    private fun calculateCoverageArea(points: List<MeasurementPoint>): Double {
        if (points.size < 3) return 0.0
        
        // Simple bounding box area calculation
        val latitudes = points.map { it.location.latLng.latitude }
        val longitudes = points.map { it.location.latLng.longitude }
        
        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLng = longitudes.minOrNull() ?: 0.0
        val maxLng = longitudes.maxOrNull() ?: 0.0
        
        // Approximate area in square meters (simplified calculation)
        val latDiff = maxLat - minLat
        val lngDiff = maxLng - minLng
        
        return latDiff * lngDiff * 111000 * 111000 // Rough conversion to square meters
    }
}
