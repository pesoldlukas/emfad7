package com.emfad.app.hardware

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Abstract connection interface for EMFAD devices
 */
interface EMFADConnection {
    suspend fun sendCommand(command: ByteArray): Result<ByteArray>
    fun disconnect()
    fun isConnected(): Boolean
}

/**
 * USB connection implementation for EMFAD devices
 */
class EMFADUSBConnection(
    private val usbConnection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : EMFADConnection {
    
    companion object {
        private const val TAG = "EMFADUSBConnection"
        private const val COMMAND_TIMEOUT_MS = 5000L
    }
    
    private var isConnected = true
    
    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> {
        if (!isConnected) {
            return Result.failure(Exception("USB connection is not active"))
        }
        
        return try {
            // Get endpoints
            val outEndpoint = usbInterface.getEndpoint(0) // OUT endpoint
            val inEndpoint = usbInterface.getEndpoint(1)  // IN endpoint
            
            // Send command
            val bytesSent = usbConnection.bulkTransfer(
                outEndpoint,
                command,
                command.size,
                COMMAND_TIMEOUT_MS.toInt()
            )
            
            if (bytesSent != command.size) {
                return Result.failure(Exception("Failed to send complete command: sent $bytesSent of ${command.size} bytes"))
            }
            
            Log.d(TAG, "Sent USB command: ${command.toHexString()}")
            
            // Read response
            val responseBuffer = ByteArray(256)
            val bytesReceived = usbConnection.bulkTransfer(
                inEndpoint,
                responseBuffer,
                responseBuffer.size,
                COMMAND_TIMEOUT_MS.toInt()
            )
            
            if (bytesReceived > 0) {
                val response = responseBuffer.sliceArray(0 until bytesReceived)
                Log.d(TAG, "Received USB response: ${response.toHexString()}")
                Result.success(response)
            } else {
                Result.failure(Exception("No response received from USB device"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending USB command", e)
            Result.failure(e)
        }
    }
    
    override fun disconnect() {
        try {
            usbConnection.releaseInterface(usbInterface)
            usbConnection.close()
            isConnected = false
            Log.d(TAG, "USB connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB connection", e)
        }
    }
    
    override fun isConnected(): Boolean = isConnected
    
    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}

/**
 * Bluetooth LE connection implementation for EMFAD devices
 */
class EMFADBluetoothConnection(
    private val context: Context,
    private val device: BluetoothDevice
) : EMFADConnection {
    
    companion object {
        private const val TAG = "EMFADBluetoothConnection"
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val COMMAND_TIMEOUT_MS = 5000L
        
        // EMFAD BLE Service and Characteristic UUIDs (from reverse engineering)
        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        private val COMMAND_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")
        private val DATA_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe")
        private val NOTIFICATION_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abf")
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var notificationCharacteristic: BluetoothGattCharacteristic? = null
    
    private var isConnected = false
    private var connectionResult: Result<Unit>? = null
    private var commandResponse: ByteArray? = null
    private var isWaitingForResponse = false
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    isConnected = false
                    connectionResult = Result.failure(Exception("Connection lost"))
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    commandCharacteristic = service.getCharacteristic(COMMAND_CHARACTERISTIC_UUID)
                    dataCharacteristic = service.getCharacteristic(DATA_CHARACTERISTIC_UUID)
                    notificationCharacteristic = service.getCharacteristic(NOTIFICATION_CHARACTERISTIC_UUID)
                    
                    if (commandCharacteristic != null && dataCharacteristic != null) {
                        // Enable notifications if available
                        notificationCharacteristic?.let { characteristic ->
                            gatt.setCharacteristicNotification(characteristic, true)
                        }
                        
                        isConnected = true
                        connectionResult = Result.success(Unit)
                        Log.d(TAG, "EMFAD service discovered and configured")
                    } else {
                        connectionResult = Result.failure(Exception("Required characteristics not found"))
                    }
                } else {
                    connectionResult = Result.failure(Exception("EMFAD service not found"))
                }
            } else {
                connectionResult = Result.failure(Exception("Service discovery failed: $status"))
            }
        }
        
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Command written successfully")
                // Read response from data characteristic
                dataCharacteristic?.let { gatt?.readCharacteristic(it) }
            } else {
                Log.e(TAG, "Failed to write command: $status")
                if (isWaitingForResponse) {
                    commandResponse = null
                    isWaitingForResponse = false
                }
            }
        }
        
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic == dataCharacteristic) {
                commandResponse = characteristic?.value
                isWaitingForResponse = false
                Log.d(TAG, "Response received: ${commandResponse?.toHexString()}")
            } else {
                Log.e(TAG, "Failed to read response: $status")
                if (isWaitingForResponse) {
                    commandResponse = null
                    isWaitingForResponse = false
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            
            if (characteristic == notificationCharacteristic) {
                val data = characteristic?.value
                Log.d(TAG, "Notification received: ${data?.toHexString()}")
                // Handle real-time data notifications
            }
        }
    }
    
    suspend fun connect(): Result<Unit> {
        return withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
                
                continuation.invokeOnCancellation {
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                }
                
                // Wait for connection result
                while (connectionResult == null) {
                    Thread.sleep(100)
                }
                
                continuation.resume(connectionResult!!)
            }
        } ?: Result.failure(Exception("Connection timeout"))
    }
    
    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> {
        if (!isConnected) {
            return Result.failure(Exception("Bluetooth connection is not active"))
        }
        
        val characteristic = commandCharacteristic
            ?: return Result.failure(Exception("Command characteristic not available"))
        
        return withTimeoutOrNull(COMMAND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                try {
                    isWaitingForResponse = true
                    commandResponse = null
                    
                    characteristic.value = command
                    val writeSuccess = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
                    
                    if (!writeSuccess) {
                        isWaitingForResponse = false
                        continuation.resume(Result.failure(Exception("Failed to initiate command write")))
                        return@suspendCancellableCoroutine
                    }
                    
                    Log.d(TAG, "Sent BLE command: ${command.toHexString()}")
                    
                    // Wait for response
                    while (isWaitingForResponse) {
                        Thread.sleep(50)
                    }
                    
                    val response = commandResponse
                    if (response != null) {
                        continuation.resume(Result.success(response))
                    } else {
                        continuation.resume(Result.failure(Exception("No response received")))
                    }
                    
                } catch (e: Exception) {
                    isWaitingForResponse = false
                    continuation.resume(Result.failure(e))
                }
            }
        } ?: Result.failure(Exception("Command timeout"))
    }
    
    override fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            isConnected = false
            Log.d(TAG, "Bluetooth connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Bluetooth connection", e)
        }
    }
    
    override fun isConnected(): Boolean = isConnected
    
    fun getSignalStrength(): Int? {
        // This would require reading RSSI
        // bluetoothGatt?.readRemoteRssi()
        return null // Placeholder
    }
    
    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}
