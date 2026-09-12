package com.example.util

import com.example.model.InvoiceItem
import com.example.model.InvoicePaymentStatus
import java.math.BigDecimal
import java.math.RoundingMode

data class ItemCalculation(
  val grossTotal: Double,
  val discount: Double,
  val itemTotal: Double
)

data class InvoiceCalculationResult(
  val subtotal: Double,
  val discount: Double,
  val taxRate: Double,
  val taxAmount: Double,
  val grandTotal: Double,
  val paidAmount: Double,
  val remainingAmount: Double,
  val paymentStatus: InvoicePaymentStatus
)

object InvoiceCalculator {

  fun calculateItemTotal(quantity: Int, unitPrice: Double, discount: Double = 0.0): Double {
    val q = BigDecimal(quantity.coerceAtLeast(1))
    val price = BigDecimal.valueOf(unitPrice.coerceAtLeast(0.0))
    val disc = BigDecimal.valueOf(discount.coerceAtLeast(0.0))
    val gross = q.multiply(price)
    val net = gross.subtract(disc).max(BigDecimal.ZERO)
    return net.setScale(2, RoundingMode.HALF_UP).toDouble()
  }

  fun calculateInvoice(
    items: List<InvoiceItem>,
    overallDiscount: Double,
    taxEnabled: Boolean,
    taxRatePercent: Double,
    paidAmount: Double
  ): InvoiceCalculationResult {
    var subtotalBd = BigDecimal.ZERO
    for (item in items) {
      val itemTotalBd = BigDecimal.valueOf(item.itemTotal.coerceAtLeast(0.0))
      subtotalBd = subtotalBd.add(itemTotalBd)
    }
    subtotalBd = subtotalBd.setScale(2, RoundingMode.HALF_UP)

    // Overall discount cannot exceed subtotal
    val rawDiscountBd = BigDecimal.valueOf(overallDiscount.coerceAtLeast(0.0)).setScale(2, RoundingMode.HALF_UP)
    val effectiveDiscountBd = rawDiscountBd.min(subtotalBd)
    val taxableBd = subtotalBd.subtract(effectiveDiscountBd).max(BigDecimal.ZERO)

    val validTaxRate = if (taxEnabled) taxRatePercent.coerceIn(0.0, 100.0) else 0.0
    val taxAmountBd = if (taxEnabled && validTaxRate > 0.0) {
      taxableBd.multiply(BigDecimal.valueOf(validTaxRate))
        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    } else {
      BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    val grandTotalBd = taxableBd.add(taxAmountBd).setScale(2, RoundingMode.HALF_UP)

    val rawPaidBd = BigDecimal.valueOf(paidAmount.coerceAtLeast(0.0)).setScale(2, RoundingMode.HALF_UP)
    val effectivePaidBd = rawPaidBd.min(grandTotalBd)
    val remainingBd = grandTotalBd.subtract(effectivePaidBd).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)

    val status = when {
      grandTotalBd.compareTo(BigDecimal.ZERO) == 0 -> InvoicePaymentStatus.PAID
      remainingBd.compareTo(BigDecimal.ZERO) == 0 -> InvoicePaymentStatus.PAID
      effectivePaidBd.compareTo(BigDecimal.ZERO) > 0 -> InvoicePaymentStatus.PARTIALLY_PAID
      else -> InvoicePaymentStatus.PENDING
    }

    return InvoiceCalculationResult(
      subtotal = subtotalBd.toDouble(),
      discount = effectiveDiscountBd.toDouble(),
      taxRate = validTaxRate,
      taxAmount = taxAmountBd.toDouble(),
      grandTotal = grandTotalBd.toDouble(),
      paidAmount = effectivePaidBd.toDouble(),
      remainingAmount = remainingBd.toDouble(),
      paymentStatus = status
    )
  }
}
