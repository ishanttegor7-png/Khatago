package com.example.model

enum class InvoicePaymentStatus {
  PAID,
  PENDING,
  PARTIALLY_PAID,
  CANCELLED;

  val displayName: String
    get() = when (this) {
      PAID -> "Paid"
      PENDING -> "Pending"
      PARTIALLY_PAID -> "Partially Paid"
      CANCELLED -> "Cancelled"
    }

  companion object {
    fun fromString(value: String): InvoicePaymentStatus {
      return when (value.uppercase()) {
        "PAID" -> PAID
        "PARTIALLY_PAID", "PARTIAL" -> PARTIALLY_PAID
        "CANCELLED", "CANCELED" -> CANCELLED
        else -> PENDING
      }
    }
  }
}

data class InvoiceItem(
  val id: String,
  val invoiceId: String,
  val productId: String? = null,
  val productName: String,
  val quantity: Int,
  val unitPrice: Double,
  val discount: Double = 0.0,
  val taxRate: Double = 0.0,
  val itemTotal: Double
)

data class Invoice(
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
  val items: List<InvoiceItem> = emptyList(),
  val subtotal: Double,
  val discount: Double = 0.0,
  val taxEnabled: Boolean = false,
  val taxRate: Double = 0.0,
  val taxAmount: Double = 0.0,
  val grandTotal: Double,
  val paidAmount: Double = 0.0,
  val remainingAmount: Double = 0.0,
  val paymentStatus: InvoicePaymentStatus = InvoicePaymentStatus.PENDING,
  val notes: String = "",
  val createdTimestamp: Long = System.currentTimeMillis(),
  val updatedTimestamp: Long = System.currentTimeMillis()
) {
  val isPaid: Boolean get() = paymentStatus == InvoicePaymentStatus.PAID
  val isPartiallyPaid: Boolean get() = paymentStatus == InvoicePaymentStatus.PARTIALLY_PAID
  val isPending: Boolean get() = paymentStatus == InvoicePaymentStatus.PENDING

  // Fallback for screens referencing single productName
  val productName: String
    get() = when {
      items.isEmpty() -> "General Invoice"
      items.size == 1 -> items.first().productName
      else -> "${items.first().productName} + ${items.size - 1} more"
    }

  val totalAmount: Double get() = grandTotal
  val date: String get() = invoiceDate
}
