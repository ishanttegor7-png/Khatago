package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

  @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0 ORDER BY timestamp DESC")
  fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY timestamp DESC")
  fun getTransactionsForCustomer(customerId: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE customerId = :customerId AND ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0 ORDER BY timestamp DESC")
  fun getTransactionsForCustomerAndUser(customerId: String, userId: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY timestamp ASC")
  suspend fun getTransactionsForCustomerAscending(customerId: String): List<TransactionEntity>

  @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED'")
  suspend fun getPendingTransactions(): List<TransactionEntity>

  @Query("SELECT * FROM transactions WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND syncStatus != 'SYNCED'")
  suspend fun getPendingTransactionsForUser(userId: String): List<TransactionEntity>

  @Query("SELECT * FROM transactions WHERE isDeleted = 0")
  suspend fun getAllTransactionsDirect(): List<TransactionEntity>

  @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: String)

  @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE id = :id")
  suspend fun softDeleteTransaction(id: String)

  @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE customerId = :customerId")
  suspend fun softDeleteTransactionsForCustomer(customerId: String)

  @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE invoiceId = :invoiceId")
  suspend fun softDeleteTransactionsForInvoice(invoiceId: String)

  @Query("UPDATE transactions SET userId = :userId WHERE userId = '' OR userId IS NULL")
  suspend fun reassignTransactionsToUser(userId: String)

  @Query("DELETE FROM transactions")
  suspend fun deleteAllTransactions()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity)

  @Delete
  suspend fun deleteTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE customerId = :customerId")
  suspend fun deleteTransactionsForCustomer(customerId: String)

  @Query("DELETE FROM transactions WHERE invoiceId = :invoiceId")
  suspend fun deleteTransactionsForInvoice(invoiceId: String)

  @Query("SELECT * FROM transactions WHERE invoiceId = :invoiceId")
  suspend fun getTransactionsForInvoice(invoiceId: String): List<TransactionEntity>

  @Query("SELECT * FROM transactions WHERE invoiceId = :invoiceId AND type = 'CREDIT' LIMIT 1")
  suspend fun getCreditTransactionForInvoice(invoiceId: String): TransactionEntity?

  @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND timestamp >= :sinceTimestamp")
  suspend fun getTransactionCountSince(sinceTimestamp: Long): Int

  @Query("SELECT COUNT(*) FROM transactions WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0 AND timestamp >= :sinceTimestamp")
  suspend fun getTransactionCountSinceForUser(sinceTimestamp: Long, userId: String): Int

  @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND timestamp >= :sinceTimestamp")
  fun getTransactionCountSinceFlow(sinceTimestamp: Long): Flow<Int>

  @Query("SELECT COUNT(*) FROM transactions WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0 AND timestamp >= :sinceTimestamp")
  fun getTransactionCountSinceFlowForUser(sinceTimestamp: Long, userId: String): Flow<Int>
}
