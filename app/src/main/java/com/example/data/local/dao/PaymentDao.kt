package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PaymentRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(payment: PaymentRecordEntity)

  @Query("SELECT * FROM payment_records WHERE paymentId = :paymentId LIMIT 1")
  fun getPaymentById(paymentId: String): Flow<PaymentRecordEntity?>

  @Query("SELECT * FROM payment_records WHERE paymentId = :paymentId LIMIT 1")
  suspend fun getPaymentByIdDirect(paymentId: String): PaymentRecordEntity?

  @Query("SELECT * FROM payment_records WHERE orderId = :orderId LIMIT 1")
  suspend fun getPaymentByOrderIdDirect(orderId: String): PaymentRecordEntity?

  @Query("SELECT * FROM payment_records WHERE firebaseUid = :firebaseUid ORDER BY createdAt DESC")
  fun getPaymentsForUser(firebaseUid: String): Flow<List<PaymentRecordEntity>>

  @Query("SELECT * FROM payment_records WHERE firebaseUid = :firebaseUid ORDER BY createdAt DESC")
  suspend fun getPaymentsForUserDirect(firebaseUid: String): List<PaymentRecordEntity>

  @Query("SELECT * FROM payment_records WHERE firebaseUid = :firebaseUid AND status = 'VERIFIED' ORDER BY verifiedAt DESC LIMIT 1")
  suspend fun getLatestVerifiedPaymentDirect(firebaseUid: String): PaymentRecordEntity?

  @Query("SELECT * FROM payment_records WHERE firebaseUid = :firebaseUid AND status IN ('STARTED', 'PENDING', 'SUCCESS_UNVERIFIED') ORDER BY createdAt DESC")
  suspend fun getPendingPaymentsDirect(firebaseUid: String): List<PaymentRecordEntity>

  @Query("UPDATE payment_records SET status = :status, gatewayPaymentId = :gatewayPaymentId, failureReason = :failureReason WHERE orderId = :orderId")
  suspend fun updatePaymentStatus(
    orderId: String,
    status: String,
    gatewayPaymentId: String? = null,
    failureReason: String? = null
  )

  @Query("UPDATE payment_records SET status = 'VERIFIED', verifiedAt = :verifiedAt, premiumStartAt = :startAt, premiumExpiryAt = :expiryAt WHERE orderId = :orderId")
  suspend fun markPaymentVerified(
    orderId: String,
    verifiedAt: Long,
    startAt: Long,
    expiryAt: Long
  )
}
