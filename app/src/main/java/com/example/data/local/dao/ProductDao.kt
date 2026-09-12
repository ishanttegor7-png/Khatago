package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

  @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY updatedAt DESC")
  fun getAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
  fun getActiveProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE isDeleted = 0 AND currentStock <= lowStockThreshold ORDER BY currentStock ASC")
  fun getLowStockProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0 LIMIT 1")
  fun getProductById(id: String): Flow<ProductEntity?>

  @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0 LIMIT 1")
  suspend fun getProductByIdSync(id: String): ProductEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProduct(product: ProductEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProducts(products: List<ProductEntity>)

  @Update
  suspend fun updateProduct(product: ProductEntity)

  @Query("UPDATE products SET currentStock = :newStock, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :productId")
  suspend fun updateStock(productId: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE products SET isActive = :isActive, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :productId")
  suspend fun updateActiveStatus(productId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE products SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
  suspend fun softDeleteProduct(id: String, timestamp: Long = System.currentTimeMillis())

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: String)

  @Query("SELECT * FROM products WHERE syncStatus IN ('PENDING', 'PENDING_DELETE')")
  suspend fun getPendingProducts(): List<ProductEntity>

  @Query("UPDATE products SET syncStatus = :status WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: String)

  @Query("UPDATE products SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = '' OR userId != :newUserId")
  suspend fun reassignProductsToUser(newUserId: String)

  @Query("DELETE FROM products")
  suspend fun deleteAllProducts()

  @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
  suspend fun getProductCountDirect(): Int

  @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
  fun getProductCountFlow(): Flow<Int>
}
