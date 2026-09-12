package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val phone: String = "",
  val address: String = "",
  val createdDate: Long = System.currentTimeMillis(),
  val updatedDate: Long = System.currentTimeMillis(),
  val avatarColorHex: Long = 0xFF0F766E,
  val isArchived: Boolean = false,
  val syncStatus: String = "PENDING",
  val userId: String = "",
  val isDeleted: Boolean = false
)
