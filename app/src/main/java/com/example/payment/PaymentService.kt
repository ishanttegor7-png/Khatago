package com.example.payment

import android.app.Activity
import com.example.auth.FirebaseAuthManager
import com.example.data.local.dao.PaymentDao
import com.example.data.local.dao.UserEntitlementDao
import com.example.data.local.entity.PaymentRecordEntity
import com.example.data.local.entity.UserEntitlementEntity
import com.example.model.PlanType
import com.example.model.SubscriptionEntitlement
import com.example.payment.backend.BackendPaymentVerificationService
import com.example.payment.backend.BackendVerificationResult
import com.example.payment.cashfree.CashfreeGatewayClient
import com.example.payment.cashfree.PlaceholderCashfreeGatewayClient
import com.example.payment.model.PaymentCheckResult
import com.example.payment.model.PaymentInitiationResult
import com.example.payment.model.PaymentRecord
import com.example.payment.model.PaymentStatus
import com.example.payment.model.PaymentUiState
import com.example.payment.model.PaymentVerificationResult
import com.example.payment.model.RestorePremiumResult
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface PaymentService {
  val uiState: StateFlow<PaymentUiState>
  fun resetUiState()

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
}

class PaymentServiceImpl(
  private val paymentDao: PaymentDao,
  private val userEntitlementDao: UserEntitlementDao,
  private val authManager: FirebaseAuthManager,
  private val networkMonitor: NetworkMonitor,
  private val verificationService: BackendPaymentVerificationService,
  private val cashfreeGateway: CashfreeGatewayClient = PlaceholderCashfreeGatewayClient()
) : PaymentService {

  private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
  override val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

  override fun resetUiState() {
    _uiState.value = PaymentUiState.Idle
  }

  override suspend fun startMonthlyPurchase(activity: Activity?): PaymentInitiationResult {
    return startPurchase(PlanType.PREMIUM_MONTHLY, 99.0, activity)
  }

  override suspend fun startYearlyPurchase(activity: Activity?): PaymentInitiationResult {
    return startPurchase(PlanType.PREMIUM_YEARLY, 799.0, activity)
  }

  private suspend fun startPurchase(
    planType: PlanType,
    amount: Double,
    activity: Activity?
  ): PaymentInitiationResult {
    val currentUser = authManager.currentUser.value
    if (currentUser == null || currentUser.uid.isBlank()) {
      _uiState.value = PaymentUiState.LoginRequired(
        "Please log in to your KhataGo account before subscribing to Premium. Subscriptions are securely linked to your account."
      )
      return PaymentInitiationResult.LoginRequired(
        "Please sign in to your KhataGo account before purchasing Premium."
      )
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      _uiState.value = PaymentUiState.Message(
        title = "No Internet Connection",
        text = "Internet connection is required to start payment."
      )
      return PaymentInitiationResult.NetworkUnavailable(
        "Internet connection is required to start payment."
      )
    }

    val timestamp = System.currentTimeMillis()
    val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(8)
    val orderId = "order_kg_${timestamp}_$randomSuffix"
    val paymentId = "pay_kg_${timestamp}_$randomSuffix"

    val record = PaymentRecordEntity(
      paymentId = paymentId,
      orderId = orderId,
      firebaseUid = currentUser.uid,
      planId = planType.name,
      amount = amount,
      currency = "INR",
      status = PaymentStatus.PENDING.name,
      gateway = "CASHFREE",
      createdAt = timestamp
    )
    paymentDao.insertOrUpdate(record)

    _uiState.value = PaymentUiState.PaymentStarted(orderId, planType, amount)

    return PaymentInitiationResult.Success(
      orderId = orderId,
      paymentId = paymentId,
      planType = planType,
      amount = amount,
      paymentSessionId = null
    )
  }

  override suspend fun handlePaymentResult(
    orderId: String,
    gatewayPaymentId: String?,
    gatewayStatus: String,
    failureReason: String?
  ): PaymentVerificationResult {
    val record = paymentDao.getPaymentByOrderIdDirect(orderId)
      ?: return PaymentVerificationResult.VerificationFailed(orderId, "Payment order not found.")

    // Prevent duplicate processing if already verified
    if (record.status == PaymentStatus.VERIFIED.name && record.verifiedAt != null) {
      val plan = record.toModel().planType
      val expiry = record.premiumExpiryAt ?: 0L
      _uiState.value = PaymentUiState.Success(plan, expiry)
      return PaymentVerificationResult.Verified(
        orderId = orderId,
        planType = plan,
        startMillis = record.premiumStartAt ?: record.createdAt,
        expiryMillis = expiry
      )
    }

    when (gatewayStatus.uppercase()) {
      "CANCELLED", "USER_DROPPED" -> {
        paymentDao.updatePaymentStatus(
          orderId = orderId,
          status = PaymentStatus.CANCELLED.name,
          gatewayPaymentId = gatewayPaymentId,
          failureReason = "Payment cancelled by user."
        )
        _uiState.value = PaymentUiState.Cancelled
        return PaymentVerificationResult.Cancelled(orderId)
      }

      "FAILED", "ERROR" -> {
        val errorMsg = failureReason ?: "Payment failed at gateway."
        paymentDao.updatePaymentStatus(
          orderId = orderId,
          status = PaymentStatus.FAILED.name,
          gatewayPaymentId = gatewayPaymentId,
          failureReason = errorMsg
        )
        _uiState.value = PaymentUiState.Failed(errorMsg)
        return PaymentVerificationResult.VerificationFailed(orderId, errorMsg)
      }

      "SUCCESS" -> {
        // CRITICAL: A gateway SUCCESS callback is NEVER trusted alone.
        // It transitions to SUCCESS_VERIFICATION_REQUIRED and requires authoritative server confirmation.
        paymentDao.updatePaymentStatus(
          orderId = orderId,
          status = PaymentStatus.SUCCESS_VERIFICATION_REQUIRED.name,
          gatewayPaymentId = gatewayPaymentId,
          failureReason = null
        )

        _uiState.value = PaymentUiState.AwaitingVerification(
          orderId = orderId,
          message = "Payment received. Contacting secure verification server..."
        )

        val planType = record.toModel().planType
        val backendResult = verificationService.verifyPaymentOnBackend(
          orderId = orderId,
          firebaseUid = record.firebaseUid,
          expectedPlan = planType,
          expectedAmount = record.amount
        )

        return when (backendResult) {
          is BackendVerificationResult.Verified -> {
            paymentDao.markPaymentVerified(
              orderId = orderId,
              verifiedAt = System.currentTimeMillis(),
              startAt = backendResult.startMillis,
              expiryAt = backendResult.expiryMillis
            )

            // Update Room database entitlement for the user
            val entitlement = UserEntitlementEntity(
              id = "entitlement_${record.firebaseUid}",
              userId = record.firebaseUid,
              planType = backendResult.planType.name,
              isActive = true,
              startMillis = backendResult.startMillis,
              expiryMillis = backendResult.expiryMillis,
              createdAt = System.currentTimeMillis(),
              updatedAt = System.currentTimeMillis(),
              syncStatus = "SYNCED"
            )
            userEntitlementDao.insertOrUpdate(entitlement)

            _uiState.value = PaymentUiState.Success(
              planType = backendResult.planType,
              expiryMillis = backendResult.expiryMillis
            )

            PaymentVerificationResult.Verified(
              orderId = orderId,
              planType = backendResult.planType,
              startMillis = backendResult.startMillis,
              expiryMillis = backendResult.expiryMillis
            )
          }

          is BackendVerificationResult.Pending -> {
            _uiState.value = PaymentUiState.AwaitingVerification(
              orderId = orderId,
              message = backendResult.message
            )
            PaymentVerificationResult.VerificationPending(orderId, backendResult.message)
          }

          is BackendVerificationResult.NetworkUnavailable -> {
            _uiState.value = PaymentUiState.Message(
              title = "Verification Pending",
              text = "Payment received, but server verification could not complete because the device is offline. Premium will unlock once internet is restored."
            )
            PaymentVerificationResult.NetworkUnavailable(backendResult.message)
          }

          is BackendVerificationResult.Failed -> {
            paymentDao.updatePaymentStatus(
              orderId = orderId,
              status = PaymentStatus.FAILED.name,
              gatewayPaymentId = gatewayPaymentId,
              failureReason = backendResult.reason
            )
            _uiState.value = PaymentUiState.Failed(backendResult.reason)
            PaymentVerificationResult.VerificationFailed(orderId, backendResult.reason)
          }
        }
      }

      else -> {
        paymentDao.updatePaymentStatus(
          orderId = orderId,
          status = PaymentStatus.PENDING.name,
          gatewayPaymentId = gatewayPaymentId,
          failureReason = null
        )
        _uiState.value = PaymentUiState.AwaitingVerification(
          orderId = orderId,
          message = "Payment status is pending gateway confirmation."
        )
        return PaymentVerificationResult.VerificationPending(
          orderId,
          "Payment status pending: $gatewayStatus"
        )
      }
    }
  }

  override suspend fun checkPaymentStatus(orderId: String): PaymentCheckResult {
    val record = paymentDao.getPaymentByOrderIdDirect(orderId)
      ?: return PaymentCheckResult.NotFound(orderId)

    if (record.status == PaymentStatus.VERIFIED.name && record.verifiedAt != null) {
      return PaymentCheckResult.Verified(
        orderId = orderId,
        planType = record.toModel().planType,
        expiryMillis = record.premiumExpiryAt ?: 0L
      )
    }

    if (record.status == PaymentStatus.CANCELLED.name) {
      return PaymentCheckResult.Cancelled(orderId)
    }

    if (record.status == PaymentStatus.FAILED.name) {
      return PaymentCheckResult.Failed(orderId, record.failureReason ?: "Payment failed.")
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      return PaymentCheckResult.NetworkError("Internet connection required to check status.")
    }

    val planType = record.toModel().planType
    val backendResult = verificationService.verifyPaymentOnBackend(
      orderId = orderId,
      firebaseUid = record.firebaseUid,
      expectedPlan = planType,
      expectedAmount = record.amount
    )

    return when (backendResult) {
      is BackendVerificationResult.Verified -> {
        paymentDao.markPaymentVerified(
          orderId = orderId,
          verifiedAt = System.currentTimeMillis(),
          startAt = backendResult.startMillis,
          expiryAt = backendResult.expiryMillis
        )
        val entitlement = UserEntitlementEntity(
          id = "entitlement_${record.firebaseUid}",
          userId = record.firebaseUid,
          planType = backendResult.planType.name,
          isActive = true,
          startMillis = backendResult.startMillis,
          expiryMillis = backendResult.expiryMillis,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          syncStatus = "SYNCED"
        )
        userEntitlementDao.insertOrUpdate(entitlement)
        PaymentCheckResult.Verified(orderId, backendResult.planType, backendResult.expiryMillis)
      }

      is BackendVerificationResult.Pending -> {
        PaymentCheckResult.PendingVerification(orderId, backendResult.message)
      }

      is BackendVerificationResult.NetworkUnavailable -> {
        PaymentCheckResult.NetworkError(backendResult.message)
      }

      is BackendVerificationResult.Failed -> {
        PaymentCheckResult.Failed(orderId, backendResult.reason)
      }
    }
  }

  override suspend fun restorePremium(): RestorePremiumResult {
    val currentUser = authManager.currentUser.value
    if (currentUser == null || currentUser.uid.isBlank()) {
      return RestorePremiumResult.LoginRequired(
        "Please sign in to restore your Premium subscription."
      )
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      return RestorePremiumResult.NetworkUnavailable(
        "Internet connection is required to restore purchases from the server."
      )
    }

    _uiState.value = PaymentUiState.Processing("Querying server for active subscriptions...")

    val result = verificationService.fetchAuthoritativeEntitlement(currentUser.uid)
    return if (result.isSuccess) {
      val entitlement = result.getOrNull()
      if (entitlement != null && entitlement.planType != PlanType.FREE && entitlement.isCurrentlyActive()) {
        userEntitlementDao.insertOrUpdate(UserEntitlementEntity.fromModel(entitlement))
        _uiState.value = PaymentUiState.Success(
          planType = entitlement.planType,
          expiryMillis = entitlement.expiryMillis ?: 0L
        )
        RestorePremiumResult.Restored(
          planType = entitlement.planType,
          expiryMillis = entitlement.expiryMillis ?: 0L
        )
      } else {
        // Subscription is expired or free
        if (entitlement != null) {
          userEntitlementDao.insertOrUpdate(
            UserEntitlementEntity.fromModel(entitlement.copy(planType = PlanType.FREE, isActive = false))
          )
        }
        _uiState.value = PaymentUiState.Message(
          title = "No Active Subscription",
          text = "No active KhataGo Premium subscription was found for this account."
        )
        RestorePremiumResult.NoActiveSubscription(
          "No active Premium subscription found for this account."
        )
      }
    } else {
      val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to reach server."
      _uiState.value = PaymentUiState.Failed(errorMsg)
      RestorePremiumResult.Error(errorMsg)
    }
  }
}
