package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "invoices",
  indices = [
    Index(value = ["invoiceNumber"], unique = true),
    Index(value = ["customerId"])
  ]
)
data class InvoiceEntity(
  @PrimaryKey
  val id: String,
  val invoiceNumber: String,
  val customerId: String,
  val customerName: String,
  val customerPhone: String = "",
  val customerAddress: String = "",
  val invoiceDate: String,
  val invoiceDateMillis: Long = System.currentTimeMillis(),
  val dueDate: String = "",
  val businessName: String = "",
  val businessPhone: String = "",
  val businessAddress: String = "",
  val businessUpi: String = "",
  val subtotal: Double,
  val discount: Double = 0.0,
  val taxEnabled: Boolean = false,
  val taxRate: Double = 0.0,
  val taxAmount: Double = 0.0,
  val grandTotal: Double,
  val paidAmount: Double = 0.0,
  val remainingAmount: Double = 0.0,
  val paymentStatus: String = "PENDING",
  val notes: String = "",
  val createdTimestamp: Long = System.currentTimeMillis(),
  val updatedTimestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "PENDING",
  val userId: String = "",
  val isDeleted: Boolean = false
)
