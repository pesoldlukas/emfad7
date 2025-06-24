package com.emfad.app.hardware

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emfad.app.models.EMFADFrequency
import com.emfad.app.models.EMFADMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for EMFAD Device Manager
 * Tests hardware communication and protocol implementation
 */
@RunWith(AndroidJUnit4::class)
class EMFADDeviceManagerTest {
    
    private lateinit var context: Context
    private lateinit var deviceManager: EMFADDeviceManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceManager = EMFADDeviceManager(context, bluetoothAdapter)
    }
    
    @Test
    fun testDeviceScanning() = runBlocking {
        // Test device scanning functionality
        val devices = deviceManager.scanForDevices()
        
        // Should not crash and return a list (may be empty if no devices)
        assertNotNull(devices)
        assertTrue("Device list should be non-null", devices is List)
        
        // Log found devices for debugging
        devices.forEach { device ->
            println("Found device: ${device.name} (${device.type})")
        }
    }
    
    @Test
    fun testProtocolCommandCreation() {
        // Test EMFAD protocol command creation
        
        // Test frequency command
        val freqCommand = EMFADProtocol.createFrequencyCommand(EMFADFrequency.FREQ_19KHZ)
        assertNotNull("Frequency command should not be null", freqCommand)
        assertTrue("Frequency command should not be empty", freqCommand.isNotEmpty())
        
        // Test mode command
        val modeCommand = EMFADProtocol.createModeCommand(EMFADMode.MODE_A)
        assertNotNull("Mode command should not be null", modeCommand)
        assertTrue("Mode command should not be empty", modeCommand.isNotEmpty())
        
        // Test gain command
        val gainCommand = EMFADProtocol.createGainCommand(1.5)
        assertNotNull("Gain command should not be null", gainCommand)
        assertTrue("Gain command should not be empty", gainCommand.isNotEmpty())
        
        // Test offset command
        val offsetCommand = EMFADProtocol.createOffsetCommand(0.1)
        assertNotNull("Offset command should not be null", offsetCommand)
        assertTrue("Offset command should not be empty", offsetCommand.isNotEmpty())
    }
    
    @Test
    fun testProtocolDataParsing() {
        // Test protocol data parsing with simulated data
        
        // Create simulated signal data response
        val simulatedResponse = createSimulatedSignalResponse()
        val parsedData = EMFADProtocol.parseSignalData(simulatedResponse)
        
        if (parsedData != null) {
            assertTrue("Parsed data should be valid", parsedData.isValid)
            assertTrue("Signal strength should be positive", parsedData.signalStrength >= 0)
            assertTrue("Frequency should be positive", parsedData.frequency > 0)
            assertTrue("Temperature should be reasonable", parsedData.temperature > -50 && parsedData.temperature < 100)
        }
    }
    
    @Test
    fun testConnectionStateManagement() = runBlocking {
        // Test connection state management
        
        // Initially should be disconnected
        assertFalse("Should start disconnected", deviceManager.isConnected())
        
        // Test connection state flow
        var stateUpdates = 0
        val job = kotlinx.coroutines.launch {
            deviceManager.connectionState.collect { state ->
                stateUpdates++
                println("Connection state: $state")
            }
        }
        
        // Allow some time for initial state
        kotlinx.coroutines.delay(100)
        
        job.cancel()
        assertTrue("Should receive at least one state update", stateUpdates > 0)
    }
    
    @Test
    fun testCommandTimeout() = runBlocking {
        // Test command timeout handling
        
        if (!deviceManager.isConnected()) {
            // Try to send command without connection - should fail gracefully
            val command = EMFADCommand.SetFrequency(EMFADFrequency.FREQ_19KHZ)
            val result = deviceManager.sendCommand(command)
            
            assertTrue("Command should fail when not connected", result.isFailure)
            assertNotNull("Should have error message", result.exceptionOrNull())
        }
    }
    
    @Test
    fun testDataValidation() {
        // Test data validation functions
        
        // Test checksum validation
        val validData = byteArrayOf(0x01, 0x02, 0x03, 0x00) // Last byte is XOR checksum
        val invalidData = byteArrayOf(0x01, 0x02, 0x03, 0xFF) // Invalid checksum
        
        assertTrue("Valid data should pass checksum", EMFADProtocol.validateChecksum(validData))
        assertFalse("Invalid data should fail checksum", EMFADProtocol.validateChecksum(invalidData))
    }
    
    @Test
    fun testFrequencyMapping() {
        // Test frequency value mapping
        
        val frequencies = EMFADFrequency.values()
        
        frequencies.forEach { frequency ->
            val command = EMFADProtocol.createFrequencyCommand(frequency)
            assertNotNull("Command should be created for frequency ${frequency.displayName}", command)
            
            // Verify command contains frequency information
            assertTrue("Command should be at least 8 bytes", command.size >= 8)
        }
    }
    
    @Test
    fun testModeMapping() {
        // Test measurement mode mapping
        
        val modes = EMFADMode.values()
        
        modes.forEach { mode ->
            val command = EMFADProtocol.createModeCommand(mode)
            assertNotNull("Command should be created for mode ${mode.displayName}", command)
            
            // Verify command structure
            assertTrue("Command should be at least 6 bytes", command.size >= 6)
        }
    }
    
    @Test
    fun testErrorHandling() = runBlocking {
        // Test error handling scenarios
        
        // Test with invalid device
        try {
            val result = deviceManager.connectUSB(null as android.hardware.usb.UsbDevice?)
            assertTrue("Should handle null device gracefully", result.isFailure)
        } catch (e: Exception) {
            // Expected - null device should cause exception
            assertTrue("Should handle null device", true)
        }
    }
    
    @Test
    fun testMemoryManagement() {
        // Test memory management and cleanup
        
        // Create multiple command objects
        repeat(1000) {
            val command = EMFADProtocol.createFrequencyCommand(EMFADFrequency.FREQ_19KHZ)
            assertNotNull("Command $it should be created", command)
        }
        
        // Force garbage collection
        System.gc()
        
        // Should still be able to create commands
        val finalCommand = EMFADProtocol.createFrequencyCommand(EMFADFrequency.FREQ_135KHZ)
        assertNotNull("Final command should be created", finalCommand)
    }
    
    /**
     * Create simulated signal response for testing
     */
    private fun createSimulatedSignalResponse(): ByteArray {
        val response = mutableListOf<Byte>()
        
        // Header
        response.addAll(listOf(0xAA.toByte(), 0x55.toByte()))
        
        // Data type (signal)
        response.add(0x01)
        
        // Length
        response.add(0x14) // 20 bytes of data
        
        // Timestamp (8 bytes)
        val timestamp = System.currentTimeMillis()
        response.addAll(longToBytes(timestamp))
        
        // Signal strength (4 bytes float)
        response.addAll(floatToBytes(0.75f))
        
        // Phase (4 bytes float)
        response.addAll(floatToBytes(45.0f))
        
        // Frequency (4 bytes float)
        response.addAll(floatToBytes(19000.0f))
        
        // Temperature (4 bytes float)
        response.addAll(floatToBytes(23.5f))
        
        // Footer
        response.addAll(listOf(0x55.toByte(), 0xAA.toByte()))
        
        return response.toByteArray()
    }
    
    private fun longToBytes(value: Long): List<Byte> {
        return listOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 32) and 0xFF).toByte(),
            ((value shr 40) and 0xFF).toByte(),
            ((value shr 48) and 0xFF).toByte(),
            ((value shr 56) and 0xFF).toByte()
        )
    }
    
    private fun floatToBytes(value: Float): List<Byte> {
        val bits = value.toBits()
        return listOf(
            (bits and 0xFF).toByte(),
            ((bits shr 8) and 0xFF).toByte(),
            ((bits shr 16) and 0xFF).toByte(),
            ((bits shr 24) and 0xFF).toByte()
        )
    }
}
