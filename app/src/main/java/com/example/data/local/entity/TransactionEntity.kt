package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "transactions",
  indices = [Index(value = ["customerId"]), Index(value = ["invoiceId"])]
)
data class TransactionEntity(
  @PrimaryKey
  val id: String,
  val customerId: String,
  val customerName: String,
  val type: String, // "CREDIT" or "PAYMENT"
  val amount: Double,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val dateFormatted: String = "",
  val invoiceId: String? = null,
  val syncStatus: String = "PENDING",
  val userId: String = "",
  val isDeleted: Boolean = false
)
