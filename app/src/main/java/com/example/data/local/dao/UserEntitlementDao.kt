package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserEntitlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserEntitlementDao {

  @Query("SELECT * FROM user_entitlements WHERE userId = :userId LIMIT 1")
  fun getEntitlementForUser(userId: String): Flow<UserEntitlementEntity?>

  @Query("SELECT * FROM user_entitlements WHERE userId = :userId LIMIT 1")
  suspend fun getEntitlementForUserDirect(userId: String): UserEntitlementEntity?

  @Query("SELECT * FROM user_entitlements ORDER BY updatedAt DESC LIMIT 1")
  fun getLatestEntitlement(): Flow<UserEntitlementEntity?>

  @Query("SELECT * FROM user_entitlements ORDER BY updatedAt DESC LIMIT 1")
  suspend fun getLatestEntitlementDirect(): UserEntitlementEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entitlement: UserEntitlementEntity)

  @Query("DELETE FROM user_entitlements WHERE userId = :userId")
  suspend fun deleteForUser(userId: String)

  @Query("DELETE FROM user_entitlements")
  suspend fun deleteAll()
}
