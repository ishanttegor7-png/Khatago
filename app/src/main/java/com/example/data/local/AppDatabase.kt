package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.PdfExportLogDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserEntitlementDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.PdfExportLogEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntitlementEntity

@Database(
  entities = [
    CustomerEntity::class,
    TransactionEntity::class,
    InvoiceEntity::class,
    InvoiceItemEntity::class,
    ProductEntity::class,
    StockMovementEntity::class,
    UserEntitlementEntity::class,
    PdfExportLogEntity::class
  ],
  version = 5,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun customerDao(): CustomerDao
  abstract fun transactionDao(): TransactionDao
  abstract fun invoiceDao(): InvoiceDao
  abstract fun productDao(): ProductDao
  abstract fun stockMovementDao(): StockMovementDao
  abstract fun userEntitlementDao(): UserEntitlementDao
  abstract fun pdfExportLogDao(): PdfExportLogDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE customers ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE customers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE invoices ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE invoices ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE invoices ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // Add productId column to invoice_items
        db.execSQL("ALTER TABLE invoice_items ADD COLUMN productId TEXT DEFAULT NULL")

        // Create products table
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS products (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            sku TEXT NOT NULL DEFAULT '',
            category TEXT NOT NULL DEFAULT '',
            purchasePrice REAL NOT NULL DEFAULT 0.0,
            sellingPrice REAL NOT NULL DEFAULT 0.0,
            currentStock REAL NOT NULL DEFAULT 0.0,
            lowStockThreshold REAL NOT NULL DEFAULT 5.0,
            unit TEXT NOT NULL DEFAULT 'piece',
            isActive INTEGER NOT NULL DEFAULT 1,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncStatus TEXT NOT NULL DEFAULT 'PENDING',
            userId TEXT NOT NULL DEFAULT '',
            isDeleted INTEGER NOT NULL DEFAULT 0
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_sku ON products(sku)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_category ON products(category)")

        // Create stock_movements table
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS stock_movements (
            id TEXT NOT NULL PRIMARY KEY,
            productId TEXT NOT NULL,
            productName TEXT NOT NULL DEFAULT '',
            quantity REAL NOT NULL,
            movementType TEXT NOT NULL,
            reason TEXT NOT NULL DEFAULT '',
            referenceId TEXT NOT NULL DEFAULT '',
            timestamp INTEGER NOT NULL,
            syncStatus TEXT NOT NULL DEFAULT 'PENDING',
            userId TEXT NOT NULL DEFAULT '',
            isDeleted INTEGER NOT NULL DEFAULT 0
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_referenceId ON stock_movements(referenceId)")
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // Create user_entitlements table
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS user_entitlements (
            id TEXT NOT NULL PRIMARY KEY,
            userId TEXT NOT NULL,
            planType TEXT NOT NULL DEFAULT 'FREE',
            isActive INTEGER NOT NULL DEFAULT 1,
            startMillis INTEGER NOT NULL,
            expiryMillis INTEGER,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_entitlements_userId ON user_entitlements(userId)")

        // Create pdf_export_logs table
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS pdf_export_logs (
            id TEXT NOT NULL PRIMARY KEY,
            timestamp INTEGER NOT NULL,
            exportType TEXT NOT NULL DEFAULT 'PDF',
            userId TEXT NOT NULL DEFAULT ''
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pdf_export_logs_timestamp ON pdf_export_logs(timestamp)")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "khatago_database"
        )
          .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .fallbackToDestructiveMigration(false)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
