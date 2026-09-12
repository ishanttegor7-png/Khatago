package com.example.model

data class Product(
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
  val updatedAt: Long = System.currentTimeMillis()
) {
  val isLowStock: Boolean get() = currentStock <= lowStockThreshold
  val profitMargin: Double get() = (sellingPrice - purchasePrice).coerceAtLeast(0.0)
}
