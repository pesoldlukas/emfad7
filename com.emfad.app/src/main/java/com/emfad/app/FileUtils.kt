package com.emfad.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object FileUtils {

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use {\ inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error reading text from URI: ${e.message}", e)
            null
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(text.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("FileUtils", "Error writing text to URI: ${e.message}", e)
            false
        }
    }
}