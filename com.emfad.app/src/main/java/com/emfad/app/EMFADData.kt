package com.emfad.app.models

import androidx.compose.runtime.Stable
import com.google.ar.sceneform.math.Vector3

// EMFAD Frequency bands (19-135 kHz in 7 steps)
enum class EMFADFrequency(val value: Double, val displayName: String) {
    FREQ_19KHZ(19000.0, "19 kHz"),
    FREQ_38KHZ(38000.0, "38 kHz"),
    FREQ_57KHZ(57000.0, "57 kHz"),
    FREQ_76KHZ(76000.0, "76 kHz"),
    FREQ_95KHZ(95000.0, "95 kHz"),
    FREQ_114KHZ(114000.0, "114 kHz"),
    FREQ_135KHZ(135000.0, "135 kHz");
    
    companion object {
        fun fromValue(value: Double): EMFADFrequency {
            return values().minByOrNull { kotlin.math.abs(it.value - value) } ?: FREQ_19KHZ
        }
    }
}

// EMFAD Measurement modes (A, A-B, B, B-A)
enum class EMFADMode(val displayName: String, val description: String) {
    MODE_A("A", "Single channel A measurement"),
    MODE_A_MINUS_B("A-B", "Differential A minus B"),
    MODE_B("B", "Single channel B measurement"),
    MODE_B_MINUS_A("B-A", "Differential B minus A");
}

// EMFAD Device connection types
enum class EMFADConnectionType(val displayName: String) {
    USB("USB"),
    BLUETOOTH("Bluetooth"),
    WIFI("WiFi"),
    DISCONNECTED("Disconnected");
}

// EMFAD Device status
@Stable
data class EMFADDeviceStatus(
    val isConnected: Boolean = false,
    val connectionType: EMFADConnectionType = EMFADConnectionType.DISCONNECTED,
    val deviceName: String = "",
    val batteryLevel: Int = 0,
    val signalStrength: Int = 0,
    val temperature: Double = 0.0,
    val lastUpdate: Long = System.currentTimeMillis()
)

// EMFAD Measurement configuration
@Stable
data class EMFADConfig(
    val frequency: EMFADFrequency = EMFADFrequency.FREQ_19KHZ,
    val mode: EMFADMode = EMFADMode.MODE_A,
    val gain: Double = 1.0,
    val offset: Double = 0.0,
    val autoMeasurementInterval: Long = 1000L, // ms
    val isAutoMode: Boolean = false,
    val filterLevel: Int = 3, // 1-5 scale
    val orientation: Double = 0.0 // degrees
)

// Enhanced EMFAD Measurement data structure
@Stable
data class EMFADMeasurementData(
    val timestamp: Long = System.currentTimeMillis(),
    val position: Vector3 = Vector3.zero(),
    val frequency: EMFADFrequency = EMFADFrequency.FREQ_19KHZ,
    val mode: EMFADMode = EMFADMode.MODE_A,
    val signalStrength: Double = 0.0,
    val depth: Double = 0.0,
    val conductivity: Double = 0.0,
    val temperature: Double = 0.0,
    val rawSpectrum: List<Double> = emptyList(),
    val filteredSpectrum: List<Double> = emptyList(),
    val materialType: MaterialType = MaterialType.UNKNOWN,
    val confidence: Double = 0.0,
    val anomalyDepth: Double = 0.0, // From calculateAnomalyDepth function
    val clusterId: Int = -1,
    val symmetryScore: Double = 0.0,
    val metadata: Map<String, Any> = emptyMap()
)

// EMFAD Spectrum data for frequency analysis
@Stable
data class EMFADSpectrum(
    val frequencies: List<Double>,
    val amplitudes: List<Double>,
    val phases: List<Double> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val peakFrequency: Double = 0.0,
    val peakAmplitude: Double = 0.0,
    val totalPower: Double = 0.0
)

// EMFAD Profile data for 2D/3D visualization
@Stable
data class EMFADProfile(
    val id: String,
    val name: String,
    val measurements: List<EMFADMeasurementData>,
    val startTime: Long,
    val endTime: Long,
    val scanArea: EMFADScanArea,
    val analysisResults: MaterialAnalysis? = null,
    val exportFormats: List<String> = listOf("EGD", "ESD", "FADS", "CSV", "PDF")
)

// EMFAD Scan area definition
@Stable
data class EMFADScanArea(
    val startPosition: Vector3,
    val endPosition: Vector3,
    val gridResolution: Double = 0.1, // meters
    val scanPattern: EMFADScanPattern = EMFADScanPattern.GRID
)

// EMFAD Scan patterns
enum class EMFADScanPattern(val displayName: String) {
    GRID("Grid Pattern"),
    LINE("Line Scan"),
    CIRCLE("Circular Scan"),
    CUSTOM("Custom Pattern");
}

// EMFAD Export data formats (.EGD, .ESD, .FADS)
@Stable
data class EMFADExportData(
    val format: EMFADExportFormat,
    val profile: EMFADProfile,
    val fileName: String,
    val filePath: String,
    val exportTime: Long = System.currentTimeMillis()
)

enum class EMFADExportFormat(val extension: String, val displayName: String) {
    EGD(".egd", "EMFAD Grid Data"),
    ESD(".esd", "EMFAD Spectrum Data"),
    FADS(".fads", "EMFAD Analysis Data Set"),
    CSV(".csv", "Comma Separated Values"),
    PDF(".pdf", "Portable Document Format"),
    MATLAB(".mat", "MATLAB Data File");
}
