package com.emfad.app.services

import android.util.Log
import com.emfad.app.hardware.EMFADDeviceManager
import com.emfad.app.hardware.EMFADRawData
import com.emfad.app.hardware.EMFADCommand
import com.emfad.app.models.*
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

/**
 * Enhanced EMFAD Measurement Service
 * Handles real-time data acquisition from EMFAD hardware
 * Implements measurement algorithms from reverse-engineered code
 */
@Singleton
class MeasurementService @Inject constructor(
    private val deviceManager: EMFADDeviceManager,
    private val spectrumAnalyzer: SpectrumAnalyzer
) {

    companion object {
        private const val TAG = "MeasurementService"
        private const val MAX_MEASUREMENTS = 10000 // Limit memory usage
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val _measurements = MutableStateFlow<List<EMFADMeasurement>>(emptyList())
    val measurements: StateFlow<List<EMFADMeasurement>> = _measurements

    private val _currentMeasurement = MutableStateFlow<EMFADMeasurementData?>(null)
    val currentMeasurement: StateFlow<EMFADMeasurementData?> = _currentMeasurement

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _measurementConfig = MutableStateFlow(EMFADConfig())
    val measurementConfig: StateFlow<EMFADConfig> = _measurementConfig

    private var measurementJob: Job? = null

    /**
     * Start continuous measurement with given configuration
     */
    fun startMeasurement(config: EMFADConfig): Flow<EMFADMeasurementData> = flow {
        if (!deviceManager.isConnected()) {
            throw Exception("No device connected")
        }

        _measurementConfig.value = config
        _isRunning.value = true

        try {
            // Configure device
            configureDevice(config)

            // Start measurement on device
            val startCommand = EMFADCommand.StartMeasurement(config.isAutoMode, config.autoMeasurementInterval)
            deviceManager.sendCommand(startCommand).getOrThrow()

            Log.d(TAG, "Started measurement with config: $config")

            // Collect real-time data
            deviceManager.readSignalData().collect { rawData ->
                val measurementData = processRawData(rawData, config)
                _currentMeasurement.value = measurementData
                emit(measurementData)

                // Add to measurements list
                addMeasurement(convertToEMFADMeasurement(measurementData))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during measurement", e)
            _isRunning.value = false
            throw e
        }
    }

    /**
     * Perform single step measurement
     */
    suspend fun performStepMeasurement(config: EMFADConfig): EMFADMeasurementData {
        if (!deviceManager.isConnected()) {
            throw Exception("No device connected")
        }

        try {
            // Configure device
            configureDevice(config)

            // Read single measurement
            val readCommand = EMFADCommand.ReadData
            val response = deviceManager.sendCommand(readCommand).getOrThrow()

            // Parse response (this would be actual device response parsing)
            val rawData = simulateRawData(config) // Replace with actual parsing
            val measurementData = processRawData(rawData, config)

            _currentMeasurement.value = measurementData
            addMeasurement(convertToEMFADMeasurement(measurementData))

            Log.d(TAG, "Performed step measurement: $measurementData")
            return measurementData

        } catch (e: Exception) {
            Log.e(TAG, "Error during step measurement", e)
            throw e
        }
    }

    /**
     * Stop measurement
     */
    suspend fun stopMeasurement() {
        try {
            if (deviceManager.isConnected()) {
                val stopCommand = EMFADCommand.StopMeasurement
                deviceManager.sendCommand(stopCommand)
            }

            measurementJob?.cancel()
            _isRunning.value = false

            Log.d(TAG, "Stopped measurement")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping measurement", e)
        }
    }

    /**
     * Calibrate device
     */
    suspend fun calibrateDevice(): Result<CalibrationData> {
        if (!deviceManager.isConnected()) {
            return Result.failure(Exception("No device connected"))
        }

        return try {
            val calibrateCommand = EMFADCommand.Calibrate
            val response = deviceManager.sendCommand(calibrateCommand).getOrThrow()

            // Parse calibration response
            val calibrationData = CalibrationData(
                timestamp = System.currentTimeMillis(),
                baselineSignal = 0.0, // Parse from response
                temperatureOffset = 0.0, // Parse from response
                gainCorrection = 1.0, // Parse from response
                isValid = true
            )

            Log.d(TAG, "Device calibrated successfully")
            Result.success(calibrationData)

        } catch (e: Exception) {
            Log.e(TAG, "Calibration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Process raw data from device into measurement data
     * Implements algorithms from reverse-engineered code
     */
    private fun processRawData(rawData: EMFADRawData, config: EMFADConfig): EMFADMeasurementData {
        // Calculate depth using algorithm from HzEMSoftexe.c
        val depth = calculateAnomalyDepth(rawData.signalStrength, rawData.frequency)

        // Calculate conductivity based on signal strength and frequency
        val conductivity = calculateConductivity(rawData.signalStrength, rawData.frequency, config.mode)

        // Analyze spectrum if available
        val spectrum = spectrumAnalyzer.analyzeSignal(rawData.signalStrength, rawData.frequency)

        return EMFADMeasurementData(
            timestamp = rawData.timestamp,
            position = Vector3.zero(), // Would be set by GPS/positioning system
            frequency = config.frequency,
            mode = config.mode,
            signalStrength = rawData.signalStrength,
            depth = depth,
            conductivity = conductivity,
            temperature = rawData.temperature,
            rawSpectrum = listOf(rawData.signalStrength),
            filteredSpectrum = listOf(rawData.signalStrength), // Apply filtering
            materialType = MaterialType.UNKNOWN, // Would be determined by AI analysis
            confidence = 0.8,
            anomalyDepth = depth,
            clusterId = -1,
            symmetryScore = 0.0,
            metadata = mapOf(
                "gain" to config.gain,
                "offset" to config.offset,
                "filterLevel" to config.filterLevel,
                "phase" to rawData.phase
            )
        )
    }

    /**
     * Calculate anomaly depth using algorithm from reverse-engineered code
     * Based on: depth = -ln(signal / 1000.0) / 0.417
     */
    private fun calculateAnomalyDepth(signalStrength: Double, frequency: Double): Double {
        return if (signalStrength > 0) {
            -ln(signalStrength / 1000.0) / 0.417
        } else {
            0.0
        }
    }

    /**
     * Calculate conductivity based on signal and frequency
     */
    private fun calculateConductivity(signalStrength: Double, frequency: Double, mode: EMFADMode): Double {
        // Simplified conductivity calculation
        // Real implementation would use complex algorithms from original software
        val baseValue = signalStrength / frequency * 1000.0

        return when (mode) {
            EMFADMode.MODE_A -> baseValue
            EMFADMode.MODE_B -> baseValue * 0.8
            EMFADMode.MODE_A_MINUS_B -> baseValue * 1.2
            EMFADMode.MODE_B_MINUS_A -> baseValue * 0.9
        }
    }

    /**
     * Configure device with measurement parameters
     */
    private suspend fun configureDevice(config: EMFADConfig) {
        // Send configuration commands to device
        deviceManager.sendCommand(EMFADCommand.SetFrequency(config.frequency))
        deviceManager.sendCommand(EMFADCommand.SetMode(config.mode))
        deviceManager.sendCommand(EMFADCommand.SetGain(config.gain))
        deviceManager.sendCommand(EMFADCommand.SetOffset(config.offset))
    }

    /**
     * Convert EMFADMeasurementData to EMFADMeasurement for database storage
     */
    private fun convertToEMFADMeasurement(data: EMFADMeasurementData): EMFADMeasurement {
        return EMFADMeasurement(
            timestamp = data.timestamp,
            x = data.position.x,
            y = data.position.y,
            z = data.position.z,
            conductivity = data.conductivity.toFloat(),
            materialType = data.materialType,
            confidence = data.confidence.toFloat(),
            clusterId = data.clusterId,
            signalStrength = data.signalStrength,
            depth = data.depth,
            temperature = data.temperature
        )
    }

    private fun addMeasurement(measurement: EMFADMeasurement) {
        val currentList = _measurements.value.toMutableList()
        currentList.add(measurement)

        // Limit list size to prevent memory issues
        if (currentList.size > MAX_MEASUREMENTS) {
            currentList.removeAt(0)
        }

        _measurements.value = currentList
        Log.d(TAG, "Added measurement: $measurement")
    }

    fun processRawData(rawData: ByteArray) {
        // Legacy method for compatibility
        // Convert ByteArray to EMFADRawData and process
        val simulatedRawData = EMFADRawData(
            timestamp = System.currentTimeMillis(),
            signalStrength = rawData.getOrNull(0)?.toDouble() ?: 0.0,
            phase = rawData.getOrNull(1)?.toDouble() ?: 0.0,
            frequency = _measurementConfig.value.frequency.value,
            temperature = 23.0,
            isValid = true
        )

        val measurementData = processRawData(simulatedRawData, _measurementConfig.value)
        _currentMeasurement.value = measurementData
        addMeasurement(convertToEMFADMeasurement(measurementData))
    }

    fun clearMeasurements() {
        _measurements.value = emptyList()
        Log.d(TAG, "Cleared all measurements.")
    }

    /**
     * Simulate raw data for testing (remove when real hardware is connected)
     */
    private fun simulateRawData(config: EMFADConfig): EMFADRawData {
        return EMFADRawData(
            timestamp = System.currentTimeMillis(),
            signalStrength = kotlin.random.Random.nextDouble(0.1, 1.0),
            phase = kotlin.random.Random.nextDouble(0.0, 360.0),
            frequency = config.frequency.value,
            temperature = kotlin.random.Random.nextDouble(20.0, 30.0),
            isValid = true
        )
    }
}

/**
 * Calibration data structure
 */
data class CalibrationData(
    val timestamp: Long,
    val baselineSignal: Double,
    val temperatureOffset: Double,
    val gainCorrection: Double,
    val isValid: Boolean
)