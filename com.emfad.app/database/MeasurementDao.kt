package com.emfad.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY timestamp ASC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>

    @Query("DELETE FROM measurements")
    suspend fun clearAllMeasurements()
}