package com.example.model

data class Customer(
  val id: String,
  val name: String,
  val phone: String = "",
  val address: String = "",
  val balance: Double = 0.0, // Positive = You will receive (Lena Hai), Negative = You will pay (Dena Hai)
  val totalCredit: Double = 0.0,
  val totalPayment: Double = 0.0,
  val transactionCount: Int = 0,
  val createdDate: Long = System.currentTimeMillis(),
  val updatedDate: Long = System.currentTimeMillis(),
  val lastUpdated: String = "Today",
  val avatarColorHex: Long = 0xFF0F766E
) {
  val isAllClear: Boolean get() = balance == 0.0
  val isLenaHai: Boolean get() = balance > 0.0
  val isDenaHai: Boolean get() = balance < 0.0
}
