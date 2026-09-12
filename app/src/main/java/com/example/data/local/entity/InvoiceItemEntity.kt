package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "invoice_items",
  indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
  @PrimaryKey
  val id: String,
  val invoiceId: String,
  val productId: String? = null,
  val productName: String,
  val quantity: Int,
  val unitPrice: Double,
  val discount: Double = 0.0,
  val taxRate: Double = 0.0,
  val itemTotal: Double,
  val itemOrder: Int = 0
)
