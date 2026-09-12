package com.example.model

enum class TransactionType {
  CREDIT,   // Udhaar Diya / Gave Credit (Lena Hai)
  PAYMENT;  // Payment Mila / Customer Settled

  companion object {
    // Aliases for compatibility with screens
    val LENA_HAI: TransactionType get() = CREDIT
    val PAYMENT_RECEIVED: TransactionType get() = PAYMENT
    val DENA_HAI: TransactionType get() = CREDIT
    val PAYMENT_MADE: TransactionType get() = PAYMENT

    fun fromString(value: String): TransactionType {
      return when (value.uppercase()) {
        "PAYMENT", "PAYMENT_RECEIVED", "PAID" -> PAYMENT
        else -> CREDIT
      }
    }
  }
}

data class Transaction(
  val id: String,
  val customerId: String,
  val customerName: String,
  val type: TransactionType,
  val amount: Double,
  val note: String = "",
  val date: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val runningBalance: Double = 0.0,
  val isPaid: Boolean = false
) {
  val isCreditGiven: Boolean get() = type == TransactionType.CREDIT
  val isPayment: Boolean get() = type == TransactionType.PAYMENT
  val isDebit: Boolean get() = isPayment
}
