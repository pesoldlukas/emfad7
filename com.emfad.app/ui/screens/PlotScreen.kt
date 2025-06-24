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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.EMFADStatusBar
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.MeasurementViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotScreen(
    onNavigateToSpec: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val measurementHistory by viewModel.measurementHistory.collectAsState()
    val currentMeasurement by viewModel.currentMeasurement.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    var selectedPlotType by remember { mutableStateOf(PlotType.SIGNAL_STRENGTH) }
    var timeRange by remember { mutableStateOf(TimeRange.LAST_30_SECONDS) }
    var showGrid by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Real-time Plot",
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
                    IconButton(onClick = { viewModel.exportPlotData() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onNavigateToSpec) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Spectrum")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "Profile")
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
            // Status Bar
            EMFADStatusBar(deviceStatus = deviceStatus)
            
            // Plot Controls
            PlotControlPanel(
                selectedPlotType = selectedPlotType,
                timeRange = timeRange,
                showGrid = showGrid,
                isRecording = isRecording,
                onPlotTypeChanged = { selectedPlotType = it },
                onTimeRangeChanged = { timeRange = it },
                onToggleGrid = { showGrid = !showGrid },
                onToggleRecording = { viewModel.toggleRecording() },
                onClearData = { viewModel.clearMeasurementHistory() }
            )
            
            // Real-time Chart
            RealtimeChart(
                measurements = measurementHistory,
                plotType = selectedPlotType,
                timeRange = timeRange,
                showGrid = showGrid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            
            // Current Values Display
            currentMeasurement?.let { measurement ->
                CurrentValuesCard(
                    measurement = measurement,
                    plotType = selectedPlotType
                )
            }
            
            // Statistics Card
            if (measurementHistory.isNotEmpty()) {
                StatisticsCard(
                    measurements = measurementHistory,
                    plotType = selectedPlotType
                )
            }
        }
    }
}

enum class PlotType(val displayName: String, val unit: String) {
    SIGNAL_STRENGTH("Signal Strength", "%"),
    DEPTH("Depth", "mm"),
    CONDUCTIVITY("Conductivity", "S/m"),
    TEMPERATURE("Temperature", "°C"),
    ANOMALY_DEPTH("Anomaly Depth", "mm")
}

enum class TimeRange(val displayName: String, val milliseconds: Long) {
    LAST_10_SECONDS("10s", 10_000),
    LAST_30_SECONDS("30s", 30_000),
    LAST_1_MINUTE("1m", 60_000),
    LAST_5_MINUTES("5m", 300_000),
    ALL_DATA("All", Long.MAX_VALUE)
}

@Composable
private fun PlotControlPanel(
    selectedPlotType: PlotType,
    timeRange: TimeRange,
    showGrid: Boolean,
    isRecording: Boolean,
    onPlotTypeChanged: (PlotType) -> Unit,
    onTimeRangeChanged: (TimeRange) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleRecording: () -> Unit,
    onClearData: () -> Unit
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
                text = "Plot Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Plot Type Selection
            Text(
                text = "Data Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PlotType.values()) { plotType ->
                    FilterChip(
                        onClick = { onPlotTypeChanged(plotType) },
                        label = { Text(plotType.displayName) },
                        selected = plotType == selectedPlotType
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time Range and Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range
                Column {
                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimeRange.values().forEach { range ->
                            FilterChip(
                                onClick = { onTimeRangeChanged(range) },
                                label = { Text(range.displayName) },
                                selected = range == timeRange
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) EMFADRed else EMFADGreen
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                
                Button(
                    onClick = onToggleGrid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showGrid) EMFADBlue else EMFADBlue.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grid")
                }
                
                Button(
                    onClick = onClearData,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADOrange
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun RealtimeChart(
    measurements: List<EMFADMeasurementData>,
    plotType: PlotType,
    timeRange: TimeRange,
    showGrid: Boolean,
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
                    text = "${plotType.displayName} vs Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Unit: ${plotType.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                drawChart(
                    measurements = measurements,
                    plotType = plotType,
                    timeRange = timeRange,
                    showGrid = showGrid,
                    canvasSize = size
                )
            }
        }
    }
}

private fun DrawScope.drawChart(
    measurements: List<EMFADMeasurementData>,
    plotType: PlotType,
    timeRange: TimeRange,
    showGrid: Boolean,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (measurements.isEmpty()) return
    
    val currentTime = System.currentTimeMillis()
    val filteredMeasurements = measurements.filter { 
        currentTime - it.timestamp <= timeRange.milliseconds 
    }
    
    if (filteredMeasurements.isEmpty()) return
    
    val values = filteredMeasurements.map { measurement ->
        when (plotType) {
            PlotType.SIGNAL_STRENGTH -> measurement.signalStrength * 100
            PlotType.DEPTH -> measurement.depth
            PlotType.CONDUCTIVITY -> measurement.conductivity
            PlotType.TEMPERATURE -> measurement.temperature
            PlotType.ANOMALY_DEPTH -> measurement.anomalyDepth
        }
    }
    
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 1.0
    val valueRange = maxValue - minValue
    
    val padding = 40f
    val chartWidth = canvasSize.width - 2 * padding
    val chartHeight = canvasSize.height - 2 * padding
    
    // Draw grid
    if (showGrid) {
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        
        // Horizontal grid lines
        for (i in 0..5) {
            val y = padding + (chartHeight * i / 5)
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
    
    // Draw chart line
    if (filteredMeasurements.size > 1) {
        val path = Path()
        val chartColor = EMFADChartPrimary
        
        filteredMeasurements.forEachIndexed { index, measurement ->
            val x = padding + (chartWidth * index / (filteredMeasurements.size - 1))
            val normalizedValue = if (valueRange > 0) {
                (values[index] - minValue) / valueRange
            } else 0.5
            val y = canvasSize.height - padding - (chartHeight * normalizedValue)
            
            if (index == 0) {
                path.moveTo(x, y.toFloat())
            } else {
                path.lineTo(x, y.toFloat())
            }
        }
        
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 3f)
        )
        
        // Draw data points
        filteredMeasurements.forEachIndexed { index, _ ->
            val x = padding + (chartWidth * index / (filteredMeasurements.size - 1))
            val normalizedValue = if (valueRange > 0) {
                (values[index] - minValue) / valueRange
            } else 0.5
            val y = canvasSize.height - padding - (chartHeight * normalizedValue)
            
            drawCircle(
                color = chartColor,
                radius = 4f,
                center = Offset(x, y.toFloat())
            )
        }
    }
}

@Composable
private fun CurrentValuesCard(
    measurement: EMFADMeasurementData,
    plotType: PlotType
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
                text = "Current Value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val currentValue = when (plotType) {
                PlotType.SIGNAL_STRENGTH -> "${(measurement.signalStrength * 100).toInt()}%"
                PlotType.DEPTH -> String.format("%.2f mm", measurement.depth)
                PlotType.CONDUCTIVITY -> String.format("%.3f S/m", measurement.conductivity)
                PlotType.TEMPERATURE -> String.format("%.1f°C", measurement.temperature)
                PlotType.ANOMALY_DEPTH -> String.format("%.2f mm", measurement.anomalyDepth)
            }
            
            Text(
                text = currentValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatisticsCard(
    measurements: List<EMFADMeasurementData>,
    plotType: PlotType
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
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val values = measurements.map { measurement ->
                when (plotType) {
                    PlotType.SIGNAL_STRENGTH -> measurement.signalStrength * 100
                    PlotType.DEPTH -> measurement.depth
                    PlotType.CONDUCTIVITY -> measurement.conductivity
                    PlotType.TEMPERATURE -> measurement.temperature
                    PlotType.ANOMALY_DEPTH -> measurement.anomalyDepth
                }
            }
            
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            val avg = values.average()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Min", String.format("%.2f", min), plotType.unit)
                StatItem("Avg", String.format("%.2f", avg), plotType.unit)
                StatItem("Max", String.format("%.2f", max), plotType.unit)
                StatItem("Count", "${measurements.size}", "")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String
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
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlotScreenPreview() {
    EMFADAnalyzerTheme {
        PlotScreen(
            onNavigateToSpec = {},
            onNavigateToProfile = {},
            onBack = {}
        )
    }
}
