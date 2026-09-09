package com.raven.data.model.storage

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Implementation of ModelPathResolver using Android Context.
 * Models are stored in app-private persistent storage (filesDir).
 */
class AndroidModelPathResolver(
    private val context: Context
) : ModelPathResolver {
    
    override fun getModelsRootDir(): File {
        // Use context.filesDir/models for persistent app-private storage
        // Alternative: context.cacheDir/models if cache is acceptable
        val dir = File(context.filesDir, "models")
        dir.mkdirs()
        return dir
    }
    
    override fun getAvailableStorage(): Long {
        return try {
            val stat = StatFs(getModelsRootDir().absolutePath)
            // AvailableBlocks * BlockSize = available bytes
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBlocksLong * stat.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    override fun getTotalStorage(): Long {
        return try {
            val stat = StatFs(getModelsRootDir().absolutePath)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.blockCountLong * stat.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                stat.blockCount.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
}
