package com.emfad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.EMFADStatusBar
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val measurementPoints by viewModel.measurementPoints.collectAsState()
    val mapMode by viewModel.mapMode.collectAsState()
    val isGPSEnabled by viewModel.isGPSEnabled.collectAsState()
    val trackingMode by viewModel.trackingMode.collectAsState()
    
    var showMeasurementOverlay by remember { mutableStateOf(true) }
    var showHeatmapOverlay by remember { mutableStateOf(false) }
    var selectedMapType by remember { mutableStateOf(MapType.STANDARD) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "GPS Map View",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.exportGPSData() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export GPS Data")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "Profile View")
                    }
                    IconButton(onClick = { viewModel.centerOnCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center on Location")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleTracking() },
                containerColor = if (trackingMode == TrackingMode.ACTIVE) EMFADRed else EMFADGreen
            ) {
                Icon(
                    if (trackingMode == TrackingMode.ACTIVE) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Tracking"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Controls
            MapControlPanel(
                selectedMapType = selectedMapType,
                showMeasurementOverlay = showMeasurementOverlay,
                showHeatmapOverlay = showHeatmapOverlay,
                isGPSEnabled = isGPSEnabled,
                trackingMode = trackingMode,
                onMapTypeChanged = { selectedMapType = it },
                onToggleMeasurementOverlay = { showMeasurementOverlay = !showMeasurementOverlay },
                onToggleHeatmapOverlay = { showHeatmapOverlay = !showHeatmapOverlay },
                onToggleGPS = { viewModel.toggleGPS() },
                onClearTrack = { viewModel.clearTrack() }
            )
            
            // Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // OSM Map Integration (placeholder for actual map implementation)
                OSMMapView(
                    currentLocation = currentLocation,
                    measurementPoints = measurementPoints,
                    mapType = selectedMapType,
                    showMeasurementOverlay = showMeasurementOverlay,
                    showHeatmapOverlay = showHeatmapOverlay,
                    onMapClick = { latLng -> viewModel.onMapClick(latLng) },
                    modifier = Modifier.fillMaxSize()
                )
                
                // GPS Status Overlay
                GPSStatusOverlay(
                    isGPSEnabled = isGPSEnabled,
                    currentLocation = currentLocation,
                    accuracy = viewModel.locationAccuracy.collectAsState().value,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
            
            // Location Info Panel
            LocationInfoPanel(
                currentLocation = currentLocation,
                measurementCount = measurementPoints.size,
                trackingMode = trackingMode
            )
        }
    }
}

enum class MapType(val displayName: String) {
    STANDARD("Standard"),
    SATELLITE("Satellite"),
    TERRAIN("Terrain"),
    HYBRID("Hybrid")
}

enum class TrackingMode(val displayName: String) {
    INACTIVE("Inactive"),
    ACTIVE("Active"),
    PAUSED("Paused")
}

data class LatLng(val latitude: Double, val longitude: Double)

data class GPSLocation(
    val latLng: LatLng,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class MeasurementPoint(
    val location: GPSLocation,
    val measurement: EMFADMeasurementData,
    val id: String = java.util.UUID.randomUUID().toString()
)

@Composable
private fun MapControlPanel(
    selectedMapType: MapType,
    showMeasurementOverlay: Boolean,
    showHeatmapOverlay: Boolean,
    isGPSEnabled: Boolean,
    trackingMode: TrackingMode,
    onMapTypeChanged: (MapType) -> Unit,
    onToggleMeasurementOverlay: () -> Unit,
    onToggleHeatmapOverlay: () -> Unit,
    onToggleGPS: () -> Unit,
    onClearTrack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Map Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Map Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapType.values().forEach { mapType ->
                    FilterChip(
                        onClick = { onMapTypeChanged(mapType) },
                        label = { Text(mapType.displayName) },
                        selected = mapType == selectedMapType,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Overlay Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = onToggleMeasurementOverlay,
                    label = { Text("Measurements") },
                    selected = showMeasurementOverlay,
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null)
                    }
                )
                
                FilterChip(
                    onClick = onToggleHeatmapOverlay,
                    label = { Text("Heatmap") },
                    selected = showHeatmapOverlay,
                    leadingIcon = {
                        Icon(Icons.Default.Thermostat, contentDescription = null)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleGPS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGPSEnabled) EMFADGreen else EMFADRed
                    )
                ) {
                    Icon(
                        if (isGPSEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isGPSEnabled) "GPS On" else "GPS Off")
                }
                
                Button(
                    onClick = onClearTrack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADOrange
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Track")
                }
            }
        }
    }
}

@Composable
private fun OSMMapView(
    currentLocation: GPSLocation?,
    measurementPoints: List<MeasurementPoint>,
    mapType: MapType,
    showMeasurementOverlay: Boolean,
    showHeatmapOverlay: Boolean,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    // Placeholder for actual OSM map implementation
    // In a real implementation, you would use a library like osmdroid or similar
    Box(
        modifier = modifier
            .background(Color.LightGray)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = "Map",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "OSM Map View",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Map Type: ${mapType.displayName}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            currentLocation?.let { location ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lat: ${String.format("%.6f", location.latLng.latitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Lng: ${String.format("%.6f", location.latLng.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (measurementPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${measurementPoints.size} measurement points",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADBlue
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Overlays: ${if (showMeasurementOverlay) "Measurements " else ""}${if (showHeatmapOverlay) "Heatmap" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GPSStatusOverlay(
    isGPSEnabled: Boolean,
    currentLocation: GPSLocation?,
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (isGPSEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                contentDescription = "GPS Status",
                tint = if (isGPSEnabled) EMFADGreen else EMFADRed
            )
            
            Text(
                text = if (isGPSEnabled) "GPS Active" else "GPS Disabled",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            if (currentLocation != null && accuracy > 0) {
                Text(
                    text = "±${accuracy.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        accuracy < 5 -> EMFADGreen
                        accuracy < 15 -> EMFADYellow
                        else -> EMFADRed
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationInfoPanel(
    currentLocation: GPSLocation?,
    measurementCount: Int,
    trackingMode: TrackingMode
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LocationInfoItem(
                    label = "Tracking",
                    value = trackingMode.displayName,
                    color = when (trackingMode) {
                        TrackingMode.ACTIVE -> EMFADGreen
                        TrackingMode.PAUSED -> EMFADYellow
                        TrackingMode.INACTIVE -> EMFADRed
                    }
                )
                
                LocationInfoItem(
                    label = "Points",
                    value = measurementCount.toString()
                )
                
                currentLocation?.let { location ->
                    LocationInfoItem(
                        label = "Altitude",
                        value = "${location.altitude.toInt()}m"
                    )
                }
            }
            
            currentLocation?.let { location ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LocationInfoItem(
                        label = "Latitude",
                        value = String.format("%.6f°", location.latLng.latitude)
                    )
                    LocationInfoItem(
                        label = "Longitude",
                        value = String.format("%.6f°", location.latLng.longitude)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationInfoItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    EMFADAnalyzerTheme {
        MapScreen(
            onNavigateToProfile = {},
            onBack = {}
        )
    }
}
