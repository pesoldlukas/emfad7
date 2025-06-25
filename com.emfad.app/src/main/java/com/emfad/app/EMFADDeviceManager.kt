package com.emfad.app.hardware

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.emfad.app.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EMFAD Device Manager
 * Handles USB and Bluetooth connections to EMFAD hardware
 * Based on connectToDevice() functionality from EMFAD3EXE.c
 */
@Singleton
class EMFADDeviceManager @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    
    companion object {
        private const val TAG = "EMFADDeviceManager"
        
        // EMFAD Device Identifiers (from reverse engineering)
        private const val EMFAD_USB_VENDOR_ID = 0x1234 // Replace with actual vendor ID
        private const val EMFAD_USB_PRODUCT_ID = 0x5678 // Replace with actual product ID
        private const val EMFAD_BLE_SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
        private const val EMFAD_DEVICE_NAME_PREFIX = "EMFAD"
        
        // Connection timeouts
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val COMMAND_TIMEOUT_MS = 5000L
    }
    
    private val _connectionState = MutableStateFlow(EMFADConnectionState.DISCONNECTED)
    val connectionState: StateFlow<EMFADConnectionState> = _connectionState.asStateFlow()
    
    private val _currentDevice = MutableStateFlow<EMFADDevice?>(null)
    val currentDevice: StateFlow<EMFADDevice?> = _currentDevice.asStateFlow()
    
    private val _deviceStatus = MutableStateFlow(EMFADDeviceStatusData(0, 0, 0.0, false, 0, false))
    val deviceStatus: StateFlow<EMFADDeviceStatusData> = _deviceStatus.asStateFlow()
    
    private var usbConnection: UsbDeviceConnection? = null
    private var bluetoothConnection: EMFADBluetoothConnection? = null
    private var currentConnection: EMFADConnection? = null
    
    /**
     * Scan for available EMFAD devices
     */
    suspend fun scanForDevices(): List<EMFADDevice> {
        val devices = mutableListOf<EMFADDevice>()
        
        // Scan USB devices
        devices.addAll(scanUSBDevices())
        
        // Scan Bluetooth devices
        devices.addAll(scanBluetoothDevices())
        
        Log.d(TAG, "Found ${devices.size} EMFAD devices")
        return devices
    }
    
    /**
     * Connect to USB device
     */
    suspend fun connectUSB(device: UsbDevice): Result<EMFADConnection> {
        return try {
            _connectionState.value = EMFADConnectionState.CONNECTING
            
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val connection = usbManager.openDevice(device)
                ?: return Result.failure(Exception("Failed to open USB device"))
            
            // Claim interface
            val usbInterface = device.getInterface(0)
            if (!connection.claimInterface(usbInterface, true)) {
                connection.close()
                return Result.failure(Exception("Failed to claim USB interface"))
            }
            
            usbConnection = connection
            
            // Send connection command
            val connectCommand = EMFADProtocol.createConnectCommand()
            val response = sendUSBCommand(connectCommand)
            
            if (response.isSuccess) {
                val emfadConnection = EMFADUSBConnection(connection, usbInterface)
                currentConnection = emfadConnection
                
                _currentDevice.value = EMFADDevice(
                    id = device.deviceId.toString(),
                    name = device.deviceName ?: "EMFAD USB Device",
                    type = EMFADDeviceType.USB,
                    usbDevice = device,
                    isConnected = true
                )
                
                _connectionState.value = EMFADConnectionState.CONNECTED
                
                // Start status monitoring
                startStatusMonitoring()
                
                Log.d(TAG, "Successfully connected to USB device: ${device.deviceName}")
                Result.success(emfadConnection)
            } else {
                connection.close()
                _connectionState.value = EMFADConnectionState.DISCONNECTED
                Result.failure(Exception("Device did not respond to connection command"))
            }
            
        } catch (e: Exception) {
            _connectionState.value = EMFADConnectionState.DISCONNECTED
            Log.e(TAG, "Failed to connect to USB device", e)
            Result.failure(e)
        }
    }
    
    /**
     * Connect to Bluetooth device
     */
    suspend fun connectBluetooth(device: BluetoothDevice): Result<EMFADConnection> {
        return try {
            _connectionState.value = EMFADConnectionState.CONNECTING
            
            val bleConnection = EMFADBluetoothConnection(context, device)
            val connectResult = bleConnection.connect()
            
            if (connectResult.isSuccess) {
                bluetoothConnection = bleConnection
                currentConnection = bleConnection
                
                _currentDevice.value = EMFADDevice(
                    id = device.address,
                    name = device.name ?: "EMFAD BLE Device",
                    type = EMFADDeviceType.BLUETOOTH,
                    bluetoothDevice = device,
                    isConnected = true
                )
                
                _connectionState.value = EMFADConnectionState.CONNECTED
                
                // Start status monitoring
                startStatusMonitoring()
                
                Log.d(TAG, "Successfully connected to Bluetooth device: ${device.name}")
                Result.success(bleConnection)
            } else {
                _connectionState.value = EMFADConnectionState.DISCONNECTED
                Result.failure(connectResult.exceptionOrNull() ?: Exception("Bluetooth connection failed"))
            }
            
        } catch (e: Exception) {
            _connectionState.value = EMFADConnectionState.DISCONNECTED
            Log.e(TAG, "Failed to connect to Bluetooth device", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send command to connected device
     */
    suspend fun sendCommand(command: EMFADCommand): Result<ByteArray> {
        val connection = currentConnection
            ?: return Result.failure(Exception("No device connected"))
        
        return try {
            val commandBytes = when (command) {
                is EMFADCommand.SetFrequency -> EMFADProtocol.createFrequencyCommand(command.frequency)
                is EMFADCommand.SetMode -> EMFADProtocol.createModeCommand(command.mode)
                is EMFADCommand.SetGain -> EMFADProtocol.createGainCommand(command.gain)
                is EMFADCommand.SetOffset -> EMFADProtocol.createOffsetCommand(command.offset)
                is EMFADCommand.StartMeasurement -> EMFADProtocol.createStartMeasurementCommand(command.autoMode, command.interval)
                is EMFADCommand.StopMeasurement -> EMFADProtocol.createCommandPacket(EMFADProtocol.CMD_STOP_MEASUREMENT, byteArrayOf())
                is EMFADCommand.ReadData -> EMFADProtocol.createReadDataCommand()
                is EMFADCommand.Calibrate -> EMFADProtocol.createCalibrateCommand()
            }
            
            connection.sendCommand(commandBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command", e)
            Result.failure(e)
        }
    }
    
    /**
     * Read signal data from device
     */
    fun readSignalData(): Flow<EMFADRawData> = flow {
        val connection = currentConnection
        if (connection == null) {
            Log.w(TAG, "No device connected for reading signal data")
            return@flow
        }
        
        while (_connectionState.value == EMFADConnectionState.CONNECTED) {
            try {
                val readCommand = EMFADProtocol.createReadDataCommand()
                val response = connection.sendCommand(readCommand)
                
                if (response.isSuccess) {
                    val rawData = EMFADProtocol.parseSignalData(response.getOrThrow())
                    if (rawData != null && rawData.isValid) {
                        emit(rawData)
                    }
                }
                
                delay(100) // Read every 100ms for real-time data
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading signal data", e)
                delay(1000) // Wait before retrying
            }
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        try {
            currentConnection?.disconnect()
            usbConnection?.close()
            bluetoothConnection?.disconnect()
            
            currentConnection = null
            usbConnection = null
            bluetoothConnection = null
            
            _currentDevice.value = null
            _connectionState.value = EMFADConnectionState.DISCONNECTED
            
            Log.d(TAG, "Disconnected from device")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
    
    /**
     * Check if device is connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == EMFADConnectionState.CONNECTED
    }
    
    /**
     * Get connection quality (0.0 to 1.0)
     */
    fun getConnectionQuality(): Double {
        return when (_connectionState.value) {
            EMFADConnectionState.CONNECTED -> {
                // For Bluetooth, use RSSI; for USB, assume good quality
                bluetoothConnection?.getSignalStrength()?.let { rssi ->
                    // Convert RSSI to quality (typical range -30 to -90 dBm)
                    ((rssi + 90) / 60.0).coerceIn(0.0, 1.0)
                } ?: 0.95 // USB connection assumed good
            }
            else -> 0.0
        }
    }
    
    private suspend fun scanUSBDevices(): List<EMFADDevice> {
        val devices = mutableListOf<EMFADDevice>()
        
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList
            
            for ((_, device) in deviceList) {
                if (device.vendorId == EMFAD_USB_VENDOR_ID && 
                    device.productId == EMFAD_USB_PRODUCT_ID) {
                    
                    devices.add(EMFADDevice(
                        id = device.deviceId.toString(),
                        name = device.deviceName ?: "EMFAD USB Device",
                        type = EMFADDeviceType.USB,
                        usbDevice = device,
                        isConnected = false
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning USB devices", e)
        }
        
        return devices
    }
    
    private suspend fun scanBluetoothDevices(): List<EMFADDevice> {
        val devices = mutableListOf<EMFADDevice>()
        
        try {
            bluetoothAdapter?.let { adapter ->
                if (adapter.isEnabled) {
                    val bondedDevices = adapter.bondedDevices
                    
                    for (device in bondedDevices) {
                        if (device.name?.startsWith(EMFAD_DEVICE_NAME_PREFIX) == true) {
                            devices.add(EMFADDevice(
                                id = device.address,
                                name = device.name ?: "EMFAD BLE Device",
                                type = EMFADDeviceType.BLUETOOTH,
                                bluetoothDevice = device,
                                isConnected = false
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning Bluetooth devices", e)
        }
        
        return devices
    }
    
    private suspend fun sendUSBCommand(command: ByteArray): Result<ByteArray> {
        val connection = usbConnection ?: return Result.failure(Exception("No USB connection"))
        
        return try {
            // Send command
            val bytesSent = connection.bulkTransfer(
                connection.interface.getEndpoint(0), // OUT endpoint
                command,
                command.size,
                COMMAND_TIMEOUT_MS.toInt()
            )
            
            if (bytesSent != command.size) {
                return Result.failure(Exception("Failed to send complete command"))
            }
            
            // Read response
            val responseBuffer = ByteArray(256)
            val bytesReceived = connection.bulkTransfer(
                connection.interface.getEndpoint(1), // IN endpoint
                responseBuffer,
                responseBuffer.size,
                COMMAND_TIMEOUT_MS.toInt()
            )
            
            if (bytesReceived > 0) {
                Result.success(responseBuffer.sliceArray(0 until bytesReceived))
            } else {
                Result.failure(Exception("No response received"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun startStatusMonitoring() {
        // Periodically read device status
        // This would run in a background coroutine
    }
}

/**
 * EMFAD Device representation
 */
data class EMFADDevice(
    val id: String,
    val name: String,
    val type: EMFADDeviceType,
    val usbDevice: UsbDevice? = null,
    val bluetoothDevice: BluetoothDevice? = null,
    val isConnected: Boolean = false
)

/**
 * Device types
 */
enum class EMFADDeviceType {
    USB,
    BLUETOOTH,
    WIFI
}

/**
 * Connection states
 */
enum class EMFADConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * EMFAD Commands
 */
sealed class EMFADCommand {
    data class SetFrequency(val frequency: EMFADFrequency) : EMFADCommand()
    data class SetMode(val mode: EMFADMode) : EMFADCommand()
    data class SetGain(val gain: Double) : EMFADCommand()
    data class SetOffset(val offset: Double) : EMFADCommand()
    data class StartMeasurement(val autoMode: Boolean, val interval: Long) : EMFADCommand()
    object StopMeasurement : EMFADCommand()
    object ReadData : EMFADCommand()
    object Calibrate : EMFADCommand()
}
