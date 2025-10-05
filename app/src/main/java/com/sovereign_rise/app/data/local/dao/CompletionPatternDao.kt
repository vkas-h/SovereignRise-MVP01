package com.sovereign_rise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sovereign_rise.app.data.local.entity.CompletionPatternEntity

@Dao
interface CompletionPatternDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionPattern(pattern: CompletionPatternEntity)
    
    @Query("SELECT * FROM completion_patterns WHERE userId = :userId")
    suspend fun getCompletionPatterns(userId: String): List<CompletionPatternEntity>
    
    @Query("SELECT * FROM completion_patterns WHERE userId = :userId AND taskType = :taskType")
    suspend fun getCompletionPatternByType(userId: String, taskType: String?): CompletionPatternEntity?
    
    @Query("DELETE FROM completion_patterns WHERE userId = :userId AND taskType = :taskType")
    suspend fun deleteCompletionPattern(userId: String, taskType: String?)
}

