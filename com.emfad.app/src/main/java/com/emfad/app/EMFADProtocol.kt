package com.emfad.app.hardware

import com.emfad.app.models.EMFADFrequency
import com.emfad.app.models.EMFADMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * EMFAD Hardware Communication Protocol
 * Based on reverse-engineered protocols from EMFAD3EXE.c, EMUNIX07EXE.c, and HzEMSoftexe.c
 */
object EMFADProtocol {
    
    // Protocol Constants (extracted from Ghidra analysis)
    const val PROTOCOL_VERSION = 0x01
    const val PACKET_HEADER = 0xAA55.toShort()
    const val PACKET_FOOTER = 0x55AA.toShort()
    const val MAX_PACKET_SIZE = 256
    
    // Command Bytes (from connectToDevice, parseEGD functions)
    const val CMD_CONNECT = 0x01
    const val CMD_DISCONNECT = 0x02
    const val CMD_SET_FREQUENCY = 0x03
    const val CMD_SET_MODE = 0x04
    const val CMD_SET_GAIN = 0x05
    const val CMD_SET_OFFSET = 0x06
    const val CMD_START_MEASUREMENT = 0x10
    const val CMD_STOP_MEASUREMENT = 0x11
    const val CMD_READ_DATA = 0x12
    const val CMD_CALIBRATE = 0x20
    const val CMD_GET_STATUS = 0x30
    const val CMD_GET_VERSION = 0x31
    
    // Response Codes
    const val RESP_OK = 0x00
    const val RESP_ERROR = 0xFF
    const val RESP_INVALID_COMMAND = 0xFE
    const val RESP_DEVICE_BUSY = 0xFD
    const val RESP_CALIBRATION_REQUIRED = 0xFC
    
    // Data Types
    const val DATA_TYPE_SIGNAL = 0x01
    const val DATA_TYPE_SPECTRUM = 0x02
    const val DATA_TYPE_STATUS = 0x03
    const val DATA_TYPE_CALIBRATION = 0x04
    
    /**
     * Create connection command packet
     */
    fun createConnectCommand(): ByteArray {
        return createCommandPacket(CMD_CONNECT, byteArrayOf(PROTOCOL_VERSION.toByte()))
    }
    
    /**
     * Create frequency setting command
     * Based on frequency selection from original EMFAD software
     */
    fun createFrequencyCommand(frequency: EMFADFrequency): ByteArray {
        val frequencyCode = when (frequency) {
            EMFADFrequency.FREQ_19KHZ -> 0x01
            EMFADFrequency.FREQ_38KHZ -> 0x02
            EMFADFrequency.FREQ_57KHZ -> 0x03
            EMFADFrequency.FREQ_76KHZ -> 0x04
            EMFADFrequency.FREQ_95KHZ -> 0x05
            EMFADFrequency.FREQ_114KHZ -> 0x06
            EMFADFrequency.FREQ_135KHZ -> 0x07
        }
        
        val payload = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(frequencyCode.toByte())
            .putShort((frequency.value / 1000).toInt().toShort()) // Frequency in kHz
            .put(0x00) // Reserved
            .array()
            
        return createCommandPacket(CMD_SET_FREQUENCY, payload)
    }
    
    /**
     * Create measurement mode command
     * Implements A, A-B, B, B-A modes from original software
     */
    fun createModeCommand(mode: EMFADMode): ByteArray {
        val modeCode = when (mode) {
            EMFADMode.MODE_A -> 0x01
            EMFADMode.MODE_A_MINUS_B -> 0x02
            EMFADMode.MODE_B -> 0x03
            EMFADMode.MODE_B_MINUS_A -> 0x04
        }
        
        return createCommandPacket(CMD_SET_MODE, byteArrayOf(modeCode.toByte()))
    }
    
    /**
     * Create gain setting command
     */
    fun createGainCommand(gain: Double): ByteArray {
        val gainValue = (gain * 100).toInt().toShort() // Convert to fixed-point
        val payload = ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(gainValue)
            .array()
            
        return createCommandPacket(CMD_SET_GAIN, payload)
    }
    
    /**
     * Create offset setting command
     */
    fun createOffsetCommand(offset: Double): ByteArray {
        val offsetValue = (offset * 1000).toInt().toShort() // Convert to fixed-point
        val payload = ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(offsetValue)
            .array()
            
        return createCommandPacket(CMD_SET_OFFSET, payload)
    }
    
    /**
     * Create measurement start command
     */
    fun createStartMeasurementCommand(autoMode: Boolean, interval: Long): ByteArray {
        val payload = ByteBuffer.allocate(5)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(if (autoMode) 0x01 else 0x00)
            .putInt(interval.toInt()) // Interval in milliseconds
            .array()
            
        return createCommandPacket(CMD_START_MEASUREMENT, payload)
    }
    
    /**
     * Create data read command
     */
    fun createReadDataCommand(): ByteArray {
        return createCommandPacket(CMD_READ_DATA, byteArrayOf())
    }
    
    /**
     * Create calibration command
     */
    fun createCalibrateCommand(): ByteArray {
        return createCommandPacket(CMD_CALIBRATE, byteArrayOf())
    }
    
    /**
     * Parse signal data from device response
     * Implements parseEGD/ESD functionality from original software
     */
    fun parseSignalData(rawBytes: ByteArray): EMFADRawData? {
        if (rawBytes.size < 8) return null
        
        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Validate packet structure
        val header = buffer.short
        if (header != PACKET_HEADER) return null
        
        val dataType = buffer.get()
        if (dataType != DATA_TYPE_SIGNAL) return null
        
        val length = buffer.get().toInt() and 0xFF
        if (buffer.remaining() < length + 2) return null // +2 for footer
        
        // Parse signal data
        val timestamp = buffer.long
        val signalStrength = buffer.float
        val phase = buffer.float
        val frequency = buffer.float
        val temperature = buffer.float
        
        // Validate footer
        val footer = buffer.short
        if (footer != PACKET_FOOTER) return null
        
        return EMFADRawData(
            timestamp = timestamp,
            signalStrength = signalStrength.toDouble(),
            phase = phase.toDouble(),
            frequency = frequency.toDouble(),
            temperature = temperature.toDouble(),
            isValid = true
        )
    }
    
    /**
     * Parse spectrum data from device response
     */
    fun parseSpectrumData(rawBytes: ByteArray): EMFADRawSpectrum? {
        if (rawBytes.size < 12) return null
        
        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Validate packet structure
        val header = buffer.short
        if (header != PACKET_HEADER) return null
        
        val dataType = buffer.get()
        if (dataType != DATA_TYPE_SPECTRUM) return null
        
        val length = buffer.get().toInt() and 0xFF
        val spectrumSize = buffer.short.toInt() and 0xFFFF
        
        if (buffer.remaining() < (spectrumSize * 8) + 2) return null // 8 bytes per point + footer
        
        val frequencies = mutableListOf<Double>()
        val amplitudes = mutableListOf<Double>()
        
        repeat(spectrumSize) {
            frequencies.add(buffer.float.toDouble())
            amplitudes.add(buffer.float.toDouble())
        }
        
        // Validate footer
        val footer = buffer.short
        if (footer != PACKET_FOOTER) return null
        
        return EMFADRawSpectrum(
            frequencies = frequencies,
            amplitudes = amplitudes,
            timestamp = System.currentTimeMillis(),
            isValid = true
        )
    }
    
    /**
     * Parse device status response
     */
    fun parseStatusData(rawBytes: ByteArray): EMFADDeviceStatusData? {
        if (rawBytes.size < 16) return null
        
        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Validate packet structure
        val header = buffer.short
        if (header != PACKET_HEADER) return null
        
        val dataType = buffer.get()
        if (dataType != DATA_TYPE_STATUS) return null
        
        val length = buffer.get().toInt() and 0xFF
        
        val batteryLevel = buffer.get().toInt() and 0xFF
        val signalQuality = buffer.get().toInt() and 0xFF
        val temperature = buffer.float
        val isCalibrated = buffer.get() != 0.toByte()
        val firmwareVersion = buffer.short.toInt() and 0xFFFF
        
        // Validate footer
        val footer = buffer.short
        if (footer != PACKET_FOOTER) return null
        
        return EMFADDeviceStatusData(
            batteryLevel = batteryLevel,
            signalQuality = signalQuality,
            temperature = temperature.toDouble(),
            isCalibrated = isCalibrated,
            firmwareVersion = firmwareVersion,
            isValid = true
        )
    }
    
    /**
     * Validate packet checksum
     */
    fun validateChecksum(data: ByteArray): Boolean {
        if (data.size < 3) return false
        
        var checksum = 0
        for (i in 0 until data.size - 1) {
            checksum = checksum xor data[i].toInt()
        }
        
        return (checksum and 0xFF) == (data.last().toInt() and 0xFF)
    }
    
    /**
     * Create command packet with header, command, payload, checksum, and footer
     */
    private fun createCommandPacket(command: Int, payload: ByteArray): ByteArray {
        val totalSize = 6 + payload.size // header(2) + cmd(1) + len(1) + payload + checksum(1) + footer(2)
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putShort(PACKET_HEADER)
        buffer.put(command.toByte())
        buffer.put(payload.size.toByte())
        buffer.put(payload)
        
        // Calculate checksum
        val checksumData = buffer.array().sliceArray(0 until buffer.position())
        var checksum = 0
        for (byte in checksumData) {
            checksum = checksum xor byte.toInt()
        }
        buffer.put((checksum and 0xFF).toByte())
        
        buffer.putShort(PACKET_FOOTER)
        
        return buffer.array()
    }
}

/**
 * Raw data structures for parsed device responses
 */
data class EMFADRawData(
    val timestamp: Long,
    val signalStrength: Double,
    val phase: Double,
    val frequency: Double,
    val temperature: Double,
    val isValid: Boolean
)

data class EMFADRawSpectrum(
    val frequencies: List<Double>,
    val amplitudes: List<Double>,
    val timestamp: Long,
    val isValid: Boolean
)

data class EMFADDeviceStatusData(
    val batteryLevel: Int,
    val signalQuality: Int,
    val temperature: Double,
    val isCalibrated: Boolean,
    val firmwareVersion: Int,
    val isValid: Boolean
)
