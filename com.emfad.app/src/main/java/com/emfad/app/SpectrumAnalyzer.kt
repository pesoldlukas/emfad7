package com.emfad.app.services

import com.emfad.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * EMFAD Spectrum Analyzer
 * Implements frequency analysis algorithms from HzEMSoftexe.c
 * Based on reverse-engineered spectrum analysis functions
 */
@Singleton
class SpectrumAnalyzer @Inject constructor() {
    
    companion object {
        private const val TAG = "SpectrumAnalyzer"
        
        // Analysis constants from reverse engineering
        private const val DEPTH_CALCULATION_FACTOR = 0.417
        private const val SIGNAL_REFERENCE_LEVEL = 1000.0
        private const val MIN_PEAK_THRESHOLD = 0.1
        private const val FREQUENCY_RESOLUTION = 1000.0 // Hz
    }
    
    /**
     * Analyze spectrum from raw measurement data
     * Implements spectrum analysis from original EMFAD software
     */
    suspend fun analyzeSpectrum(rawData: List<Double>, frequencies: List<Double>): EMFADSpectrum = withContext(Dispatchers.Default) {
        require(rawData.size == frequencies.size) { "Data and frequency arrays must have same size" }
        
        if (rawData.isEmpty()) {
            return@withContext EMFADSpectrum(
                frequencies = emptyList(),
                amplitudes = emptyList(),
                timestamp = System.currentTimeMillis()
            )
        }
        
        // Apply windowing function (Hanning window)
        val windowedData = applyHanningWindow(rawData)
        
        // Calculate power spectrum
        val powerSpectrum = calculatePowerSpectrum(windowedData)
        
        // Find peaks
        val peaks = findPeaks(powerSpectrum, frequencies)
        val peakFrequency = peaks.maxByOrNull { it.amplitude }?.frequency ?: frequencies.first()
        val peakAmplitude = peaks.maxByOrNull { it.amplitude }?.amplitude ?: 0.0
        
        // Calculate total power
        val totalPower = powerSpectrum.sum()
        
        EMFADSpectrum(
            frequencies = frequencies,
            amplitudes = powerSpectrum,
            phases = calculatePhases(windowedData),
            timestamp = System.currentTimeMillis(),
            peakFrequency = peakFrequency,
            peakAmplitude = peakAmplitude,
            totalPower = totalPower
        )
    }
    
    /**
     * Analyze single signal value
     */
    fun analyzeSignal(signalStrength: Double, frequency: Double): EMFADSpectrum {
        return EMFADSpectrum(
            frequencies = listOf(frequency),
            amplitudes = listOf(signalStrength),
            phases = listOf(0.0),
            timestamp = System.currentTimeMillis(),
            peakFrequency = frequency,
            peakAmplitude = signalStrength,
            totalPower = signalStrength
        )
    }
    
    /**
     * Calculate depth using algorithm from HzEMSoftexe.c
     * Formula: depth = -ln(signal / 1000.0) / 0.417
     */
    fun calculateDepth(signalStrength: Double, frequency: Double): Double {
        return if (signalStrength > 0) {
            -ln(signalStrength / SIGNAL_REFERENCE_LEVEL) / DEPTH_CALCULATION_FACTOR
        } else {
            0.0
        }
    }
    
    /**
     * Detect peaks in spectrum
     * Implements peak detection algorithm from original software
     */
    fun detectPeaks(spectrum: EMFADSpectrum): List<SpectrumPeak> {
        return findPeaks(spectrum.amplitudes, spectrum.frequencies)
    }
    
    /**
     * Calculate anomaly depth from multiple measurements
     * Implements calculateAnomalyDepth function from reverse-engineered code
     */
    fun calculateAnomalyDepth(measurements: List<EMFADMeasurementData>): Double {
        if (measurements.isEmpty()) return 0.0
        
        // Find maximum signal strength
        val maxSignal = measurements.maxOfOrNull { it.signalStrength } ?: 0.0
        
        // Calculate depth based on maximum signal
        val primaryDepth = calculateDepth(maxSignal, measurements.first().frequency.value)
        
        // Apply correction factors based on measurement pattern
        val correctionFactor = calculateDepthCorrection(measurements)
        
        return primaryDepth * correctionFactor
    }
    
    /**
     * Analyze material properties from spectrum
     */
    suspend fun analyzeMaterial(spectrum: EMFADSpectrum, measurements: List<EMFADMeasurementData>): MaterialAnalysisResult = withContext(Dispatchers.Default) {
        val peaks = detectPeaks(spectrum)
        val dominantFrequency = spectrum.peakFrequency
        val signalPattern = analyzeSignalPattern(measurements)
        
        // Material classification based on frequency response
        val materialType = classifyMaterial(dominantFrequency, spectrum.peakAmplitude, signalPattern)
        val confidence = calculateConfidence(spectrum, signalPattern)
        
        MaterialAnalysisResult(
            materialType = materialType,
            confidence = confidence,
            dominantFrequency = dominantFrequency,
            signalPattern = signalPattern,
            peaks = peaks,
            analysisTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Apply Hanning window to reduce spectral leakage
     */
    private fun applyHanningWindow(data: List<Double>): List<Double> {
        val n = data.size
        return data.mapIndexed { i, value ->
            val window = 0.5 * (1 - cos(2 * PI * i / (n - 1)))
            value * window
        }
    }
    
    /**
     * Calculate power spectrum from windowed data
     */
    private fun calculatePowerSpectrum(data: List<Double>): List<Double> {
        // Simplified power spectrum calculation
        // In real implementation, this would use FFT
        return data.map { value ->
            value * value // Power = amplitude squared
        }
    }
    
    /**
     * Calculate phase information
     */
    private fun calculatePhases(data: List<Double>): List<Double> {
        // Simplified phase calculation
        // Real implementation would use complex FFT
        return data.mapIndexed { index, _ ->
            (index * 2 * PI / data.size) % (2 * PI)
        }
    }
    
    /**
     * Find peaks in spectrum data
     */
    private fun findPeaks(amplitudes: List<Double>, frequencies: List<Double>): List<SpectrumPeak> {
        val peaks = mutableListOf<SpectrumPeak>()
        
        if (amplitudes.size < 3) return peaks
        
        for (i in 1 until amplitudes.size - 1) {
            val current = amplitudes[i]
            val prev = amplitudes[i - 1]
            val next = amplitudes[i + 1]
            
            // Check if current point is a local maximum
            if (current > prev && current > next && current > MIN_PEAK_THRESHOLD) {
                peaks.add(
                    SpectrumPeak(
                        frequency = frequencies[i],
                        amplitude = current,
                        index = i,
                        width = calculatePeakWidth(amplitudes, i),
                        prominence = current - minOf(prev, next)
                    )
                )
            }
        }
        
        // Sort by amplitude (highest first)
        return peaks.sortedByDescending { it.amplitude }
    }
    
    /**
     * Calculate peak width at half maximum
     */
    private fun calculatePeakWidth(amplitudes: List<Double>, peakIndex: Int): Double {
        val peakAmplitude = amplitudes[peakIndex]
        val halfMax = peakAmplitude / 2.0
        
        // Find left half-maximum point
        var leftIndex = peakIndex
        while (leftIndex > 0 && amplitudes[leftIndex] > halfMax) {
            leftIndex--
        }
        
        // Find right half-maximum point
        var rightIndex = peakIndex
        while (rightIndex < amplitudes.size - 1 && amplitudes[rightIndex] > halfMax) {
            rightIndex++
        }
        
        return (rightIndex - leftIndex).toDouble()
    }
    
    /**
     * Calculate depth correction factor based on measurement pattern
     */
    private fun calculateDepthCorrection(measurements: List<EMFADMeasurementData>): Double {
        if (measurements.size < 2) return 1.0
        
        // Analyze signal variation
        val signals = measurements.map { it.signalStrength }
        val mean = signals.average()
        val variance = signals.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Calculate correction based on signal stability
        val stabilityFactor = 1.0 - (standardDeviation / mean).coerceIn(0.0, 0.5)
        
        // Apply frequency-dependent correction
        val frequencyFactor = calculateFrequencyCorrection(measurements.first().frequency)
        
        return stabilityFactor * frequencyFactor
    }
    
    /**
     * Calculate frequency-dependent correction factor
     */
    private fun calculateFrequencyCorrection(frequency: EMFADFrequency): Double {
        return when (frequency) {
            EMFADFrequency.FREQ_19KHZ -> 1.0
            EMFADFrequency.FREQ_38KHZ -> 0.95
            EMFADFrequency.FREQ_57KHZ -> 0.90
            EMFADFrequency.FREQ_76KHZ -> 0.85
            EMFADFrequency.FREQ_95KHZ -> 0.80
            EMFADFrequency.FREQ_114KHZ -> 0.75
            EMFADFrequency.FREQ_135KHZ -> 0.70
        }
    }
    
    /**
     * Analyze signal pattern for material classification
     */
    private fun analyzeSignalPattern(measurements: List<EMFADMeasurementData>): SignalPattern {
        if (measurements.isEmpty()) {
            return SignalPattern.UNKNOWN
        }
        
        val signals = measurements.map { it.signalStrength }
        val mean = signals.average()
        val variance = signals.map { (it - mean).pow(2) }.average()
        val trend = calculateTrend(signals)
        
        return when {
            variance < 0.01 && abs(trend) < 0.001 -> SignalPattern.STABLE
            trend > 0.01 -> SignalPattern.INCREASING
            trend < -0.01 -> SignalPattern.DECREASING
            variance > 0.1 -> SignalPattern.OSCILLATING
            else -> SignalPattern.VARIABLE
        }
    }
    
    /**
     * Calculate signal trend (slope)
     */
    private fun calculateTrend(signals: List<Double>): Double {
        if (signals.size < 2) return 0.0
        
        val n = signals.size
        val x = (0 until n).map { it.toDouble() }
        val y = signals
        
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it * it }.sum()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }
    
    /**
     * Classify material based on frequency response
     */
    private fun classifyMaterial(frequency: Double, amplitude: Double, pattern: SignalPattern): MaterialType {
        // Simplified material classification
        // Real implementation would use machine learning or lookup tables
        return when {
            amplitude > 0.8 && frequency < 50000 -> MaterialType.METAL
            amplitude > 0.6 && pattern == SignalPattern.STABLE -> MaterialType.CERAMIC
            amplitude < 0.3 -> MaterialType.ORGANIC
            pattern == SignalPattern.OSCILLATING -> MaterialType.COMPOSITE
            else -> MaterialType.UNKNOWN
        }
    }
    
    /**
     * Calculate confidence in material analysis
     */
    private fun calculateConfidence(spectrum: EMFADSpectrum, pattern: SignalPattern): Double {
        val peakStrength = spectrum.peakAmplitude / spectrum.totalPower
        val patternConfidence = when (pattern) {
            SignalPattern.STABLE -> 0.9
            SignalPattern.INCREASING, SignalPattern.DECREASING -> 0.7
            SignalPattern.OSCILLATING -> 0.6
            SignalPattern.VARIABLE -> 0.5
            SignalPattern.UNKNOWN -> 0.3
        }
        
        return (peakStrength * patternConfidence).coerceIn(0.0, 1.0)
    }
}

/**
 * Spectrum peak data structure
 */
data class SpectrumPeak(
    val frequency: Double,
    val amplitude: Double,
    val index: Int,
    val width: Double,
    val prominence: Double
)

/**
 * Material analysis result
 */
data class MaterialAnalysisResult(
    val materialType: MaterialType,
    val confidence: Double,
    val dominantFrequency: Double,
    val signalPattern: SignalPattern,
    val peaks: List<SpectrumPeak>,
    val analysisTimestamp: Long
)

/**
 * Signal pattern types
 */
enum class SignalPattern {
    STABLE,
    INCREASING,
    DECREASING,
    OSCILLATING,
    VARIABLE,
    UNKNOWN
}
