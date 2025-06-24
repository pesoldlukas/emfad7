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
fun SetupScreen(
    onNavigateToSurvey: () -> Unit,
    onBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val measurementConfig by viewModel.measurementConfig.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Setup Configuration",
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
                    IconButton(onClick = onNavigateToSurvey) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Survey")
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
            
            // Frequency Selection (7 buttons: 19-135 kHz)
            EMFADFrequencySelector(
                selectedFrequency = measurementConfig.frequency,
                onFrequencySelected = { viewModel.updateFrequency(it) }
            )
            
            // Measurement Mode Selection (A, A-B, B, B-A)
            EMFADModeSelector(
                selectedMode = measurementConfig.mode,
                onModeSelected = { viewModel.updateMode(it) }
            )
            
            // Gain and Offset Controls
            GainOffsetControls(
                gain = measurementConfig.gain,
                offset = measurementConfig.offset,
                onGainChanged = { viewModel.updateGain(it) },
                onOffsetChanged = { viewModel.updateOffset(it) }
            )
            
            // Auto Measurement Interval
            AutoMeasurementControls(
                isAutoMode = measurementConfig.isAutoMode,
                interval = measurementConfig.autoMeasurementInterval,
                onAutoModeToggle = { viewModel.toggleAutoMode() },
                onIntervalChanged = { viewModel.updateAutoInterval(it) }
            )
            
            // Filter and Orientation Settings
            AdvancedSettings(
                filterLevel = measurementConfig.filterLevel,
                orientation = measurementConfig.orientation,
                onFilterLevelChanged = { viewModel.updateFilterLevel(it) },
                onOrientationChanged = { viewModel.updateOrientation(it) }
            )
            
            // Apply Settings Button
            Button(
                onClick = {
                    viewModel.applyConfiguration()
                    onNavigateToSurvey()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EMFADGreen
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Settings & Start Survey")
            }
        }
    }
}

@Composable
private fun GainOffsetControls(
    gain: Double,
    offset: Double,
    onGainChanged: (Double) -> Unit,
    onOffsetChanged: (Double) -> Unit
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
                text = "Gain & Offset",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gain Control
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gain",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format("%.2f", gain),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EMFADBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = gain.toFloat(),
                    onValueChange = { onGainChanged(it.toDouble()) },
                    valueRange = 0.1f..10.0f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADBlue,
                        activeTrackColor = EMFADBlue
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.1", style = MaterialTheme.typography.bodySmall)
                    Text("10.0", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Offset Control
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Offset",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format("%.3f", offset),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EMFADYellow
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = offset.toFloat(),
                    onValueChange = { onOffsetChanged(it.toDouble()) },
                    valueRange = -1.0f..1.0f,
                    steps = 200,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADYellow,
                        activeTrackColor = EMFADYellow
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-1.0", style = MaterialTheme.typography.bodySmall)
                    Text("1.0", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AutoMeasurementControls(
    isAutoMode: Boolean,
    interval: Long,
    onAutoModeToggle: () -> Unit,
    onIntervalChanged: (Long) -> Unit
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
                text = "Auto Measurement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Auto Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = isAutoMode,
                    onCheckedChange = { onAutoModeToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EMFADGreen,
                        checkedTrackColor = EMFADGreen.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (isAutoMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Interval Control
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Measurement Interval",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${interval}ms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EMFADGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = interval.toFloat(),
                        onValueChange = { onIntervalChanged(it.toLong()) },
                        valueRange = 100f..5000f,
                        steps = 49,
                        colors = SliderDefaults.colors(
                            thumbColor = EMFADGreen,
                            activeTrackColor = EMFADGreen
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100ms", style = MaterialTheme.typography.bodySmall)
                        Text("5000ms", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettings(
    filterLevel: Int,
    orientation: Double,
    onFilterLevelChanged: (Int) -> Unit,
    onOrientationChanged: (Double) -> Unit
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
                text = "Advanced Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Level
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Level",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$filterLevel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EMFADBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = filterLevel.toFloat(),
                    onValueChange = { onFilterLevelChanged(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADBlue,
                        activeTrackColor = EMFADBlue
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Low (1)", style = MaterialTheme.typography.bodySmall)
                    Text("High (5)", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Orientation
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Orientation",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${orientation.toInt()}°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EMFADYellow
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = orientation.toFloat(),
                    onValueChange = { onOrientationChanged(it.toDouble()) },
                    valueRange = 0f..360f,
                    steps = 71,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADYellow,
                        activeTrackColor = EMFADYellow
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0°", style = MaterialTheme.typography.bodySmall)
                    Text("360°", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    EMFADAnalyzerTheme {
        SetupScreen(
            onNavigateToSurvey = {},
            onBack = {}
        )
    }
}
