package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoice(invoice: InvoiceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

  @Update
  suspend fun updateInvoice(invoice: InvoiceEntity)

  @Query("DELETE FROM invoices WHERE id = :invoiceId")
  suspend fun deleteInvoiceById(invoiceId: String)

  @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
  suspend fun deleteItemsForInvoice(invoiceId: String)

  @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY createdTimestamp DESC")
  fun getAllInvoices(): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoice_items ORDER BY itemOrder ASC")
  fun getAllInvoiceItems(): Flow<List<InvoiceItemEntity>>

  @Query("SELECT * FROM invoices WHERE id = :invoiceId AND isDeleted = 0 LIMIT 1")
  fun getInvoiceById(invoiceId: String): Flow<InvoiceEntity?>

  @Query("SELECT * FROM invoices WHERE id = :invoiceId AND isDeleted = 0 LIMIT 1")
  suspend fun getInvoiceByIdSync(invoiceId: String): InvoiceEntity?

  @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY itemOrder ASC")
  fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItemEntity>>

  @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY itemOrder ASC")
  suspend fun getItemsForInvoiceSync(invoiceId: String): List<InvoiceItemEntity>

  @Query("SELECT * FROM invoices WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdTimestamp DESC")
  fun getInvoicesForCustomer(customerId: String): Flow<List<InvoiceEntity>>

  @Query("SELECT invoiceNumber FROM invoices WHERE isDeleted = 0")
  suspend fun getAllInvoiceNumbers(): List<String>

  @Query("SELECT * FROM invoices WHERE syncStatus != 'SYNCED'")
  suspend fun getPendingInvoices(): List<InvoiceEntity>

  @Query("SELECT * FROM invoices WHERE isDeleted = 0")
  suspend fun getAllInvoicesDirect(): List<InvoiceEntity>

  @Query("UPDATE invoices SET syncStatus = :status WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: String)

  @Query("UPDATE invoices SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedTimestamp = :timestamp WHERE id = :id")
  suspend fun softDeleteInvoice(id: String, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE invoices SET userId = :userId WHERE userId = '' OR userId IS NULL")
  suspend fun reassignInvoicesToUser(userId: String)

  @Query("DELETE FROM invoices")
  suspend fun deleteAllInvoices()

  @Query("DELETE FROM invoice_items")
  suspend fun deleteAllInvoiceItems()

  @Query("SELECT COUNT(*) FROM invoices WHERE isDeleted = 0 AND createdTimestamp >= :sinceTimestamp")
  suspend fun getInvoiceCountSince(sinceTimestamp: Long): Int

  @Query("SELECT COUNT(*) FROM invoices WHERE isDeleted = 0 AND createdTimestamp >= :sinceTimestamp")
  fun getInvoiceCountSinceFlow(sinceTimestamp: Long): Flow<Int>
}
