package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Product

@Entity(
  tableName = "products",
  indices = [
    Index(value = ["sku"]),
    Index(value = ["category"])
  ]
)
data class ProductEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val sku: String = "",
  val category: String = "",
  val purchasePrice: Double = 0.0,
  val sellingPrice: Double = 0.0,
  val currentStock: Double = 0.0,
  val lowStockThreshold: Double = 5.0,
  val unit: String = "piece",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "PENDING",
  val userId: String = "",
  val isDeleted: Boolean = false
) {
  fun toModel(): Product = Product(
    id = id,
    name = name,
    sku = sku,
    category = category,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    currentStock = currentStock,
    lowStockThreshold = lowStockThreshold,
    unit = unit,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
  )
}
