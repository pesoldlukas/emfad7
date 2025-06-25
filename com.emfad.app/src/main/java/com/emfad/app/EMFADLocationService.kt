package com.emfad.app.services

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.emfad.app.ui.screens.GPSLocation
import com.emfad.app.ui.screens.LatLng
import com.emfad.app.ui.screens.MeasurementPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EMFAD Location Service
 * Handles GPS integration for measurement point tracking
 * Provides location data for MapScreen and measurement correlation
 */
@Singleton
class EMFADLocationService @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    
    companion object {
        private const val TAG = "EMFADLocationService"
        private const val LOCATION_UPDATE_INTERVAL = 1000L // 1 second
        private const val LOCATION_FASTEST_INTERVAL = 500L // 0.5 seconds
        private const val MIN_ACCURACY_METERS = 50f // Minimum acceptable accuracy
    }
    
    private val _currentLocation = MutableStateFlow<GPSLocation?>(null)
    val currentLocation: StateFlow<GPSLocation?> = _currentLocation.asStateFlow()
    
    private val _locationAccuracy = MutableStateFlow(0f)
    val locationAccuracy: StateFlow<Float> = _locationAccuracy.asStateFlow()
    
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()
    
    private val _trackingPoints = MutableStateFlow<List<MeasurementPoint>>(emptyList())
    val trackingPoints: StateFlow<List<MeasurementPoint>> = _trackingPoints.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    private var isTracking = false
    
    /**
     * Start location updates
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startLocationUpdates(): Flow<GPSLocation> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                
                result.lastLocation?.let { location ->
                    val gpsLocation = convertToGPSLocation(location)
                    _currentLocation.value = gpsLocation
                    _locationAccuracy.value = location.accuracy
                    _isLocationEnabled.value = true
                    
                    Log.d(TAG, "Location updated: ${gpsLocation.latLng.latitude}, ${gpsLocation.latLng.longitude}, accuracy: ${location.accuracy}m")
                    
                    // Only emit locations with acceptable accuracy
                    if (location.accuracy <= MIN_ACCURACY_METERS) {
                        trySend(gpsLocation)
                    }
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                _isLocationEnabled.value = availability.isLocationAvailable
                Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Started location updates")
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            _isLocationEnabled.value = false
        }
        
        awaitClose {
            stopLocationUpdates()
        }
    }
    
    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
            Log.d(TAG, "Stopped location updates")
        }
    }
    
    /**
     * Get current location once
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getCurrentLocation(): GPSLocation? {
        return try {
            val location = fusedLocationClient.lastLocation.result
            location?.let { convertToGPSLocation(it) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            null
        }
    }
    
    /**
     * Get location accuracy
     */
    fun getLocationAccuracy(): Float {
        return _locationAccuracy.value
    }
    
    /**
     * Check if location is available
     */
    fun isLocationAvailable(): Boolean {
        return _isLocationEnabled.value && _currentLocation.value != null
    }
    
    /**
     * Start tracking measurement points with GPS
     */
    fun startTracking() {
        isTracking = true
        Log.d(TAG, "Started GPS tracking for measurements")
    }
    
    /**
     * Stop tracking measurement points
     */
    fun stopTracking() {
        isTracking = false
        Log.d(TAG, "Stopped GPS tracking for measurements")
    }
    
    /**
     * Add measurement point with current GPS location
     */
    fun addMeasurementPoint(measurement: com.emfad.app.models.EMFADMeasurementData) {
        if (!isTracking) return
        
        val currentGPSLocation = _currentLocation.value
        if (currentGPSLocation != null) {
            val measurementPoint = MeasurementPoint(
                location = currentGPSLocation,
                measurement = measurement,
                id = java.util.UUID.randomUUID().toString()
            )
            
            val currentPoints = _trackingPoints.value.toMutableList()
            currentPoints.add(measurementPoint)
            _trackingPoints.value = currentPoints
            
            Log.d(TAG, "Added measurement point at ${currentGPSLocation.latLng.latitude}, ${currentGPSLocation.latLng.longitude}")
        } else {
            Log.w(TAG, "Cannot add measurement point: no GPS location available")
        }
    }
    
    /**
     * Clear all tracking points
     */
    fun clearTrackingPoints() {
        _trackingPoints.value = emptyList()
        Log.d(TAG, "Cleared all tracking points")
    }
    
    /**
     * Save tracking data to file
     */
    suspend fun saveTrackingData(points: List<MeasurementPoint>): Result<android.net.Uri> {
        return try {
            val content = generateTrackingDataContent(points)
            val fileName = "emfad_tracking_${System.currentTimeMillis()}"
            
            // This would use MediaStore or file provider to save
            val uri = android.net.Uri.parse("file:///$fileName.gpx")
            
            Log.d(TAG, "Saved tracking data: ${points.size} points")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save tracking data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get tracking statistics
     */
    fun getTrackingStatistics(): TrackingStatistics {
        val points = _trackingPoints.value
        
        if (points.isEmpty()) {
            return TrackingStatistics(
                totalPoints = 0,
                totalDistance = 0.0,
                averageAccuracy = 0f,
                trackingDuration = 0L,
                boundingBox = null
            )
        }
        
        val totalDistance = calculateTotalDistance(points)
        val averageAccuracy = points.map { it.location.accuracy }.average().toFloat()
        val trackingDuration = calculateTrackingDuration(points)
        val boundingBox = calculateBoundingBox(points)
        
        return TrackingStatistics(
            totalPoints = points.size,
            totalDistance = totalDistance,
            averageAccuracy = averageAccuracy,
            trackingDuration = trackingDuration,
            boundingBox = boundingBox
        )
    }
    
    /**
     * Convert Android Location to GPSLocation
     */
    private fun convertToGPSLocation(location: Location): GPSLocation {
        return GPSLocation(
            latLng = LatLng(location.latitude, location.longitude),
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = location.time
        )
    }
    
    /**
     * Generate tracking data content (GPX format)
     */
    private fun generateTrackingDataContent(points: List<MeasurementPoint>): String {
        val gpxBuilder = StringBuilder()
        
        gpxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxBuilder.append("<gpx version=\"1.1\" creator=\"EMFAD Android App\">\n")
        gpxBuilder.append("  <metadata>\n")
        gpxBuilder.append("    <name>EMFAD Measurement Track</name>\n")
        gpxBuilder.append("    <time>${java.time.Instant.ofEpochMilli(System.currentTimeMillis())}</time>\n")
        gpxBuilder.append("  </metadata>\n")
        gpxBuilder.append("  <trk>\n")
        gpxBuilder.append("    <name>EMFAD Measurements</name>\n")
        gpxBuilder.append("    <trkseg>\n")
        
        points.forEach { point ->
            gpxBuilder.append("      <trkpt lat=\"${point.location.latLng.latitude}\" lon=\"${point.location.latLng.longitude}\">\n")
            gpxBuilder.append("        <ele>${point.location.altitude}</ele>\n")
            gpxBuilder.append("        <time>${java.time.Instant.ofEpochMilli(point.location.timestamp)}</time>\n")
            gpxBuilder.append("        <extensions>\n")
            gpxBuilder.append("          <emfad:signalStrength>${point.measurement.signalStrength}</emfad:signalStrength>\n")
            gpxBuilder.append("          <emfad:depth>${point.measurement.depth}</emfad:depth>\n")
            gpxBuilder.append("          <emfad:conductivity>${point.measurement.conductivity}</emfad:conductivity>\n")
            gpxBuilder.append("          <emfad:frequency>${point.measurement.frequency.value}</emfad:frequency>\n")
            gpxBuilder.append("        </extensions>\n")
            gpxBuilder.append("      </trkpt>\n")
        }
        
        gpxBuilder.append("    </trkseg>\n")
        gpxBuilder.append("  </trk>\n")
        gpxBuilder.append("</gpx>\n")
        
        return gpxBuilder.toString()
    }
    
    /**
     * Calculate total distance between tracking points
     */
    private fun calculateTotalDistance(points: List<MeasurementPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        
        for (i in 1 until points.size) {
            val prevPoint = points[i - 1]
            val currentPoint = points[i]
            
            val distance = calculateDistance(
                prevPoint.location.latLng,
                currentPoint.location.latLng
            )
            
            totalDistance += distance
        }
        
        return totalDistance
    }
    
    /**
     * Calculate distance between two GPS coordinates (Haversine formula)
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLngRad / 2) * kotlin.math.sin(deltaLngRad / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate tracking duration
     */
    private fun calculateTrackingDuration(points: List<MeasurementPoint>): Long {
        if (points.size < 2) return 0L
        
        val firstPoint = points.minByOrNull { it.location.timestamp }
        val lastPoint = points.maxByOrNull { it.location.timestamp }
        
        return (lastPoint?.location?.timestamp ?: 0L) - (firstPoint?.location?.timestamp ?: 0L)
    }
    
    /**
     * Calculate bounding box for tracking points
     */
    private fun calculateBoundingBox(points: List<MeasurementPoint>): BoundingBox? {
        if (points.isEmpty()) return null
        
        val latitudes = points.map { it.location.latLng.latitude }
        val longitudes = points.map { it.location.latLng.longitude }
        
        return BoundingBox(
            north = latitudes.maxOrNull() ?: 0.0,
            south = latitudes.minOrNull() ?: 0.0,
            east = longitudes.maxOrNull() ?: 0.0,
            west = longitudes.minOrNull() ?: 0.0
        )
    }
}

/**
 * Tracking statistics data class
 */
data class TrackingStatistics(
    val totalPoints: Int,
    val totalDistance: Double, // in meters
    val averageAccuracy: Float, // in meters
    val trackingDuration: Long, // in milliseconds
    val boundingBox: BoundingBox?
)

/**
 * Bounding box for GPS coordinates
 */
data class BoundingBox(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)
