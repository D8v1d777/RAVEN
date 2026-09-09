package com.raven.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raven.data.model.db.ModelDao
import com.raven.data.model.db.ModelEntity

/**
 * Room database for Raven.
 * Single database for all entities (models, conversations, messages, etc.).
 * Version should increment when schema changes.
 */
@Database(
    entities = [ModelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RavenDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    
    companion object {
        private var instance: RavenDatabase? = null
        private val LOCK = Any()
        
        /**
         * Get or create the database singleton.
         * Thread-safe using synchronized block.
         */
        fun getInstance(context: Context): RavenDatabase {
            return instance ?: synchronized(LOCK) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RavenDatabase::class.java,
                    "raven.db"
                ).build().also { instance = it }
            }
        }
    }
}
