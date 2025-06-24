package com.emfad.app.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.emfad.app.hardware.EMFADDeviceManager
import com.emfad.app.services.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection Module for EMFAD Services
 * Provides all backend services for hardware integration
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * Provide Bluetooth Adapter
     */
    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }
    
    /**
     * Provide EMFAD Device Manager
     */
    @Provides
    @Singleton
    fun provideEMFADDeviceManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter?
    ): EMFADDeviceManager {
        return EMFADDeviceManager(context, bluetoothAdapter)
    }
    
    /**
     * Provide Spectrum Analyzer
     */
    @Provides
    @Singleton
    fun provideSpectrumAnalyzer(): SpectrumAnalyzer {
        return SpectrumAnalyzer()
    }
    
    /**
     * Provide Enhanced Measurement Service
     */
    @Provides
    @Singleton
    fun provideMeasurementService(
        deviceManager: EMFADDeviceManager,
        spectrumAnalyzer: SpectrumAnalyzer
    ): MeasurementService {
        return MeasurementService(deviceManager, spectrumAnalyzer)
    }
    
    /**
     * Provide Export Service
     */
    @Provides
    @Singleton
    fun provideExportService(@ApplicationContext context: Context): ExportService {
        return ExportService(context)
    }
    
    /**
     * Provide Fused Location Provider Client
     */
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
    
    /**
     * Provide EMFAD Location Service
     */
    @Provides
    @Singleton
    fun provideEMFADLocationService(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): EMFADLocationService {
        return EMFADLocationService(context, fusedLocationClient)
    }
    
    /**
     * Provide Database Service (existing)
     */
    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context): DatabaseService {
        return DatabaseService(context)
    }
    
    /**
     * Provide Analysis Service (existing)
     */
    @Provides
    @Singleton
    fun provideAnalysisService(): AnalysisService {
        return AnalysisService()
    }
}
