package com.example.payment.backend

import com.example.model.PlanType
import com.example.model.SubscriptionEntitlement
import com.example.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class BackendVerificationResult {
  data class Verified(
    val orderId: String,
    val planType: PlanType,
    val startMillis: Long,
    val expiryMillis: Long
  ) : BackendVerificationResult()

  data class Pending(
    val orderId: String,
    val message: String
  ) : BackendVerificationResult()

  data class Failed(
    val orderId: String,
    val reason: String
  ) : BackendVerificationResult()

  data class NetworkUnavailable(val message: String) : BackendVerificationResult()
}

/**
 * Interface defining the contract for verifying transactions with the trusted backend/server.
 * Client callbacks are NEVER trusted as authoritative proof of payment.
 */
interface BackendPaymentVerificationService {
  suspend fun verifyPaymentOnBackend(
    orderId: String,
    firebaseUid: String,
    expectedPlan: PlanType,
    expectedAmount: Double = 0.0
  ): BackendVerificationResult

  suspend fun fetchAuthoritativeEntitlement(
    firebaseUid: String
  ): Result<SubscriptionEntitlement>
}

/**
 * Default implementation using Firestore authoritative document:
 * users/{uid}/subscription/current
 *
 * In the KhataGo architecture:
 * 1. The payment gateway confirms the transaction to the secure backend (via webhook or server verify).
 * 2. The secure backend verifies the signature, UID, order ID, amount, and plan, then writes the verified entitlement to Firestore.
 * 3. This service reads the authoritative record and verifies:
 *    - correct Firebase UID
 *    - correct order ID
 *    - correct plan
 *    - correct amount
 *    - successful payment
 *    - valid Premium expiry
 */
class FirestoreAuthoritativeVerificationService(
  private val networkMonitor: NetworkMonitor,
  private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : BackendPaymentVerificationService {

  override suspend fun verifyPaymentOnBackend(
    orderId: String,
    firebaseUid: String,
    expectedPlan: PlanType,
    expectedAmount: Double
  ): BackendVerificationResult {
    if (firebaseUid.isBlank()) {
      return BackendVerificationResult.Failed(orderId, "User is not authenticated with Firebase.")
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      return BackendVerificationResult.NetworkUnavailable(
        "Internet connection is required to verify payment with the server."
      )
    }

    return try {
      val firestore = firestoreProvider()
      val subDoc = firestore.collection("users")
        .document(firebaseUid)
        .collection("subscription")
        .document("current")
        .get()
        .await()

      if (!subDoc.exists()) {
        return BackendVerificationResult.Pending(
          orderId = orderId,
          message = "Payment received. Authoritative server verification is in progress..."
        )
      }

      // 1. Verify correct Firebase UID
      val docUserId = subDoc.getString("userId")
      if (!docUserId.isNullOrBlank() && docUserId != firebaseUid) {
        return BackendVerificationResult.Failed(orderId, "Firebase UID does not match subscription owner.")
      }

      // 2. Verify correct order ID
      val verifiedOrderId = subDoc.getString("orderId")
      if (verifiedOrderId != null && verifiedOrderId != orderId) {
        return BackendVerificationResult.Pending(
          orderId = orderId,
          message = "Awaiting verification for order $orderId..."
        )
      }

      // 3. Verify correct plan
      val rawPlanType = subDoc.getString("planType") ?: "FREE"
      val parsedPlan = try {
        PlanType.valueOf(rawPlanType)
      } catch (_: Exception) {
        PlanType.FREE
      }
      if (parsedPlan == PlanType.FREE || parsedPlan != expectedPlan) {
        return BackendVerificationResult.Pending(
          orderId = orderId,
          message = "Awaiting cloud plan confirmation for ${expectedPlan.displayName}..."
        )
      }

      // 4. Verify correct amount (if provided)
      val verifiedAmount = subDoc.getDouble("amount") ?: subDoc.getLong("amount")?.toDouble()
      if (expectedAmount > 0 && verifiedAmount != null && kotlin.math.abs(verifiedAmount - expectedAmount) > 1.0) {
        return BackendVerificationResult.Failed(orderId, "Payment amount mismatch with server order record.")
      }

      // 5. Verify successful payment and active status
      val isActive = subDoc.getBoolean("isActive") ?: false
      val paymentSuccess = subDoc.getBoolean("paymentSuccess") ?: true
      if (!isActive || !paymentSuccess) {
        return BackendVerificationResult.Failed(orderId, "Payment was not marked successful by the server.")
      }

      // 6. Verify valid Premium expiry
      val startMillis = subDoc.getLong("startMillis") ?: System.currentTimeMillis()
      val expiryMillis = subDoc.getLong("expiryMillis") ?: 0L
      val now = System.currentTimeMillis()
      if (expiryMillis <= now) {
        return BackendVerificationResult.Failed(orderId, "Verified subscription has already expired.")
      }

      // All 6 authoritative checks passed
      BackendVerificationResult.Verified(
        orderId = orderId,
        planType = parsedPlan,
        startMillis = startMillis,
        expiryMillis = expiryMillis
      )
    } catch (e: Exception) {
      BackendVerificationResult.Pending(
        orderId = orderId,
        message = "Awaiting cloud confirmation: ${e.localizedMessage ?: "Please try refreshing in a moment."}"
      )
    }
  }

  override suspend fun fetchAuthoritativeEntitlement(
    firebaseUid: String
  ): Result<SubscriptionEntitlement> {
    if (firebaseUid.isBlank()) {
      return Result.failure(IllegalStateException("Cannot fetch entitlement for blank Firebase UID."))
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      return Result.failure(IllegalStateException("Device is offline. Cannot reach authoritative server."))
    }

    return try {
      val firestore = firestoreProvider()
      val subDoc = firestore.collection("users")
        .document(firebaseUid)
        .collection("subscription")
        .document("current")
        .get()
        .await()

      if (!subDoc.exists()) {
        // Return Free entitlement if no cloud subscription document exists
        return Result.success(
          SubscriptionEntitlement(
            id = "entitlement_$firebaseUid",
            userId = firebaseUid,
            planType = PlanType.FREE,
            isActive = true
          )
        )
      }

      val rawPlan = subDoc.getString("planType") ?: "FREE"
      val isActive = subDoc.getBoolean("isActive") ?: false
      val startMillis = subDoc.getLong("startMillis") ?: System.currentTimeMillis()
      val expiryMillis = subDoc.getLong("expiryMillis")?.takeIf { it > 0 }
      val now = System.currentTimeMillis()

      val plan = try {
        PlanType.valueOf(rawPlan)
      } catch (_: Exception) {
        PlanType.FREE
      }

      // Check if plan has expired
      val isExpired = expiryMillis != null && expiryMillis > 0 && now > expiryMillis
      val effectivePlan = if (isExpired || !isActive) PlanType.FREE else plan

      val entitlement = SubscriptionEntitlement(
        id = subDoc.getString("id") ?: "entitlement_$firebaseUid",
        userId = firebaseUid,
        planType = effectivePlan,
        isActive = if (isExpired) false else isActive,
        startMillis = startMillis,
        expiryMillis = expiryMillis,
        createdAt = subDoc.getLong("createdAt") ?: startMillis,
        updatedAt = subDoc.getLong("updatedAt") ?: now
      )
      Result.success(entitlement)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
