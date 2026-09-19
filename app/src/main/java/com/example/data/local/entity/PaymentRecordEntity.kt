package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.payment.model.PaymentRecord
import com.example.payment.model.PaymentStatus

@Entity(
  tableName = "payment_records",
  indices = [
    Index(value = ["orderId"], unique = true),
    Index(value = ["firebaseUid"]),
    Index(value = ["createdAt"])
  ]
)
data class PaymentRecordEntity(
  @PrimaryKey val paymentId: String,
  val orderId: String,
  val firebaseUid: String,
  val planId: String,
  val amount: Double,
  val currency: String = "INR",
  val status: String = "PENDING",
  val gateway: String = "CASHFREE",
  val createdAt: Long = System.currentTimeMillis(),
  val verifiedAt: Long? = null,
  val premiumStartAt: Long? = null,
  val premiumExpiryAt: Long? = null,
  val gatewayPaymentId: String? = null,
  val failureReason: String? = null
) {
  fun toModel(): PaymentRecord {
    val parsedStatus = when (status) {
      "STARTED" -> PaymentStatus.PENDING
      "SUCCESS_UNVERIFIED" -> PaymentStatus.SUCCESS_VERIFICATION_REQUIRED
      else -> try {
        PaymentStatus.valueOf(status)
      } catch (_: Exception) {
        PaymentStatus.FAILED
      }
    }
    return PaymentRecord(
      paymentId = paymentId,
      orderId = orderId,
      firebaseUid = firebaseUid,
      planId = planId,
      amount = amount,
      currency = currency,
      status = parsedStatus,
      gateway = gateway,
      createdAt = createdAt,
      verifiedAt = verifiedAt,
      premiumStartAt = premiumStartAt,
      premiumExpiryAt = premiumExpiryAt,
      gatewayPaymentId = gatewayPaymentId,
      failureReason = failureReason
    )
  }

  companion object {
    fun fromModel(model: PaymentRecord): PaymentRecordEntity {
      return PaymentRecordEntity(
        paymentId = model.paymentId,
        orderId = model.orderId,
        firebaseUid = model.firebaseUid,
        planId = model.planId,
        amount = model.amount,
        currency = model.currency,
        status = model.status.name,
        gateway = model.gateway,
        createdAt = model.createdAt,
        verifiedAt = model.verifiedAt,
        premiumStartAt = model.premiumStartAt,
        premiumExpiryAt = model.premiumExpiryAt,
        gatewayPaymentId = model.gatewayPaymentId,
        failureReason = model.failureReason
      )
    }
  }
}
