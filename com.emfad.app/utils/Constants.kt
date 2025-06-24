package com.emfad.app.utils

object Constants {
    const val DATABASE_NAME = "emfad_database"
    const val MEASUREMENT_TABLE_NAME = "measurements"

    // BLE Service and Characteristic UUIDs (placeholders - replace with actual device UUIDs)
    const val EMFAD_SERVICE_UUID = "0000180D-0000-1000-8000-00805f9b34fb" // Example: Heart Rate Service
    const val EMFAD_MEASUREMENT_CHARACTERISTIC_UUID = "00002A37-0000-1000-8000-00805f9b34fb" // Example: Heart Rate Measurement
    const val CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    // ARCore related constants
    const val AR_PLANE_DETECTION_ENABLED = true
    const val AR_OBJECT_SCALE_FACTOR = 0.1f // Scale factor for placing virtual objects

    // Export file names
    const val CSV_FILE_NAME = "emfad_measurements.csv"
    const val PDF_FILE_NAME = "emfad_report.pdf"
    const val MATLAB_FILE_NAME = "emfad_data.mat"

    // Settings defaults
    const val DEFAULT_MEASUREMENT_FREQUENCY = 1000L // milliseconds
    const val DEFAULT_FILTER_LEVEL = 5
}