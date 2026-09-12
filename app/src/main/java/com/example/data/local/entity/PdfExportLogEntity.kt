package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "pdf_export_logs",
  indices = [Index(value = ["timestamp"])]
)
data class PdfExportLogEntity(
  @PrimaryKey val id: String,
  val timestamp: Long = System.currentTimeMillis(),
  val exportType: String = "PDF", // "INVOICE_PDF", "BUSINESS_REPORT_PDF", etc.
  val userId: String = ""
)
