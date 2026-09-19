package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

  @Query("SELECT * FROM stock_movements WHERE isDeleted = 0 ORDER BY timestamp DESC")
  fun getAllMovements(): Flow<List<StockMovementEntity>>

  @Query("SELECT * FROM stock_movements WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND isDeleted = 0 ORDER BY timestamp DESC")
  fun getMovementsForUser(userId: String): Flow<List<StockMovementEntity>>

  @Query("SELECT * FROM stock_movements WHERE productId = :productId AND isDeleted = 0 ORDER BY timestamp DESC")
  fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

  @Query("SELECT * FROM stock_movements WHERE productId = :productId AND isDeleted = 0 ORDER BY timestamp DESC")
  suspend fun getMovementsForProductSync(productId: String): List<StockMovementEntity>

  @Query("SELECT * FROM stock_movements WHERE referenceId = :referenceId AND isDeleted = 0")
  suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovementEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMovement(movement: StockMovementEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMovements(movements: List<StockMovementEntity>)

  @Query("DELETE FROM stock_movements WHERE referenceId = :referenceId")
  suspend fun deleteMovementsByReferenceId(referenceId: String)

  @Query("DELETE FROM stock_movements WHERE id = :id")
  suspend fun deleteMovementById(id: String)

  @Query("SELECT * FROM stock_movements WHERE syncStatus IN ('PENDING', 'PENDING_DELETE')")
  suspend fun getPendingMovements(): List<StockMovementEntity>

  @Query("SELECT * FROM stock_movements WHERE ((:userId = '' AND (userId = '' OR userId IS NULL)) OR (:userId != '' AND userId = :userId)) AND syncStatus IN ('PENDING', 'PENDING_DELETE')")
  suspend fun getPendingMovementsForUser(userId: String): List<StockMovementEntity>

  @Query("UPDATE stock_movements SET syncStatus = :status WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: String)

  @Query("UPDATE stock_movements SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = '' OR userId IS NULL")
  suspend fun reassignMovementsToUser(newUserId: String)

  @Query("DELETE FROM stock_movements")
  suspend fun deleteAllMovements()
}
