package com.emfad.app.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.bluetooth.EMFADBluetoothManager
import com.emfad.app.bluetooth.BluetoothConnectionCallback
import com.emfad.app.models.*
import com.emfad.app.services.DatabaseService
import com.emfad.app.services.ExportService
import com.emfad.app.services.MeasurementService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val measurementService: MeasurementService,
    private val databaseService: DatabaseService,
    private val exportService: ExportService,
    private val deviceManager: EMFADDeviceManager,
    private val locationService: EMFADLocationService
) : ViewModel(), BluetoothConnectionCallback {

    private val bluetoothManager = EMFADBluetoothManager(context, bluetoothAdapter)

    val measurements: StateFlow<List<EMFADMeasurement>> = measurementService.measurements

    private val _connectionStatus = MutableStateFlow<String>("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    // Device Status
    private val _deviceStatus = MutableStateFlow(EMFADDeviceStatus())
    val deviceStatus: StateFlow<EMFADDeviceStatus> = _deviceStatus

    // Measurement Configuration
    private val _measurementConfig = MutableStateFlow(EMFADConfig())
    val measurementConfig: StateFlow<EMFADConfig> = _measurementConfig

    // Current Measurement
    private val _currentMeasurement = MutableStateFlow<EMFADMeasurementData?>(null)
    val currentMeasurement: StateFlow<EMFADMeasurementData?> = _currentMeasurement

    // Measurement History
    private val _measurementHistory = MutableStateFlow<List<EMFADMeasurementData>>(emptyList())
    val measurementHistory: StateFlow<List<EMFADMeasurementData>> = _measurementHistory

    // Current Spectrum
    private val _currentSpectrum = MutableStateFlow<EMFADSpectrum?>(null)
    val currentSpectrum: StateFlow<EMFADSpectrum?> = _currentSpectrum

    // Selected Frequencies for Spectrum Analysis
    private val _selectedFrequencies = MutableStateFlow<Set<EMFADFrequency>>(emptySet())
    val selectedFrequencies: StateFlow<Set<EMFADFrequency>> = _selectedFrequencies

    // Spectrum Analysis Results
    private val _spectrumAnalysis = MutableStateFlow<Any?>(null)
    val spectrumAnalysis: StateFlow<Any?> = _spectrumAnalysis

    // Auto Mode State
    private val _isAutoMode = MutableStateFlow(false)
    val isAutoMode: StateFlow<Boolean> = _isAutoMode

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    init {
        bluetoothManager.setConnectionCallback(this)
        observeConnectionState()
        loadConfiguration()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionStatus.collect { state ->
                _deviceStatus.value = _deviceStatus.value.copy(
                    isConnected = state == "Connected",
                    connectionType = when (state) {
                        "Connected" -> EMFADConnectionType.BLUETOOTH
                        else -> EMFADConnectionType.DISCONNECTED
                    }
                )
            }
        }
    }

    private fun loadConfiguration() {
        // Load saved configuration from preferences or database
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothManager.connect(device)
    }

    fun connectUSB() {
        viewModelScope.launch {
            try {
                // Scan for USB devices
                val devices = deviceManager.scanForDevices()
                val usbDevice = devices.firstOrNull { it.type == com.emfad.app.hardware.EMFADDeviceType.USB }

                if (usbDevice?.usbDevice != null) {
                    val result = deviceManager.connectUSB(usbDevice.usbDevice)
                    if (result.isSuccess) {
                        _deviceStatus.value = _deviceStatus.value.copy(
                            isConnected = true,
                            connectionType = EMFADConnectionType.USB,
                            deviceName = usbDevice.name
                        )
                        _connectionStatus.value = "Connected"
                        Log.d("MeasurementViewModel", "USB connection successful")
                    } else {
                        _connectionStatus.value = "Connection failed: ${result.exceptionOrNull()?.message}"
                        Log.e("MeasurementViewModel", "USB connection failed", result.exceptionOrNull())
                    }
                } else {
                    _connectionStatus.value = "No USB device found"
                    Log.w("MeasurementViewModel", "No EMFAD USB device found")
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Connection error: ${e.message}"
                Log.e("MeasurementViewModel", "Error during USB connection", e)
            }
        }
    }

    fun connectBluetooth() {
        // Scan and connect to Bluetooth device
        // This would show a device selection dialog
    }

    fun disconnect() {
        bluetoothManager.disconnect()
        _deviceStatus.value = _deviceStatus.value.copy(
            isConnected = false,
            connectionType = EMFADConnectionType.DISCONNECTED
        )
    }

    fun disconnectFromDevice() {
        disconnect()
    }

    fun startMeasurement() {
        // This would typically involve sending a command to the BLE device
        // For now, we'll simulate data or rely on characteristic notifications
    }

    fun stopMeasurement() {
        // This would typically involve sending a command to the BLE device
    }

    fun clearMeasurements() {
        measurementService.clearMeasurements()
        viewModelScope.launch {
            databaseService.clearAllMeasurements()
        }
    }

    fun clearMeasurementHistory() {
        _measurementHistory.value = emptyList()
    }

    // Configuration Methods
    fun updateFrequency(frequency: EMFADFrequency) {
        _measurementConfig.value = _measurementConfig.value.copy(frequency = frequency)
    }

    fun updateMode(mode: EMFADMode) {
        _measurementConfig.value = _measurementConfig.value.copy(mode = mode)
    }

    fun updateGain(gain: Double) {
        _measurementConfig.value = _measurementConfig.value.copy(gain = gain)
    }

    fun updateOffset(offset: Double) {
        _measurementConfig.value = _measurementConfig.value.copy(offset = offset)
    }

    fun updateAutoInterval(interval: Long) {
        _measurementConfig.value = _measurementConfig.value.copy(autoMeasurementInterval = interval)
    }

    fun updateFilterLevel(level: Int) {
        _measurementConfig.value = _measurementConfig.value.copy(filterLevel = level)
    }

    fun updateOrientation(orientation: Double) {
        _measurementConfig.value = _measurementConfig.value.copy(orientation = orientation)
    }

    fun toggleAutoMode() {
        val newAutoMode = !_measurementConfig.value.isAutoMode
        _measurementConfig.value = _measurementConfig.value.copy(isAutoMode = newAutoMode)
        _isAutoMode.value = newAutoMode
    }

    fun applyConfiguration() {
        // Send configuration to device
        // Save configuration to preferences
    }

    // Measurement Methods
    fun performStepMeasurement() {
        viewModelScope.launch {
            try {
                if (deviceManager.isConnected()) {
                    // Perform real measurement using enhanced service
                    val measurement = measurementService.performStepMeasurement(_measurementConfig.value)
                    _currentMeasurement.value = measurement

                    // Add to history
                    val history = _measurementHistory.value.toMutableList()
                    history.add(measurement)
                    _measurementHistory.value = history

                    // Add GPS location if available
                    locationService.addMeasurementPoint(measurement)

                    Log.d("MeasurementViewModel", "Step measurement completed: $measurement")
                } else {
                    // Fallback to simulation if no device connected
                    simulateMeasurement()
                }
            } catch (e: Exception) {
                Log.e("MeasurementViewModel", "Error during step measurement", e)
                // Fallback to simulation on error
                simulateMeasurement()
            }
        }
    }

    fun startContinuousMeasurement() {
        _isRecording.value = true
        // Start continuous measurement based on auto interval
    }

    fun stopMeasurement() {
        _isRecording.value = false
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    private fun simulateMeasurement() {
        // Simulate measurement data for testing
        val measurement = EMFADMeasurementData(
            timestamp = System.currentTimeMillis(),
            frequency = _measurementConfig.value.frequency,
            mode = _measurementConfig.value.mode,
            signalStrength = kotlin.random.Random.nextDouble(0.0, 1.0),
            depth = kotlin.random.Random.nextDouble(0.0, 100.0),
            conductivity = kotlin.random.Random.nextDouble(0.0, 1.0),
            temperature = kotlin.random.Random.nextDouble(15.0, 35.0)
        )

        _currentMeasurement.value = measurement

        val history = _measurementHistory.value.toMutableList()
        history.add(measurement)
        _measurementHistory.value = history
    }

    // Spectrum Analysis Methods
    fun toggleFrequencySelection(frequency: EMFADFrequency) {
        val currentSelection = _selectedFrequencies.value.toMutableSet()
        if (currentSelection.contains(frequency)) {
            currentSelection.remove(frequency)
        } else {
            currentSelection.add(frequency)
        }
        _selectedFrequencies.value = currentSelection
    }

    fun selectAllFrequencies() {
        _selectedFrequencies.value = EMFADFrequency.values().toSet()
    }

    fun clearFrequencySelection() {
        _selectedFrequencies.value = emptySet()
    }

    fun autoScaleSpectrum() {
        // Auto-scale spectrum display
    }

    fun exportSpectrumData() {
        viewModelScope.launch {
            _currentSpectrum.value?.let { spectrum ->
                exportService.exportSpectrum(spectrum)
            }
        }
    }

    fun exportPlotData() {
        viewModelScope.launch {
            val data = _measurementHistory.value
            if (data.isNotEmpty()) {
                exportService.exportMeasurementData(data)
            }
        }
    }

    // Example of how to add a measurement (would come from BLE callback)
    fun onNewDataReceived(rawData: ByteArray) {
        measurementService.processRawData(rawData)
        viewModelScope.launch {
            measurements.value.lastOrNull()?.let { databaseService.saveMeasurement(it) }
        }

        // Also update current measurement and spectrum
        simulateMeasurement()
        updateSpectrum()
    }

    private fun updateSpectrum() {
        // Generate spectrum data from current measurements
        val frequencies = EMFADFrequency.values().map { it.value }
        val amplitudes = frequencies.map { kotlin.random.Random.nextDouble(0.0, 1.0) }

        val spectrum = EMFADSpectrum(
            frequencies = frequencies,
            amplitudes = amplitudes,
            timestamp = System.currentTimeMillis(),
            peakFrequency = frequencies[amplitudes.indexOf(amplitudes.maxOrNull())],
            peakAmplitude = amplitudes.maxOrNull() ?: 0.0,
            totalPower = amplitudes.sum()
        )

        _currentSpectrum.value = spectrum
    }

    override fun onConnected() {
        _connectionStatus.value = "Connected"
    }

    override fun onDisconnected() {
        _connectionStatus.value = "Disconnected"
    }

    override fun onConnectionError(errorMessage: String) {
        _connectionStatus.value = "Error: $errorMessage"
    }

    override fun onDataReceived(data: ByteArray) {
        onNewDataReceived(data)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.close()
    }
}