package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PdfExportLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfExportLogDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: PdfExportLogEntity)

  @Query("SELECT COUNT(*) FROM pdf_export_logs WHERE timestamp >= :sinceTimestamp")
  suspend fun getCountSince(sinceTimestamp: Long): Int

  @Query("SELECT COUNT(*) FROM pdf_export_logs WHERE timestamp >= :sinceTimestamp")
  fun getCountSinceFlow(sinceTimestamp: Long): Flow<Int>

  @Query("DELETE FROM pdf_export_logs")
  suspend fun deleteAllLogs()
}
