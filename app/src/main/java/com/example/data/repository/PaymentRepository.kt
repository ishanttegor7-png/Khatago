package com.example.data.repository

import android.app.Activity
import com.example.data.local.dao.PaymentDao
import com.example.payment.PaymentService
import com.example.payment.model.PaymentCheckResult
import com.example.payment.model.PaymentInitiationResult
import com.example.payment.model.PaymentRecord
import com.example.payment.model.PaymentUiState
import com.example.payment.model.PaymentVerificationResult
import com.example.payment.model.RestorePremiumResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface PaymentRepository {
  val paymentUiState: StateFlow<PaymentUiState>
  fun getPaymentsForUser(firebaseUid: String): Flow<List<PaymentRecord>>
  suspend fun startMonthlyPurchase(activity: Activity? = null): PaymentInitiationResult
  suspend fun startYearlyPurchase(activity: Activity? = null): PaymentInitiationResult
  suspend fun checkPaymentStatus(orderId: String): PaymentCheckResult
  suspend fun restorePremium(): RestorePremiumResult
  suspend fun handlePaymentResult(
    orderId: String,
    gatewayPaymentId: String?,
    gatewayStatus: String,
    failureReason: String? = null
  ): PaymentVerificationResult
  fun resetUiState()
}

class PaymentRepositoryImpl(
  private val paymentDao: PaymentDao,
  private val paymentService: PaymentService
) : PaymentRepository {

  override val paymentUiState: StateFlow<PaymentUiState> = paymentService.uiState

  override fun getPaymentsForUser(firebaseUid: String): Flow<List<PaymentRecord>> {
    return paymentDao.getPaymentsForUser(firebaseUid).map { list ->
      list.map { it.toModel() }
    }
  }

  override suspend fun startMonthlyPurchase(activity: Activity?): PaymentInitiationResult {
    return paymentService.startMonthlyPurchase(activity)
  }

  override suspend fun startYearlyPurchase(activity: Activity?): PaymentInitiationResult {
    return paymentService.startYearlyPurchase(activity)
  }

  override suspend fun checkPaymentStatus(orderId: String): PaymentCheckResult {
    return paymentService.checkPaymentStatus(orderId)
  }

  override suspend fun restorePremium(): RestorePremiumResult {
    return paymentService.restorePremium()
  }

  override suspend fun handlePaymentResult(
    orderId: String,
    gatewayPaymentId: String?,
    gatewayStatus: String,
    failureReason: String?
  ): PaymentVerificationResult {
    return paymentService.handlePaymentResult(orderId, gatewayPaymentId, gatewayStatus, failureReason)
  }

  override fun resetUiState() {
    paymentService.resetUiState()
  }
}
