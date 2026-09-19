package com.example.payment.model

import com.example.model.PlanType

enum class PaymentStatus {
  PENDING,
  SUCCESS_VERIFICATION_REQUIRED,
  VERIFIED,
  FAILED,
  CANCELLED,
  EXPIRED
}

/**
 * Immutable, audit-friendly representation of a KhataGo payment order and its verification lifecycle.
 */
data class PaymentRecord(
  val paymentId: String,
  val orderId: String,
  val firebaseUid: String,
  val planId: String,
  val amount: Double,
  val currency: String = "INR",
  val status: PaymentStatus,
  val gateway: String = "CASHFREE",
  val createdAt: Long = System.currentTimeMillis(),
  val verifiedAt: Long? = null,
  val premiumStartAt: Long? = null,
  val premiumExpiryAt: Long? = null,
  val gatewayPaymentId: String? = null,
  val failureReason: String? = null
) {
  val planType: PlanType
    get() = when (planId) {
      PlanType.PREMIUM_YEARLY.name, "PREMIUM_YEARLY" -> PlanType.PREMIUM_YEARLY
      PlanType.PREMIUM_MONTHLY.name, "PREMIUM_MONTHLY" -> PlanType.PREMIUM_MONTHLY
      else -> PlanType.FREE
    }

  val isVerified: Boolean
    get() = status == PaymentStatus.VERIFIED && verifiedAt != null
}

sealed class PaymentInitiationResult {
  data class Success(
    val orderId: String,
    val paymentId: String,
    val planType: PlanType,
    val amount: Double,
    val paymentSessionId: String? = null
  ) : PaymentInitiationResult()

  data class LoginRequired(val message: String) : PaymentInitiationResult()
  data class NetworkUnavailable(val message: String) : PaymentInitiationResult()
  data class AlreadyPremium(val message: String, val expiryMillis: Long?) : PaymentInitiationResult()
  data class Error(val message: String) : PaymentInitiationResult()
}

sealed class PaymentCheckResult {
  data class Verified(
    val orderId: String,
    val planType: PlanType,
    val expiryMillis: Long
  ) : PaymentCheckResult()

  data class PendingVerification(
    val orderId: String,
    val message: String
  ) : PaymentCheckResult()

  data class Failed(
    val orderId: String,
    val reason: String
  ) : PaymentCheckResult()

  data class Cancelled(val orderId: String) : PaymentCheckResult()
  data class NotFound(val orderId: String) : PaymentCheckResult()
  data class NetworkError(val message: String) : PaymentCheckResult()
}

sealed class PaymentVerificationResult {
  data class Verified(
    val orderId: String,
    val planType: PlanType,
    val startMillis: Long,
    val expiryMillis: Long
  ) : PaymentVerificationResult()

  data class VerificationPending(
    val orderId: String,
    val message: String
  ) : PaymentVerificationResult()

  data class VerificationFailed(
    val orderId: String,
    val reason: String
  ) : PaymentVerificationResult()

  data class Cancelled(
    val orderId: String,
    val reason: String = "Payment was cancelled by user."
  ) : PaymentVerificationResult()

  data class NetworkUnavailable(val message: String) : PaymentVerificationResult()
}

sealed class RestorePremiumResult {
  data class Restored(
    val planType: PlanType,
    val expiryMillis: Long
  ) : RestorePremiumResult()

  data class NoActiveSubscription(val message: String) : RestorePremiumResult()
  data class LoginRequired(val message: String) : RestorePremiumResult()
  data class NetworkUnavailable(val message: String) : RestorePremiumResult()
  data class Error(val message: String) : RestorePremiumResult()
}

sealed class PaymentUiState {
  object Idle : PaymentUiState()
  data class Processing(val message: String) : PaymentUiState()
  data class PaymentStarted(
    val orderId: String,
    val planType: PlanType,
    val amount: Double
  ) : PaymentUiState()
  data class AwaitingVerification(
    val orderId: String,
    val message: String
  ) : PaymentUiState()
  data class Success(
    val planType: PlanType,
    val expiryMillis: Long
  ) : PaymentUiState()
  data class Failed(val message: String) : PaymentUiState()
  object Cancelled : PaymentUiState()
  data class LoginRequired(val message: String) : PaymentUiState()
  data class Message(val title: String, val text: String) : PaymentUiState()
}
