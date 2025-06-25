package com.emfad.app.services

import android.util.Log
import com.emfad.app.database.AppDatabase
import com.emfad.app.database.MeasurementEntity
import com.emfad.app.models.EMFADMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatabaseService(private val database: AppDatabase) {

    private val measurementDao = database.measurementDao()

    suspend fun saveMeasurement(measurement: EMFADMeasurement) {
        val entity = MeasurementEntity(
            timestamp = measurement.timestamp,
            x = measurement.x,
            y = measurement.y,
            z = measurement.z,
            conductivity = measurement.conductivity,
            materialType = measurement.materialType?.name,
            clusterId = measurement.clusterId
        )
        measurementDao.insertMeasurement(entity)
        Log.d("DatabaseService", "Saved measurement to DB: $measurement")
    }

    fun getAllMeasurements(): Flow<List<EMFADMeasurement>> {
        return measurementDao.getAllMeasurements().map {
            it.map { entity ->
                EMFADMeasurement(
                    timestamp = entity.timestamp,
                    x = entity.x,
                    y = entity.y,
                    z = entity.z,
                    conductivity = entity.conductivity,
                    materialType = entity.materialType?.let { name -> com.emfad.app.models.MaterialType.valueOf(name) },
                    clusterId = entity.clusterId
                )
            }
        }
    }

    suspend fun clearAllMeasurements() {
        measurementDao.clearAllMeasurements()
        Log.d("DatabaseService", "Cleared all measurements from DB.")
    }
}