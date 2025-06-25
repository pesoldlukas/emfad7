package com.emfad.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emfad.app.utils.Constants

@Entity(tableName = Constants.MEASUREMENT_TABLE_NAME)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val conductivity: Float,
    val materialType: String?,
    val clusterId: Int?
)