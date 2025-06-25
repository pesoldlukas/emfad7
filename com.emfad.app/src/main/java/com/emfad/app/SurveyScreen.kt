package com.emfad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.*
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.MeasurementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(
    onNavigateToPlot: () -> Unit,
    onNavigateToSpec: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val currentMeasurement by viewModel.currentMeasurement.collectAsState()
    val isAutoMode by viewModel.isAutoMode.collectAsState()
    val measurementConfig by viewModel.measurementConfig.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Survey Mode",
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
                    IconButton(onClick = onNavigateToPlot) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Plot View")
                    }
                    IconButton(onClick = onNavigateToSpec) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Spectrum View")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "Profile View")
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
            
            // Signal Strength Indicator
            currentMeasurement?.let { measurement ->
                EMFADSignalIndicator(
                    signalStrength = measurement.signalStrength
                )
            }
            
            // Measurement Control Panel
            MeasurementControlPanel(
                isAutoMode = isAutoMode,
                measurementConfig = measurementConfig,
                onToggleAutoMode = { viewModel.toggleAutoMode() },
                onStepMeasurement = { viewModel.performStepMeasurement() },
                onStartContinuous = { viewModel.startContinuousMeasurement() },
                onStopMeasurement = { viewModel.stopMeasurement() }
            )
            
            // Current Measurement Display
            currentMeasurement?.let { measurement ->
                CurrentMeasurementCard(measurement = measurement)
            }
            
            // Quick Navigation
            QuickNavigationPanel(
                onNavigateToPlot = onNavigateToPlot,
                onNavigateToSpec = onNavigateToSpec,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun MeasurementControlPanel(
    isAutoMode: Boolean,
    measurementConfig: EMFADConfig,
    onToggleAutoMode: () -> Unit,
    onStepMeasurement: () -> Unit,
    onStartContinuous: () -> Unit,
    onStopMeasurement: () -> Unit
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
                text = "Measurement Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto/Manual Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { if (isAutoMode) onToggleAutoMode() },
                        label = { Text("Manual") },
                        selected = !isAutoMode,
                        leadingIcon = {
                            Icon(Icons.Default.TouchApp, contentDescription = null)
                        }
                    )
                    FilterChip(
                        onClick = { if (!isAutoMode) onToggleAutoMode() },
                        label = { Text("Auto") },
                        selected = isAutoMode,
                        leadingIcon = {
                            Icon(Icons.Default.AutoMode, contentDescription = null)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control Buttons
            if (isAutoMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartContinuous,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADGreen
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Auto")
                    }
                    
                    Button(
                        onClick = onStopMeasurement,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADRed
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auto interval: ${measurementConfig.autoMeasurementInterval}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = onStepMeasurement,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Step Measurement")
                }
            }
        }
    }
}

@Composable
private fun CurrentMeasurementCard(
    measurement: EMFADMeasurementData
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
                text = "Current Measurement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Measurement Values Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MeasurementValueCard(
                    label = "Signal",
                    value = "${(measurement.signalStrength * 100).toInt()}%",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MeasurementValueCard(
                    label = "Depth",
                    value = String.format("%.2f", measurement.depth),
                    unit = "mm",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MeasurementValueCard(
                    label = "Conductivity",
                    value = String.format("%.3f", measurement.conductivity),
                    unit = "S/m",
                    modifier = Modifier.weight(1f)
                )
                MeasurementValueCard(
                    label = "Temperature",
                    value = String.format("%.1f", measurement.temperature),
                    unit = "°C",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Frequency and Mode Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequency: ${measurement.frequency.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Mode: ${measurement.mode.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Material Type and Confidence
            if (measurement.materialType != MaterialType.UNKNOWN) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Material: ${measurement.materialType.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADBlue
                    )
                    Text(
                        text = "Confidence: ${(measurement.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            measurement.confidence > 0.8 -> EMFADGreen
                            measurement.confidence > 0.6 -> EMFADYellow
                            else -> EMFADRed
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementValueCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun QuickNavigationPanel(
    onNavigateToPlot: () -> Unit,
    onNavigateToSpec: () -> Unit,
    onNavigateToProfile: () -> Unit
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
                text = "Analysis Views",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToPlot,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Plot")
                }
                
                Button(
                    onClick = onNavigateToSpec,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spec")
                }
                
                Button(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SurveyScreenPreview() {
    EMFADAnalyzerTheme {
        SurveyScreen(
            onNavigateToPlot = {},
            onNavigateToSpec = {},
            onNavigateToProfile = {},
            onBack = {}
        )
    }
}
