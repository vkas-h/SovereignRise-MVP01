package com.sovereign_rise.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sovereign_rise.app.data.local.dao.*
import com.sovereign_rise.app.data.local.entity.*

/**
 * Room database for app data storage (AI features + core app data with offline support)
 */
@Database(
    entities = [
        // AI Features entities
        UsageStatsEntity::class,
        UsagePatternEntity::class,
        CompletionPatternEntity::class,
        BurnoutMetricsEntity::class,
        // Core app entities with offline support
        TaskEntity::class,
        HabitEntity::class,
        UserEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class UsageStatsDatabase : RoomDatabase() {
    
    // AI Features DAOs
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun usagePatternDao(): UsagePatternDao
    abstract fun completionPatternDao(): CompletionPatternDao
    abstract fun burnoutMetricsDao(): BurnoutMetricsDao
    
    // Core app DAOs
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao
    
    companion object {
        @Volatile
        private var INSTANCE: UsageStatsDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            // Old migration - keeping for backward compatibility
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration - no longer used
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration from v2 to v3: Remove item-related tables and columns
                
                // Drop item tables
                database.execSQL("DROP TABLE IF EXISTS items")
                database.execSQL("DROP TABLE IF EXISTS user_items")
                
                // Remove XP/Aether columns from tasks if they exist
                // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table
                database.execSQL("DROP TABLE IF EXISTS tasks")
                database.execSQL("""
                    CREATE TABLE tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        status TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        reminderTime INTEGER,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        isMissed INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        lastSyncedAt INTEGER,
                        serverUpdatedAt INTEGER,
                        localUpdatedAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX index_tasks_userId ON tasks(userId)")
                database.execSQL("CREATE INDEX index_tasks_status ON tasks(status)")
                database.execSQL("CREATE INDEX index_tasks_createdAt ON tasks(createdAt)")
                
                // Recreate user_profile without level/xp/aether/crown/title
                database.execSQL("DROP TABLE IF EXISTS user_profile")
                database.execSQL("""
                    CREATE TABLE user_profile (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        streakDays INTEGER NOT NULL,
                        profileImageUrl TEXT,
                        syncStatus TEXT NOT NULL,
                        lastSyncedAt INTEGER,
                        serverUpdatedAt INTEGER,
                        localUpdatedAt INTEGER NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """)
            }
        }
        
        fun getInstance(context: Context): UsageStatsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): UsageStatsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UsageStatsDatabase::class.java,
                "sovereign_rise_database"
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration() // Allow destructive migration if needed
                .build()
        }
    }
}

