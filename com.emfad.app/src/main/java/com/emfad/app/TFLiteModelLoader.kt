package com.emfad.app.ai

import android.content.Context
import android.util.Log
import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModelLoader(private val context: Context) {

    private var interpreter: Interpreter? = null

    /**
     * Loads a TensorFlow Lite model from the specified path in the assets folder.
     * @param modelPath The path to the TFLite model file within the assets directory.
     * @return The loaded Interpreter instance, or null if loading failed.
     */
    fun loadModel(modelPath: String): Interpreter? {
        try {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            // options.setNumThreads(4) // Example: set number of threads for better performance
            interpreter = Interpreter(modelBuffer, options)
            Log.d("TFLiteModelLoader", "TensorFlow Lite model loaded successfully from $modelPath.")
            return interpreter
        } catch (e: IOException) {
            Log.e("TFLiteModelLoader", "IOException while loading TFLite model from $modelPath: ${e.message}", e)
            return null
        } catch (e: OutOfMemoryError) {
            Log.e("TFLiteModelLoader", "OutOfMemoryError while loading TFLite model from $modelPath: ${e.message}. Model might be too large.", e)
            return null
        } catch (e: Exception) {
            Log.e("TFLiteModelLoader", "An unexpected error occurred while loading TFLite model from $modelPath: ${e.message}", e)
            return null
        }
    }

    /**
     * Runs inference on the loaded model.
     * @param input The input data for the model.
     * @param output The output buffer for the model's predictions.
     * @return True if inference was successful, false otherwise.
     */
    fun runInference(input: Any, output: Any): Boolean {
        return try {
            interpreter?.run(input, output)
            true
        } catch (e: Exception) {
            Log.e("TFLiteModelLoader", "Error running inference: ${e.message}", e)
            false
        }
    }

    /**
     * Closes the TensorFlow Lite interpreter and releases resources.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d("TFLiteModelLoader", "TensorFlow Lite interpreter closed.")
    }
}