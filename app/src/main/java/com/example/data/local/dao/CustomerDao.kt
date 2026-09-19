package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

  @Query("SELECT * FROM customers WHERE isArchived = 0 AND isDeleted = 0 ORDER BY updatedDate DESC")
  fun getAllCustomers(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isArchived = 0 AND isDeleted = 0 ORDER BY updatedDate DESC")
  fun getCustomersForUser(userId: String): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0 LIMIT 1")
  fun getCustomerById(id: String): Flow<CustomerEntity?>

  @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0 LIMIT 1")
  suspend fun getCustomerByIdDirect(id: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE phone = :phone AND isArchived = 0 AND isDeleted = 0 LIMIT 1")
  suspend fun findCustomerByPhone(phone: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE phone = :phone AND ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isArchived = 0 AND isDeleted = 0 LIMIT 1")
  suspend fun findCustomerByPhoneForUser(phone: String, userId: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isArchived = 0 AND isDeleted = 0 LIMIT 1")
  suspend fun findCustomerByName(name: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isArchived = 0 AND isDeleted = 0 LIMIT 1")
  suspend fun findCustomerByNameForUser(name: String, userId: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED'")
  suspend fun getPendingCustomers(): List<CustomerEntity>

  @Query("SELECT * FROM customers WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND syncStatus != 'SYNCED'")
  suspend fun getPendingCustomersForUser(userId: String): List<CustomerEntity>

  @Query("SELECT * FROM customers WHERE isDeleted = 0")
  suspend fun getAllCustomersDirect(): List<CustomerEntity>

  @Query("UPDATE customers SET syncStatus = :status WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: String)

  @Query("UPDATE customers SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedDate = :timestamp WHERE id = :id")
  suspend fun softDeleteCustomer(id: String, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE customers SET userId = :userId WHERE userId = '' OR userId IS NULL")
  suspend fun reassignCustomersToUser(userId: String)

  @Query("DELETE FROM customers")
  suspend fun deleteAllCustomers()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: CustomerEntity)

  @Update
  suspend fun updateCustomer(customer: CustomerEntity)

  @Delete
  suspend fun deleteCustomer(customer: CustomerEntity)

  @Query("DELETE FROM customers WHERE id = :id")
  suspend fun deleteCustomerById(id: String)

  @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
  suspend fun getCustomerCountDirect(): Int

  @Query("SELECT COUNT(*) FROM customers WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0")
  suspend fun getCustomerCountDirectForUser(userId: String): Int

  @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
  fun getCustomerCountFlow(): Flow<Int>

  @Query("SELECT COUNT(*) FROM customers WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0")
  fun getCustomerCountFlowForUser(userId: String): Flow<Int>
}
