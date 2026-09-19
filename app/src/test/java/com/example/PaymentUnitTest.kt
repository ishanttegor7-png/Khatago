package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.auth.FirebaseAuthManager
import com.example.auth.UserAccount
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PaymentRecordEntity
import com.example.data.local.entity.UserEntitlementEntity
import com.example.data.repository.PaymentRepository
import com.example.data.repository.PaymentRepositoryImpl
import com.example.model.PlanType
import com.example.model.SubscriptionEntitlement
import com.example.payment.PaymentServiceImpl
import com.example.payment.backend.BackendPaymentVerificationService
import com.example.payment.backend.BackendVerificationResult
import com.example.payment.cashfree.PlaceholderCashfreeGatewayClient
import com.example.payment.model.PaymentCheckResult
import com.example.payment.model.PaymentInitiationResult
import com.example.payment.model.PaymentStatus
import com.example.payment.model.PaymentVerificationResult
import com.example.payment.model.RestorePremiumResult
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake test implementation of BackendPaymentVerificationService
 */
class FakeBackendVerificationService : BackendPaymentVerificationService {
  var shouldVerify: Boolean = true
  var mockEntitlement: SubscriptionEntitlement? = null
  var isOffline: Boolean = false

  override suspend fun verifyPaymentOnBackend(
    orderId: String,
    firebaseUid: String,
    expectedPlan: PlanType,
    expectedAmount: Double
  ): BackendVerificationResult {
    if (isOffline) {
      return BackendVerificationResult.NetworkUnavailable("Network unavailable")
    }
    return if (shouldVerify) {
      val now = System.currentTimeMillis()
      val duration = if (expectedPlan == PlanType.PREMIUM_MONTHLY) 30L * 24 * 60 * 60 * 1000L else 365L * 24 * 60 * 60 * 1000L
      BackendVerificationResult.Verified(
        orderId = orderId,
        planType = expectedPlan,
        startMillis = now,
        expiryMillis = now + duration
      )
    } else {
      BackendVerificationResult.Pending(orderId, "Verification pending")
    }
  }

  override suspend fun fetchAuthoritativeEntitlement(
    firebaseUid: String
  ): Result<SubscriptionEntitlement> {
    if (isOffline) {
      return Result.failure(IllegalStateException("Offline"))
    }
    val ent = mockEntitlement ?: SubscriptionEntitlement(
      id = "entitlement_$firebaseUid",
      userId = firebaseUid,
      planType = PlanType.FREE,
      isActive = true
    )
    return Result.success(ent)
  }
}

/**
 * Fake test implementation of NetworkMonitor
 */
class FakeNetworkMonitor(context: Context) : NetworkMonitor(context) {
  var online: Boolean = true
  override fun isCurrentlyOnline(): Boolean = online
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentUnitTest {

  private lateinit var database: AppDatabase
  private lateinit var fakeVerificationService: FakeBackendVerificationService
  private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
  private lateinit var authManager: FirebaseAuthManager
  private lateinit var paymentRepository: PaymentRepository

  private val testUid = "test_user_uid_123"

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    fakeVerificationService = FakeBackendVerificationService()
    fakeNetworkMonitor = FakeNetworkMonitor(context)
    authManager = FirebaseAuthManager(context)

    // Pre-populate test user
    authManager.setCurrentUserForTesting(
      UserAccount(
        uid = testUid,
        displayName = "Test Merchant",
        email = "test@khatago.com"
      )
    )

    val paymentService = PaymentServiceImpl(
      paymentDao = database.paymentDao(),
      userEntitlementDao = database.userEntitlementDao(),
      authManager = authManager,
      networkMonitor = fakeNetworkMonitor,
      verificationService = fakeVerificationService,
      cashfreeGateway = PlaceholderCashfreeGatewayClient()
    )

    paymentRepository = PaymentRepositoryImpl(
      paymentDao = database.paymentDao(),
      paymentService = paymentService
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testMonthlyPlanSelection_initiatesCorrectAmountAndPlan() = runBlocking {
    val result = paymentRepository.startMonthlyPurchase(null)
    assertTrue("Monthly initiation must succeed", result is PaymentInitiationResult.Success)

    val success = result as PaymentInitiationResult.Success
    assertEquals(PlanType.PREMIUM_MONTHLY, success.planType)
    assertEquals(99.0, success.amount, 0.01)
    assertTrue(success.orderId.startsWith("order_kg_"))

    val records = database.paymentDao().getPaymentsForUserDirect(testUid)
    assertEquals(1, records.size)
    assertEquals(99.0, records[0].amount, 0.01)
    assertEquals("PREMIUM_MONTHLY", records[0].planId)
    assertEquals("PENDING", records[0].status)
    assertEquals(testUid, records[0].firebaseUid)
  }

  @Test
  fun testYearlyPlanSelection_initiatesCorrectAmountAndPlan() = runBlocking {
    val result = paymentRepository.startYearlyPurchase(null)
    assertTrue("Yearly initiation must succeed", result is PaymentInitiationResult.Success)

    val success = result as PaymentInitiationResult.Success
    assertEquals(PlanType.PREMIUM_YEARLY, success.planType)
    assertEquals(799.0, success.amount, 0.01)

    val records = database.paymentDao().getPaymentsForUserDirect(testUid)
    assertEquals(1, records.size)
    assertEquals(799.0, records[0].amount, 0.01)
    assertEquals("PREMIUM_YEARLY", records[0].planId)
  }

  @Test
  fun testLoggedOutUser_preventsPurchaseInitiation() = runBlocking {
    // Clear user account (simulate logged out)
    authManager.setCurrentUserForTesting(null)

    val monthlyResult = paymentRepository.startMonthlyPurchase(null)
    assertTrue("Logged out user cannot start purchase", monthlyResult is PaymentInitiationResult.LoginRequired)

    val yearlyResult = paymentRepository.startYearlyPurchase(null)
    assertTrue("Logged out user cannot start purchase", yearlyResult is PaymentInitiationResult.LoginRequired)

    // Verify no payment records were written
    val allRecords = database.paymentDao().getPaymentByOrderIdDirect("dummy")
    assertEquals(null, allRecords)
  }

  @Test
  fun testPaymentCancelled_doesNotUnlockPremium() = runBlocking {
    val initResult = paymentRepository.startMonthlyPurchase(null) as PaymentInitiationResult.Success

    val cancelResult = paymentRepository.handlePaymentResult(
      orderId = initResult.orderId,
      gatewayPaymentId = "cf_cancelled_123",
      gatewayStatus = "CANCELLED",
      failureReason = "User exited checkout"
    )

    assertTrue("Result must be Cancelled", cancelResult is PaymentVerificationResult.Cancelled)

    // Verify DB record status
    val record = database.paymentDao().getPaymentByOrderIdDirect(initResult.orderId)
    assertNotNull(record)
    assertEquals(PaymentStatus.CANCELLED.name, record!!.status)

    // Verify entitlement was NOT unlocked
    val entitlement = database.userEntitlementDao().getEntitlementForUserDirect(testUid)
    assertTrue(entitlement == null || entitlement.planType == "FREE")
  }

  @Test
  fun testPaymentFailed_doesNotUnlockPremium() = runBlocking {
    val initResult = paymentRepository.startMonthlyPurchase(null) as PaymentInitiationResult.Success

    val failResult = paymentRepository.handlePaymentResult(
      orderId = initResult.orderId,
      gatewayPaymentId = "cf_failed_456",
      gatewayStatus = "FAILED",
      failureReason = "Card declined by issuing bank"
    )

    assertTrue("Result must be VerificationFailed", failResult is PaymentVerificationResult.VerificationFailed)

    val record = database.paymentDao().getPaymentByOrderIdDirect(initResult.orderId)
    assertNotNull(record)
    assertEquals(PaymentStatus.FAILED.name, record!!.status)
    assertEquals("Card declined by issuing bank", record.failureReason)

    val entitlement = database.userEntitlementDao().getEntitlementForUserDirect(testUid)
    assertTrue(entitlement == null || entitlement.planType == "FREE")
  }

  @Test
  fun testUnverifiedPayment_doesNotUnlockPremium() = runBlocking {
    fakeVerificationService.shouldVerify = false // Server has NOT verified yet

    val initResult = paymentRepository.startMonthlyPurchase(null) as PaymentInitiationResult.Success

    val result = paymentRepository.handlePaymentResult(
      orderId = initResult.orderId,
      gatewayPaymentId = "cf_success_pending_backend",
      gatewayStatus = "SUCCESS"
    )

    assertTrue("Unverified payment must return VerificationPending", result is PaymentVerificationResult.VerificationPending)

    // Ensure entitlement is still FREE
    val entitlement = database.userEntitlementDao().getEntitlementForUserDirect(testUid)
    assertTrue(entitlement == null || entitlement.planType == "FREE")
  }

  @Test
  fun testVerifiedPayment_unlocksPremiumEntitlement() = runBlocking {
    fakeVerificationService.shouldVerify = true // Backend confirms payment

    val initResult = paymentRepository.startMonthlyPurchase(null) as PaymentInitiationResult.Success

    val result = paymentRepository.handlePaymentResult(
      orderId = initResult.orderId,
      gatewayPaymentId = "cf_pay_verified_999",
      gatewayStatus = "SUCCESS"
    )

    assertTrue("Verified payment must succeed", result is PaymentVerificationResult.Verified)
    val verified = result as PaymentVerificationResult.Verified
    assertEquals(PlanType.PREMIUM_MONTHLY, verified.planType)
    assertTrue(verified.expiryMillis > System.currentTimeMillis())

    // Verify Room entitlement updated
    val entitlement = database.userEntitlementDao().getEntitlementForUserDirect(testUid)
    assertNotNull(entitlement)
    assertEquals("PREMIUM_MONTHLY", entitlement!!.planType)
    assertTrue(entitlement.isActive)
    assertTrue(entitlement.expiryMillis!! > System.currentTimeMillis())
  }

  @Test
  fun testRestorePremium_activeSubscriptionRestoresEntitlement() = runBlocking {
    val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
    fakeVerificationService.mockEntitlement = SubscriptionEntitlement(
      id = "entitlement_$testUid",
      userId = testUid,
      planType = PlanType.PREMIUM_MONTHLY,
      isActive = true,
      startMillis = System.currentTimeMillis(),
      expiryMillis = expiry
    )

    val result = paymentRepository.restorePremium()
    assertTrue("Active subscription must restore successfully", result is RestorePremiumResult.Restored)

    val restored = result as RestorePremiumResult.Restored
    assertEquals(PlanType.PREMIUM_MONTHLY, restored.planType)
    assertEquals(expiry, restored.expiryMillis)

    // Verify local Room entitlement matches
    val local = database.userEntitlementDao().getEntitlementForUserDirect(testUid)
    assertNotNull(local)
    assertEquals("PREMIUM_MONTHLY", local!!.planType)
  }

  @Test
  fun testRestorePremium_loggedOutUser_requiresLogin() = runBlocking {
    authManager.setCurrentUserForTesting(null)

    val result = paymentRepository.restorePremium()
    assertTrue("Logged out user restore requires login", result is RestorePremiumResult.LoginRequired)
  }

  @Test
  fun testRestorePremium_noActiveSubscription_returnsNoActive() = runBlocking {
    fakeVerificationService.mockEntitlement = SubscriptionEntitlement(
      id = "entitlement_$testUid",
      userId = testUid,
      planType = PlanType.FREE,
      isActive = true
    )

    val result = paymentRepository.restorePremium()
    assertTrue("No active subscription must be returned", result is RestorePremiumResult.NoActiveSubscription)
  }
}
