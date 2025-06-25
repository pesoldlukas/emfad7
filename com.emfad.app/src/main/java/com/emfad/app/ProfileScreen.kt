package com.emfad.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.EMFADStatusBar
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.AnalysisViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToAR: () -> Unit,
    onBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val analysisResults by viewModel.materialAnalysisResult.collectAsState()
    val profileViewMode by viewModel.profileViewMode.collectAsState()
    
    var selectedVisualization by remember { mutableStateOf(ProfileVisualization.HEATMAP_2D) }
    var colorScheme by remember { mutableStateOf(ProfileColorScheme.THERMAL) }
    var showGrid by remember { mutableStateOf(true) }
    var showAnnotations by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile Analysis",
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
                    IconButton(onClick = { viewModel.exportProfile() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Map View")
                    }
                    IconButton(onClick = onNavigateToAR) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "AR View")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Controls
            ProfileControlPanel(
                selectedVisualization = selectedVisualization,
                colorScheme = colorScheme,
                showGrid = showGrid,
                showAnnotations = showAnnotations,
                onVisualizationChanged = { selectedVisualization = it },
                onColorSchemeChanged = { colorScheme = it },
                onToggleGrid = { showGrid = !showGrid },
                onToggleAnnotations = { showAnnotations = !showAnnotations },
                onGenerate3D = { viewModel.generate3DProfile() },
                onResetView = { viewModel.resetProfileView() }
            )
            
            // Profile Visualization
            currentProfile?.let { profile ->
                ProfileVisualizationCard(
                    profile = profile,
                    visualization = selectedVisualization,
                    colorScheme = colorScheme,
                    showGrid = showGrid,
                    showAnnotations = showAnnotations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
            
            // Analysis Results
            analysisResults?.let { analysis ->
                ProfileAnalysisCard(analysis = analysis)
            }
            
            // Profile Statistics
            currentProfile?.let { profile ->
                ProfileStatisticsCard(profile = profile)
            }
            
            // Export Options
            ProfileExportCard(
                onExportEGD = { viewModel.exportAsEGD() },
                onExportESD = { viewModel.exportAsESD() },
                onExportFADS = { viewModel.exportAsFADS() },
                onExportPDF = { viewModel.exportAsPDF() }
            )
        }
    }
}

enum class ProfileVisualization(val displayName: String) {
    HEATMAP_2D("2D Heatmap"),
    CONTOUR_2D("2D Contour"),
    SURFACE_3D("3D Surface"),
    VOLUME_3D("3D Volume"),
    CROSS_SECTION("Cross Section"),
    DEPTH_PROFILE("Depth Profile")
}

enum class ProfileColorScheme(val displayName: String) {
    THERMAL("Thermal"),
    RAINBOW("Rainbow"),
    GRAYSCALE("Grayscale"),
    EMFAD_CUSTOM("EMFAD Custom")
}

@Composable
private fun ProfileControlPanel(
    selectedVisualization: ProfileVisualization,
    colorScheme: ProfileColorScheme,
    showGrid: Boolean,
    showAnnotations: Boolean,
    onVisualizationChanged: (ProfileVisualization) -> Unit,
    onColorSchemeChanged: (ProfileColorScheme) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAnnotations: () -> Unit,
    onGenerate3D: () -> Unit,
    onResetView: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Profile Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visualization Type Selection
            Text(
                text = "Visualization Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProfileVisualization.values()) { visualization ->
                    FilterChip(
                        onClick = { onVisualizationChanged(visualization) },
                        label = { Text(visualization.displayName) },
                        selected = visualization == selectedVisualization
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color Scheme Selection
            Text(
                text = "Color Scheme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProfileColorScheme.values()) { scheme ->
                    FilterChip(
                        onClick = { onColorSchemeChanged(scheme) },
                        label = { Text(scheme.displayName) },
                        selected = scheme == colorScheme
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = onToggleGrid,
                    label = { Text("Grid") },
                    selected = showGrid,
                    leadingIcon = {
                        Icon(Icons.Default.GridOn, contentDescription = null)
                    }
                )
                
                FilterChip(
                    onClick = onToggleAnnotations,
                    label = { Text("Annotations") },
                    selected = showAnnotations,
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerate3D,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3D View")
                }
                
                Button(
                    onClick = onResetView,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADOrange
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ProfileVisualizationCard(
    profile: EMFADProfile,
    visualization: ProfileVisualization,
    colorScheme: ProfileColorScheme,
    showGrid: Boolean,
    showAnnotations: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${visualization.displayName} - ${profile.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${profile.measurements.size} points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Visualization Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                drawProfileVisualization(
                    profile = profile,
                    visualization = visualization,
                    colorScheme = colorScheme,
                    showGrid = showGrid,
                    showAnnotations = showAnnotations,
                    canvasSize = size
                )
            }
        }
    }
}

private fun DrawScope.drawProfileVisualization(
    profile: EMFADProfile,
    visualization: ProfileVisualization,
    colorScheme: ProfileColorScheme,
    showGrid: Boolean,
    showAnnotations: Boolean,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (profile.measurements.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - 2 * padding
    val chartHeight = canvasSize.height - 2 * padding
    
    // Extract position and signal data
    val positions = profile.measurements.map { it.position }
    val signals = profile.measurements.map { it.signalStrength }
    
    val minX = positions.minOfOrNull { it.x } ?: 0f
    val maxX = positions.maxOfOrNull { it.x } ?: 1f
    val minZ = positions.minOfOrNull { it.z } ?: 0f
    val maxZ = positions.maxOfOrNull { it.z } ?: 1f
    val minSignal = signals.minOrNull() ?: 0.0
    val maxSignal = signals.maxOrNull() ?: 1.0
    
    val xRange = maxX - minX
    val zRange = maxZ - minZ
    val signalRange = maxSignal - minSignal
    
    // Draw grid
    if (showGrid) {
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        
        // Horizontal grid lines
        for (i in 0..10) {
            val y = padding + (chartHeight * i / 10)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(canvasSize.width - padding, y),
                strokeWidth = 1f
            )
        }
        
        // Vertical grid lines
        for (i in 0..10) {
            val x = padding + (chartWidth * i / 10)
            drawLine(
                color = gridColor,
                start = Offset(x, padding),
                end = Offset(x, canvasSize.height - padding),
                strokeWidth = 1f
            )
        }
    }
    
    // Draw visualization based on type
    when (visualization) {
        ProfileVisualization.HEATMAP_2D -> {
            drawHeatmap2D(
                profile.measurements,
                minX, maxX, minZ, maxZ, minSignal, maxSignal,
                colorScheme, padding, chartWidth, chartHeight, canvasSize
            )
        }
        ProfileVisualization.CONTOUR_2D -> {
            drawContour2D(
                profile.measurements,
                minX, maxX, minZ, maxZ, minSignal, maxSignal,
                padding, chartWidth, chartHeight, canvasSize
            )
        }
        ProfileVisualization.CROSS_SECTION -> {
            drawCrossSection(
                profile.measurements,
                minX, maxX, minSignal, maxSignal,
                padding, chartWidth, chartHeight, canvasSize
            )
        }
        else -> {
            // Placeholder for 3D visualizations
            drawText("3D visualization requires OpenGL", Offset(padding, padding + 20))
        }
    }
    
    // Draw annotations
    if (showAnnotations) {
        drawAnnotations(
            profile.measurements,
            minX, maxX, minZ, maxZ,
            padding, chartWidth, chartHeight, canvasSize
        )
    }
}

private fun DrawScope.drawHeatmap2D(
    measurements: List<EMFADMeasurementData>,
    minX: Float, maxX: Float, minZ: Float, maxZ: Float,
    minSignal: Double, maxSignal: Double,
    colorScheme: ProfileColorScheme,
    padding: Float, chartWidth: Float, chartHeight: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val xRange = maxX - minX
    val zRange = maxZ - minZ
    val signalRange = maxSignal - minSignal
    
    measurements.forEach { measurement ->
        val x = padding + (chartWidth * (measurement.position.x - minX) / xRange)
        val z = padding + (chartHeight * (measurement.position.z - minZ) / zRange)
        
        val normalizedSignal = if (signalRange > 0) {
            (measurement.signalStrength - minSignal) / signalRange
        } else 0.5
        
        val color = getColorForValue(normalizedSignal, colorScheme)
        
        drawCircle(
            color = color,
            radius = 8f,
            center = Offset(x, z)
        )
    }
}

private fun DrawScope.drawContour2D(
    measurements: List<EMFADMeasurementData>,
    minX: Float, maxX: Float, minZ: Float, maxZ: Float,
    minSignal: Double, maxSignal: Double,
    padding: Float, chartWidth: Float, chartHeight: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Simplified contour drawing - connect points with similar signal strengths
    val contourLevels = listOf(0.2, 0.4, 0.6, 0.8)
    val signalRange = maxSignal - minSignal
    
    contourLevels.forEach { level ->
        val targetSignal = minSignal + (signalRange * level)
        val contourColor = getColorForValue(level, ProfileColorScheme.THERMAL)
        
        measurements.filter { 
            abs(it.signalStrength - targetSignal) < signalRange * 0.1 
        }.forEach { measurement ->
            val x = padding + (chartWidth * (measurement.position.x - minX) / (maxX - minX))
            val z = padding + (chartHeight * (measurement.position.z - minZ) / (maxZ - minZ))
            
            drawCircle(
                color = contourColor,
                radius = 4f,
                center = Offset(x, z)
            )
        }
    }
}

private fun DrawScope.drawCrossSection(
    measurements: List<EMFADMeasurementData>,
    minX: Float, maxX: Float,
    minSignal: Double, maxSignal: Double,
    padding: Float, chartWidth: Float, chartHeight: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val xRange = maxX - minX
    val signalRange = maxSignal - minSignal
    
    // Sort measurements by X position for cross-section
    val sortedMeasurements = measurements.sortedBy { it.position.x }
    
    if (sortedMeasurements.size > 1) {
        for (i in 0 until sortedMeasurements.size - 1) {
            val measurement1 = sortedMeasurements[i]
            val measurement2 = sortedMeasurements[i + 1]
            
            val x1 = padding + (chartWidth * (measurement1.position.x - minX) / xRange)
            val y1 = canvasSize.height - padding - (chartHeight * (measurement1.signalStrength - minSignal) / signalRange).toFloat()
            val x2 = padding + (chartWidth * (measurement2.position.x - minX) / xRange)
            val y2 = canvasSize.height - padding - (chartHeight * (measurement2.signalStrength - minSignal) / signalRange).toFloat()
            
            drawLine(
                color = EMFADChartPrimary,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3f
            )
        }
    }
}

private fun DrawScope.drawAnnotations(
    measurements: List<EMFADMeasurementData>,
    minX: Float, maxX: Float, minZ: Float, maxZ: Float,
    padding: Float, chartWidth: Float, chartHeight: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Find and annotate peak measurements
    val maxSignal = measurements.maxByOrNull { it.signalStrength }
    maxSignal?.let { peak ->
        val x = padding + (chartWidth * (peak.position.x - minX) / (maxX - minX))
        val z = padding + (chartHeight * (peak.position.z - minZ) / (maxZ - minZ))
        
        // Draw peak marker
        drawCircle(
            color = EMFADRed,
            radius = 12f,
            center = Offset(x, z),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

private fun getColorForValue(normalizedValue: Double, colorScheme: ProfileColorScheme): Color {
    val value = normalizedValue.coerceIn(0.0, 1.0)
    
    return when (colorScheme) {
        ProfileColorScheme.THERMAL -> {
            when {
                value < 0.25 -> Color.Blue.copy(alpha = 0.8f)
                value < 0.5 -> Color.Green.copy(alpha = 0.8f)
                value < 0.75 -> Color.Yellow.copy(alpha = 0.8f)
                else -> Color.Red.copy(alpha = 0.8f)
            }
        }
        ProfileColorScheme.RAINBOW -> {
            val hue = (value * 300).toFloat() // 0 to 300 degrees (blue to red)
            Color.hsv(hue, 1f, 1f, 0.8f)
        }
        ProfileColorScheme.GRAYSCALE -> {
            val gray = value.toFloat()
            Color(gray, gray, gray, 0.8f)
        }
        ProfileColorScheme.EMFAD_CUSTOM -> {
            when {
                value < 0.33 -> EMFADBlue.copy(alpha = 0.8f)
                value < 0.66 -> EMFADYellow.copy(alpha = 0.8f)
                else -> EMFADRed.copy(alpha = 0.8f)
            }
        }
    }
}

@Composable
private fun ProfileAnalysisCard(
    analysis: MaterialAnalysis
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Material Analysis Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalysisItem(
                    label = "Material Type",
                    value = analysis.materialType.name
                )
                AnalysisItem(
                    label = "Confidence",
                    value = "${(analysis.confidence * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
private fun ProfileStatisticsCard(
    profile: EMFADProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Profile Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val signals = profile.measurements.map { it.signalStrength }
            val depths = profile.measurements.map { it.depth }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Points", "${profile.measurements.size}")
                StatItem("Avg Signal", String.format("%.2f", signals.average()))
                StatItem("Max Depth", String.format("%.1f mm", depths.maxOrNull() ?: 0.0))
                StatItem("Duration", "${(profile.endTime - profile.startTime) / 1000}s")
            }
        }
    }
}

@Composable
private fun ProfileExportCard(
    onExportEGD: () -> Unit,
    onExportESD: () -> Unit,
    onExportFADS: () -> Unit,
    onExportPDF: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Export Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportEGD,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EMFADBlue)
                ) {
                    Text("EGD")
                }
                Button(
                    onClick = onExportESD,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EMFADBlue)
                ) {
                    Text("ESD")
                }
                Button(
                    onClick = onExportFADS,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EMFADBlue)
                ) {
                    Text("FADS")
                }
                Button(
                    onClick = onExportPDF,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EMFADGreen)
                ) {
                    Text("PDF")
                }
            }
        }
    }
}

@Composable
private fun AnalysisItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
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
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    EMFADAnalyzerTheme {
        ProfileScreen(
            onNavigateToMap = {},
            onNavigateToAR = {},
            onBack = {}
        )
    }
}
