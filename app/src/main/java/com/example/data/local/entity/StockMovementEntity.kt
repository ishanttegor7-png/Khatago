package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.StockMovement
import com.example.model.StockMovementType

@Entity(
  tableName = "stock_movements",
  indices = [
    Index(value = ["productId"]),
    Index(value = ["referenceId"])
  ]
)
data class StockMovementEntity(
  @PrimaryKey
  val id: String,
  val productId: String,
  val productName: String = "",
  val quantity: Double,
  val movementType: String, // IN, OUT, ADJUSTMENT, INVOICE_SALE, INVOICE_CANCEL_RESTORE
  val reason: String = "",
  val referenceId: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "PENDING",
  val userId: String = "",
  val isDeleted: Boolean = false
) {
  fun toModel(): StockMovement = StockMovement(
    id = id,
    productId = productId,
    productName = productName,
    quantity = quantity,
    movementType = try {
      StockMovementType.valueOf(movementType)
    } catch (e: Exception) {
      StockMovementType.ADJUSTMENT
    },
    reason = reason,
    referenceId = referenceId,
    timestamp = timestamp
  )
}
