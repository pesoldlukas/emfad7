package com.emfad.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID

interface BluetoothConnectionCallback {
    fun onConnected()
    fun onDisconnected()
    fun onConnectionFailed(error: String)
    fun onDataReceived(data: ByteArray)
    fun onCharacteristicWrite(status: Int)
    fun onCharacteristicRead(data: ByteArray?, status: Int)
}

@SuppressLint("MissingPermission")
class EMFADBluetoothManager(private val context: Context, private val bluetoothAdapter: BluetoothAdapter, private val callback: BluetoothConnectionCallback) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null

    // TODO: Replace with actual EMFAD device service and characteristic UUIDs
    private val GATT_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb") // Example Heart Rate Service UUID
    private val MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb") // Example Heart Rate Measurement Characteristic UUID
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("EMFADBluetoothManager", "Connected to GATT server.")
                callback.onConnected()
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("EMFADBluetoothManager", "Disconnected from GATT server.")
                callback.onDisconnected()
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("EMFADBluetoothManager", "Connection state change error: $status")
                callback.onConnectionFailed("Connection error: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(GATT_SERVICE_UUID)
                if (service != null) {
                    measurementCharacteristic = service.getCharacteristic(MEASUREMENT_CHARACTERISTIC_UUID)
                    if (measurementCharacteristic != null) {
                        gatt.setCharacteristicNotification(measurementCharacteristic, true)
                        val descriptor = measurementCharacteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                        Log.d("EMFADBluetoothManager", "Measurement characteristic found and notifications enabled.")
                    } else {
                        Log.e("EMFADBluetoothManager", "Measurement characteristic not found.")
                        callback.onConnectionFailed("Measurement characteristic not found.")
                    }
                } else {
                    Log.e("EMFADBluetoothManager", "GATT service not found.")
                    callback.onConnectionFailed("GATT service not found.")
                }
            } else {
                Log.e("EMFADBluetoothManager", "onServicesDiscovered received status: $status")
                callback.onConnectionFailed("Service discovery failed: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == MEASUREMENT_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                data?.let { callback.onDataReceived(it) }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            callback.onCharacteristicWrite(status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            val data = characteristic.value
            callback.onCharacteristicRead(data, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("EMFADBluetoothManager", "Failed to write descriptor: $status")
                callback.onConnectionFailed("Failed to enable notifications: $status")
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothGatt == null) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } else {
            bluetoothGatt?.connect()
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun writeCharacteristic(data: ByteArray) {
        measurementCharacteristic?.let {
            it.value = data
            bluetoothGatt?.writeCharacteristic(it)
        } ?: Log.e("EMFADBluetoothManager", "Measurement characteristic not available for writing.")
    }

    fun readCharacteristic() {
        measurementCharacteristic?.let {
            bluetoothGatt?.readCharacteristic(it)
        } ?: Log.e("EMFADBluetoothManager", "Measurement characteristic not available for reading.")
    }

    fun getConnectionQuality(): Double {
        // This would typically involve RSSI and other connection parameters
        return 0.95 // Dummy value
    }
}                        Log.e("EMFADBluetoothManager", "Measurement characteristic not found.")
                    }
                } else {
                    Log.e("EMFADBluetoothManager", "GATT service not found.")
                }
            } else {
                Log.w("EMFADBluetoothManager", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == MEASUREMENT_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                // Process the received data
                Log.d("EMFADBluetoothManager", "Received data: ${data.toHexString()}")
                // TODO: Pass data to a ViewModel or a callback for further processing
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}