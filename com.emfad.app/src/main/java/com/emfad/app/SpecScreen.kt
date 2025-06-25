package com.emfad.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.EMFADStatusBar
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.MeasurementViewModel
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecScreen(
    onNavigateToPlot: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val currentSpectrum by viewModel.currentSpectrum.collectAsState()
    val selectedFrequencies by viewModel.selectedFrequencies.collectAsState()
    val spectrumAnalysis by viewModel.spectrumAnalysis.collectAsState()
    
    var displayMode by remember { mutableStateOf(SpectrumDisplayMode.LINEAR) }
    var frequencyRange by remember { mutableStateOf(FrequencyRange.FULL_RANGE) }
    var showPeaks by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Frequency Spectrum",
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
                    IconButton(onClick = { viewModel.exportSpectrumData() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onNavigateToPlot) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Plot")
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
            
            // Spectrum Controls
            SpectrumControlPanel(
                displayMode = displayMode,
                frequencyRange = frequencyRange,
                showPeaks = showPeaks,
                onDisplayModeChanged = { displayMode = it },
                onFrequencyRangeChanged = { frequencyRange = it },
                onTogglePeaks = { showPeaks = !showPeaks },
                onAutoScale = { viewModel.autoScaleSpectrum() }
            )
            
            // Interactive Frequency Selection Buttons
            FrequencySelectionPanel(
                selectedFrequencies = selectedFrequencies,
                onFrequencyToggle = { viewModel.toggleFrequencySelection(it) },
                onSelectAll = { viewModel.selectAllFrequencies() },
                onClearSelection = { viewModel.clearFrequencySelection() }
            )
            
            // Spectrum Chart
            currentSpectrum?.let { spectrum ->
                SpectrumChart(
                    spectrum = spectrum,
                    displayMode = displayMode,
                    frequencyRange = frequencyRange,
                    showPeaks = showPeaks,
                    selectedFrequencies = selectedFrequencies,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )
            }
            
            // Spectrum Analysis Results
            spectrumAnalysis?.let { analysis ->
                SpectrumAnalysisCard(analysis = analysis)
            }
            
            // Peak Detection Results
            currentSpectrum?.let { spectrum ->
                PeakDetectionCard(spectrum = spectrum)
            }
        }
    }
}

enum class SpectrumDisplayMode(val displayName: String) {
    LINEAR("Linear"),
    LOGARITHMIC("Logarithmic"),
    DECIBEL("Decibel (dB)")
}

enum class FrequencyRange(val displayName: String, val minFreq: Double, val maxFreq: Double) {
    FULL_RANGE("Full Range", 19000.0, 135000.0),
    LOW_FREQ("Low (19-57 kHz)", 19000.0, 57000.0),
    MID_FREQ("Mid (57-95 kHz)", 57000.0, 95000.0),
    HIGH_FREQ("High (95-135 kHz)", 95000.0, 135000.0)
}

@Composable
private fun SpectrumControlPanel(
    displayMode: SpectrumDisplayMode,
    frequencyRange: FrequencyRange,
    showPeaks: Boolean,
    onDisplayModeChanged: (SpectrumDisplayMode) -> Unit,
    onFrequencyRangeChanged: (FrequencyRange) -> Unit,
    onTogglePeaks: () -> Unit,
    onAutoScale: () -> Unit
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
                text = "Spectrum Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display Mode Selection
            Text(
                text = "Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SpectrumDisplayMode.values()) { mode ->
                    FilterChip(
                        onClick = { onDisplayModeChanged(mode) },
                        label = { Text(mode.displayName) },
                        selected = mode == displayMode
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Frequency Range Selection
            Text(
                text = "Frequency Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FrequencyRange.values()) { range ->
                    FilterChip(
                        onClick = { onFrequencyRangeChanged(range) },
                        label = { Text(range.displayName) },
                        selected = range == frequencyRange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTogglePeaks,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showPeaks) EMFADGreen else EMFADGreen.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peaks")
                }
                
                Button(
                    onClick = onAutoScale,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Scale")
                }
            }
        }
    }
}

@Composable
private fun FrequencySelectionPanel(
    selectedFrequencies: Set<EMFADFrequency>,
    onFrequencyToggle: (EMFADFrequency) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Frequency Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAll) {
                        Text("Select All")
                    }
                    TextButton(onClick = onClearSelection) {
                        Text("Clear")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Frequency Buttons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EMFADFrequency.values()) { frequency ->
                    val isSelected = selectedFrequencies.contains(frequency)
                    val frequencyColors = listOf(
                        FrequencyBand1, FrequencyBand2, FrequencyBand3, FrequencyBand4,
                        FrequencyBand5, FrequencyBand6, FrequencyBand7
                    )
                    val colorIndex = EMFADFrequency.values().indexOf(frequency)
                    val color = frequencyColors.getOrNull(colorIndex) ?: EMFADBlue
                    
                    Card(
                        modifier = Modifier.clickable { onFrequencyToggle(frequency) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color else color.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = frequency.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) EMFADWhite else EMFADBlack
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = EMFADWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectrumChart(
    spectrum: EMFADSpectrum,
    displayMode: SpectrumDisplayMode,
    frequencyRange: FrequencyRange,
    showPeaks: Boolean,
    selectedFrequencies: Set<EMFADFrequency>,
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
                    text = "Frequency Spectrum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${frequencyRange.displayName} • ${displayMode.displayName}",
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
                drawSpectrumChart(
                    spectrum = spectrum,
                    displayMode = displayMode,
                    frequencyRange = frequencyRange,
                    showPeaks = showPeaks,
                    selectedFrequencies = selectedFrequencies,
                    canvasSize = size
                )
            }
        }
    }
}

private fun DrawScope.drawSpectrumChart(
    spectrum: EMFADSpectrum,
    displayMode: SpectrumDisplayMode,
    frequencyRange: FrequencyRange,
    showPeaks: Boolean,
    selectedFrequencies: Set<EMFADFrequency>,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (spectrum.frequencies.isEmpty() || spectrum.amplitudes.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - 2 * padding
    val chartHeight = canvasSize.height - 2 * padding
    
    // Filter data by frequency range
    val filteredData = spectrum.frequencies.zip(spectrum.amplitudes).filter { (freq, _) ->
        freq >= frequencyRange.minFreq && freq <= frequencyRange.maxFreq
    }
    
    if (filteredData.isEmpty()) return
    
    val frequencies = filteredData.map { it.first }
    val amplitudes = filteredData.map { it.second }
    
    // Transform amplitudes based on display mode
    val transformedAmplitudes = when (displayMode) {
        SpectrumDisplayMode.LINEAR -> amplitudes
        SpectrumDisplayMode.LOGARITHMIC -> amplitudes.map { log10(max(it, 0.001)) }
        SpectrumDisplayMode.DECIBEL -> amplitudes.map { 20 * log10(max(it, 0.001)) }
    }
    
    val minFreq = frequencies.minOrNull() ?: 0.0
    val maxFreq = frequencies.maxOrNull() ?: 1.0
    val minAmp = transformedAmplitudes.minOrNull() ?: 0.0
    val maxAmp = transformedAmplitudes.maxOrNull() ?: 1.0
    
    val freqRange = maxFreq - minFreq
    val ampRange = maxAmp - minAmp
    
    // Draw grid
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
    
    // Vertical grid lines (frequency markers)
    EMFADFrequency.values().forEach { freq ->
        if (freq.value >= frequencyRange.minFreq && freq.value <= frequencyRange.maxFreq) {
            val x = padding + (chartWidth * (freq.value - minFreq) / freqRange).toFloat()
            val isSelected = selectedFrequencies.contains(freq)
            
            drawLine(
                color = if (isSelected) EMFADYellow else gridColor,
                start = Offset(x, padding),
                end = Offset(x, canvasSize.height - padding),
                strokeWidth = if (isSelected) 3f else 1f
            )
        }
    }
    
    // Draw spectrum line
    if (frequencies.size > 1) {
        val spectrumColor = EMFADChartPrimary
        
        for (i in 0 until frequencies.size - 1) {
            val x1 = padding + (chartWidth * (frequencies[i] - minFreq) / freqRange).toFloat()
            val y1 = canvasSize.height - padding - (chartHeight * (transformedAmplitudes[i] - minAmp) / ampRange).toFloat()
            val x2 = padding + (chartWidth * (frequencies[i + 1] - minFreq) / freqRange).toFloat()
            val y2 = canvasSize.height - padding - (chartHeight * (transformedAmplitudes[i + 1] - minAmp) / ampRange).toFloat()
            
            drawLine(
                color = spectrumColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2f
            )
        }
        
        // Draw peak markers
        if (showPeaks) {
            val peakIndices = findPeaks(transformedAmplitudes)
            peakIndices.forEach { index ->
                val x = padding + (chartWidth * (frequencies[index] - minFreq) / freqRange).toFloat()
                val y = canvasSize.height - padding - (chartHeight * (transformedAmplitudes[index] - minAmp) / ampRange).toFloat()
                
                drawCircle(
                    color = EMFADRed,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun findPeaks(amplitudes: List<Double>): List<Int> {
    val peaks = mutableListOf<Int>()
    for (i in 1 until amplitudes.size - 1) {
        if (amplitudes[i] > amplitudes[i - 1] && amplitudes[i] > amplitudes[i + 1]) {
            peaks.add(i)
        }
    }
    return peaks.sortedByDescending { amplitudes[it] }.take(5) // Top 5 peaks
}

@Composable
private fun SpectrumAnalysisCard(
    analysis: Any // Replace with actual spectrum analysis type
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
                text = "Spectrum Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add spectrum analysis results here
            Text(
                text = "Analysis results will be displayed here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PeakDetectionCard(
    spectrum: EMFADSpectrum
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
                text = "Peak Detection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PeakInfoItem(
                    label = "Peak Frequency",
                    value = "${(spectrum.peakFrequency / 1000).toInt()} kHz"
                )
                PeakInfoItem(
                    label = "Peak Amplitude",
                    value = String.format("%.3f", spectrum.peakAmplitude)
                )
                PeakInfoItem(
                    label = "Total Power",
                    value = String.format("%.2f", spectrum.totalPower)
                )
            }
        }
    }
}

@Composable
private fun PeakInfoItem(
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
fun SpecScreenPreview() {
    EMFADAnalyzerTheme {
        SpecScreen(
            onNavigateToPlot = {},
            onNavigateToProfile = {},
            onBack = {}
        )
    }
}
