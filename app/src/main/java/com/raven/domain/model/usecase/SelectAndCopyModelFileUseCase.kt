package com.raven.domain.model.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.raven.domain.model.ModelError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles Android file picker integration.
 * Converts user-selected content:// URI to a local file.
 * Essential for not requiring broad filesystem read permissions.
 */
class SelectAndCopyModelFileUseCase(
    private val context: Context
) {
    
    /**
     * Given a content URI from file picker, copy to a temporary location.
     * Returns the local file path suitable for import.
     * 
     * Usage:
     * 1. User selects file via DocumentPickerContract or SAF
     * 2. We get a content:// URI
     * 3. This use case copies it to a temp location
     * 4. ImportModelUseCase takes it from there
     */
    suspend fun copyUriToLocalFile(
        uri: Uri,
        displayName: String? = null
    ): File = withContext(Dispatchers.IO) {
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw ModelError.InvalidFile("Unsupported URI scheme: ${uri.scheme}")
        }
        
        val fileName = displayName ?: uri.lastPathSegment?.take(50) ?: "model.gguf"
        val tempFile = File(context.cacheDir, fileName)
        
        try {
            val contentResolver: ContentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw ModelError.CopyFailed("Could not open URI: $uri")
            
            return@withContext tempFile
        } catch (e: Exception) {
            tempFile.delete()
            throw ModelError.CopyFailed("Failed to copy file from URI: ${e.message}", e)
        }
    }
    
    /**
     * Get display name from a content URI.
     * Queries ContentResolver for the document's actual filename.
     */
    suspend fun getDisplayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val contentResolver: ContentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) it.getString(index) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get file size from a content URI.
     * Returns -1 if size cannot be determined.
     */
    suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val contentResolver: ContentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (index >= 0) it.getLong(index) else -1L
                } else {
                    -1L
                }
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
}
