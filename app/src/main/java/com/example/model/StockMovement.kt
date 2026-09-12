package com.example.model

enum class StockMovementType {
  IN,
  OUT,
  ADJUSTMENT,
  INVOICE_SALE,
  INVOICE_CANCEL_RESTORE;

  val displayName: String
    get() = when (this) {
      IN -> "Stock In (+)"
      OUT -> "Stock Out (-)"
      ADJUSTMENT -> "Adjustment"
      INVOICE_SALE -> "Sold in Invoice (-)"
      INVOICE_CANCEL_RESTORE -> "Invoice Cancelled (+)"
    }
}

data class StockMovement(
  val id: String,
  val productId: String,
  val productName: String = "",
  val quantity: Double,
  val movementType: StockMovementType,
  val reason: String = "",
  val referenceId: String = "",
  val timestamp: Long = System.currentTimeMillis()
)
