package com.emfad.app.di

import android.app.Application
import android.content.Context
import com.emfad.app.bluetooth.EMFADBluetoothManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideBluetoothManager(app: Application): EMFADBluetoothManager {
        return EMFADBluetoothManager(app.applicationContext)
    }
}