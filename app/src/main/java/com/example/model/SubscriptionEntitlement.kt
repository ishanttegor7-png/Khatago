package com.example.model

import java.util.Calendar

enum class PlanType(val displayName: String, val priceDisplay: String, val billingPeriod: String) {
  FREE("Free", "₹0", "Forever Free"),
  PREMIUM_MONTHLY("Premium Monthly", "₹99/month", "Billed monthly"),
  PREMIUM_YEARLY("Premium Yearly", "₹799/year", "Billed annually")
}

data class PlanLimits(
  val maxCustomers: Int,
  val maxProducts: Int,
  val maxInvoicesPerMonth: Int,
  val maxKhataTransactionsPerMonth: Int,
  val maxPdfExportsPerMonth: Int,
  val isCsvExportAllowed: Boolean,
  val isAdvancedReportsAllowed: Boolean,
  val isAdsRemoved: Boolean
) {
  companion object {
    val FREE_LIMITS = PlanLimits(
      maxCustomers = 50,
      maxProducts = 100,
      maxInvoicesPerMonth = 50,
      maxKhataTransactionsPerMonth = 200,
      maxPdfExportsPerMonth = 10,
      isCsvExportAllowed = false,
      isAdvancedReportsAllowed = false,
      isAdsRemoved = false
    )

    val PREMIUM_LIMITS = PlanLimits(
      maxCustomers = 500,
      maxProducts = 1000,
      maxInvoicesPerMonth = 500,
      maxKhataTransactionsPerMonth = 2000,
      maxPdfExportsPerMonth = Int.MAX_VALUE, // Unlimited
      isCsvExportAllowed = true,
      isAdvancedReportsAllowed = true,
      isAdsRemoved = true
    )

    fun forPlan(planType: PlanType): PlanLimits = when (planType) {
      PlanType.FREE -> FREE_LIMITS
      PlanType.PREMIUM_MONTHLY, PlanType.PREMIUM_YEARLY -> PREMIUM_LIMITS
    }
  }
}

data class SubscriptionEntitlement(
  val id: String = "entitlement_default",
  val userId: String = "",
  val planType: PlanType = PlanType.FREE,
  val isActive: Boolean = true,
  val startMillis: Long = System.currentTimeMillis(),
  val expiryMillis: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun isExpired(currentMillis: Long = System.currentTimeMillis()): Boolean {
    if (planType == PlanType.FREE) return false
    val expiry = expiryMillis ?: return false
    return expiry > 0 && currentMillis > expiry
  }

  fun isCurrentlyActive(currentMillis: Long = System.currentTimeMillis()): Boolean {
    if (!isActive) return false
    if (planType == PlanType.FREE) return true
    return !isExpired(currentMillis)
  }

  fun getEffectivePlan(currentMillis: Long = System.currentTimeMillis()): PlanType {
    return if (isCurrentlyActive(currentMillis)) planType else PlanType.FREE
  }

  fun getLimits(currentMillis: Long = System.currentTimeMillis()): PlanLimits {
    return PlanLimits.forPlan(getEffectivePlan(currentMillis))
  }
}

data class PlanUsageMetrics(
  val customerCount: Int = 0,
  val maxCustomers: Int = PlanLimits.FREE_LIMITS.maxCustomers,
  val productCount: Int = 0,
  val maxProducts: Int = PlanLimits.FREE_LIMITS.maxProducts,
  val monthlyInvoiceCount: Int = 0,
  val maxMonthlyInvoices: Int = PlanLimits.FREE_LIMITS.maxInvoicesPerMonth,
  val monthlyTransactionCount: Int = 0,
  val maxMonthlyTransactions: Int = PlanLimits.FREE_LIMITS.maxKhataTransactionsPerMonth,
  val monthlyPdfExportCount: Int = 0,
  val maxMonthlyPdfExports: Int = PlanLimits.FREE_LIMITS.maxPdfExportsPerMonth,
  val isCsvExportAllowed: Boolean = false,
  val isAdvancedReportsAllowed: Boolean = false,
  val planType: PlanType = PlanType.FREE,
  val isPremiumActive: Boolean = false,
  val expiryMillis: Long? = null
) {
  val isCustomerLimitReached: Boolean get() = customerCount >= maxCustomers
  val isProductLimitReached: Boolean get() = productCount >= maxProducts
  val isInvoiceLimitReached: Boolean get() = monthlyInvoiceCount >= maxMonthlyInvoices
  val isTransactionLimitReached: Boolean get() = monthlyTransactionCount >= maxMonthlyTransactions
  val isPdfExportLimitReached: Boolean get() = monthlyPdfExportCount >= maxMonthlyPdfExports
}

object MonthDateUtils {
  fun getStartOfCurrentMonthMillis(calendar: Calendar = Calendar.getInstance()): Long {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }
}
