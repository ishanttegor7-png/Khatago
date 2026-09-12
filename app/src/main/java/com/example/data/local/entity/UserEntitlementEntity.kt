package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.PlanType
import com.example.model.SubscriptionEntitlement

@Entity(
  tableName = "user_entitlements",
  indices = [Index(value = ["userId"])]
)
data class UserEntitlementEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val planType: String = "FREE",
  val isActive: Boolean = true,
  val startMillis: Long = System.currentTimeMillis(),
  val expiryMillis: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
) {
  fun toModel(): SubscriptionEntitlement {
    val plan = try {
      PlanType.valueOf(planType)
    } catch (e: Exception) {
      PlanType.FREE
    }
    return SubscriptionEntitlement(
      id = id,
      userId = userId,
      planType = plan,
      isActive = isActive,
      startMillis = startMillis,
      expiryMillis = expiryMillis,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }

  companion object {
    fun fromModel(model: SubscriptionEntitlement, syncStatus: String = "SYNCED"): UserEntitlementEntity {
      return UserEntitlementEntity(
        id = model.id,
        userId = model.userId,
        planType = model.planType.name,
        isActive = model.isActive,
        startMillis = model.startMillis,
        expiryMillis = model.expiryMillis,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
        syncStatus = syncStatus
      )
    }
  }
}
