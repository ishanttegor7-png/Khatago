package com.example.payment.cashfree

import android.app.Activity

enum class CashfreeEnvironment {
  SANDBOX,
  PRODUCTION
}

/**
 * Configuration for Cashfree Payment Gateway integration.
 * IMPORTANT:
 * - DO NOT store merchant secret keys, API secret keys, or verification secrets in the Android app.
 * - Sensitive credentials remain strictly on the secure backend.
 * - The Android client only uses client-safe public identifiers and backend proxy endpoints.
 */
data class CashfreeConfig(
  val environment: CashfreeEnvironment = CashfreeEnvironment.SANDBOX,
  val appId: String = "CF_APP_ID_PLACEHOLDER", // Public Client App ID for Cashfree SDK
  val backendBaseUrl: String = "https://your-backend-api.com/api/payments", // URL to your trusted backend server
  val returnUrlScheme: String = "khatago://payment-return"
) {
  val isConfigured: Boolean
    get() = appId.isNotBlank() && appId != "CF_APP_ID_PLACEHOLDER" && backendBaseUrl.isNotBlank() && !backendBaseUrl.contains("your-backend-api.com")
}

sealed class CashfreeCheckoutResult {
  data class Success(
    val orderId: String,
    val paymentId: String?,
    val rawResponse: String? = null
  ) : CashfreeCheckoutResult()

  data class Failed(
    val orderId: String,
    val errorCode: String,
    val errorMessage: String
  ) : CashfreeCheckoutResult()

  data class Cancelled(val orderId: String) : CashfreeCheckoutResult()

  data class NotReady(val reason: String) : CashfreeCheckoutResult()
}

/**
 * Cashfree SDK integration contract.
 * When real Cashfree Android SDK is included, this interface adapts Cashfree's CFPaymentGatewayService.
 */
interface CashfreeGatewayClient {
  fun isReady(): Boolean
  suspend fun createPaymentSession(orderId: String, amount: Double, firebaseUid: String): Result<String>
  suspend fun launchCheckout(
    activity: Activity?,
    orderId: String,
    paymentSessionId: String
  ): CashfreeCheckoutResult
}

/**
 * Default placeholder implementation of CashfreeGatewayClient.
 * Fails safely when Cashfree SDK is not yet linked or credentials are placeholders.
 */
class PlaceholderCashfreeGatewayClient(
  private val config: CashfreeConfig = CashfreeConfig()
) : CashfreeGatewayClient {

  override fun isReady(): Boolean = config.isConfigured

  override suspend fun createPaymentSession(
    orderId: String,
    amount: Double,
    firebaseUid: String
  ): Result<String> {
    if (!isReady()) {
      return Result.failure(
        IllegalStateException("Cashfree is not yet configured. Please configure your Cashfree App ID and backend URL.")
      )
    }
    // In production, your Android app calls your trusted backend which uses the Cashfree Secret Key
    // to generate a paymentSessionId from Cashfree PG Orders API.
    return Result.failure(
      IllegalStateException("Backend endpoint for Cashfree order creation is not configured.")
    )
  }

  override suspend fun launchCheckout(
    activity: Activity?,
    orderId: String,
    paymentSessionId: String
  ): CashfreeCheckoutResult {
    return CashfreeCheckoutResult.NotReady(
      "Cashfree Android SDK is not configured with live credentials."
    )
  }
}
