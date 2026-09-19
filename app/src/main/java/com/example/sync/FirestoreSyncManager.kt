package com.example.sync

import android.content.Context
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserEntitlementDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntitlementEntity
import com.example.model.BusinessProfile
import com.example.util.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncStatus {
  SYNCED,
  SYNCING,
  PENDING,
  OFFLINE,
  FAILED,
  NOT_CONFIGURED
}

data class SyncSummary(
  val customersUploaded: Int = 0,
  val transactionsUploaded: Int = 0,
  val invoicesUploaded: Int = 0,
  val productsUploaded: Int = 0,
  val movementsUploaded: Int = 0,
  val timestamp: Long = System.currentTimeMillis()
)

data class RestoreSummary(
  val customersRestored: Int = 0,
  val transactionsRestored: Int = 0,
  val invoicesRestored: Int = 0,
  val productsRestored: Int = 0,
  val movementsRestored: Int = 0,
  val profileRestored: Boolean = false,
  val profile: BusinessProfile? = null
)

class FirestoreSyncManager(
  private val context: Context,
  private val customerDao: CustomerDao,
  private val transactionDao: TransactionDao,
  private val invoiceDao: InvoiceDao,
  private val productDao: ProductDao,
  private val stockMovementDao: StockMovementDao,
  private val networkMonitor: NetworkMonitor,
  private val userEntitlementDao: UserEntitlementDao? = null
) {

  private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
  val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
  val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

  private val _lastSyncMessage = MutableStateFlow<String>("")
  val lastSyncMessage: StateFlow<String> = _lastSyncMessage.asStateFlow()

  fun isFirebaseConfigured(): Boolean {
    return try {
      if (FirebaseApp.getApps(context).isEmpty()) return false
      val app = FirebaseApp.getInstance()
      val apiKey = app.options.apiKey
      apiKey.isNotBlank() &&
        !apiKey.startsWith("AIzaSyD-KhataGo") &&
        apiKey != "dummy_api_key" &&
        !apiKey.contains("placeholder", ignoreCase = true)
    } catch (e: Exception) {
      false
    }
  }

  suspend fun syncPending(uid: String, currentProfile: BusinessProfile? = null): Result<SyncSummary> =
    withContext(Dispatchers.IO) {
      if (uid.isBlank()) {
        _syncStatus.value = SyncStatus.PENDING
        return@withContext Result.failure(IllegalStateException("No authenticated user account."))
      }

      if (!networkMonitor.isCurrentlyOnline()) {
        _syncStatus.value = SyncStatus.OFFLINE
        _lastSyncMessage.value = "Offline — changes stored safely on device."
        return@withContext Result.failure(Exception("Internet is not available. Stored locally."))
      }

      _syncStatus.value = SyncStatus.SYNCING
      _lastSyncMessage.value = "Syncing changes to cloud..."

      if (!isFirebaseConfigured()) {
        _syncStatus.value = SyncStatus.NOT_CONFIGURED
        _lastSyncMessage.value = "Local Storage Active: All records safely saved on device."
        _lastSyncTimestamp.value = System.currentTimeMillis()
        return@withContext Result.success(SyncSummary(timestamp = System.currentTimeMillis()))
      }

      try {
        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(uid)

        // 1. Sync Customers
        val pendingCustomers = customerDao.getPendingCustomersForUser(uid)
        var custCount = 0
        for (cust in pendingCustomers) {
          val docRef = userDoc.collection("customers").document(cust.id)
          if (cust.isDeleted) {
            docRef.delete().await()
            customerDao.deleteCustomerById(cust.id)
          } else {
            val map = hashMapOf(
              "id" to cust.id,
              "name" to cust.name,
              "phone" to cust.phone,
              "address" to cust.address,
              "createdDate" to cust.createdDate,
              "updatedDate" to cust.updatedDate,
              "avatarColorHex" to cust.avatarColorHex,
              "isArchived" to cust.isArchived,
              "isDeleted" to false,
              "userId" to uid
            )
            docRef.set(map, SetOptions.merge()).await()
            customerDao.updateSyncStatus(cust.id, "SYNCED")
          }
          custCount++
        }

        // 2. Sync Transactions
        val pendingTransactions = transactionDao.getPendingTransactionsForUser(uid)
        var txCount = 0
        for (tx in pendingTransactions) {
          val docRef = userDoc.collection("transactions").document(tx.id)
          if (tx.isDeleted) {
            docRef.delete().await()
            transactionDao.deleteTransaction(tx)
          } else {
            val map = hashMapOf(
              "id" to tx.id,
              "customerId" to tx.customerId,
              "customerName" to tx.customerName,
              "type" to tx.type,
              "amount" to tx.amount,
              "note" to tx.note,
              "timestamp" to tx.timestamp,
              "dateFormatted" to tx.dateFormatted,
              "invoiceId" to (tx.invoiceId ?: ""),
              "isDeleted" to false,
              "userId" to uid
            )
            docRef.set(map, SetOptions.merge()).await()
            transactionDao.updateSyncStatus(tx.id, "SYNCED")
          }
          txCount++
        }

        // 3. Sync Invoices
        val pendingInvoices = invoiceDao.getPendingInvoicesForUser(uid)
        var invCount = 0
        for (inv in pendingInvoices) {
          val docRef = userDoc.collection("invoices").document(inv.id)
          if (inv.isDeleted) {
            docRef.delete().await()
            invoiceDao.deleteInvoiceById(inv.id)
            invoiceDao.deleteItemsForInvoice(inv.id)
          } else {
            val items = invoiceDao.getItemsForInvoiceSync(inv.id)
            val itemsList = items.map { item ->
              hashMapOf(
                "id" to item.id,
                "productName" to item.productName,
                "quantity" to item.quantity,
                "unitPrice" to item.unitPrice,
                "discount" to item.discount,
                "taxRate" to item.taxRate,
                "itemTotal" to item.itemTotal,
                "itemOrder" to item.itemOrder
              )
            }
            val map = hashMapOf(
              "id" to inv.id,
              "invoiceNumber" to inv.invoiceNumber,
              "customerId" to inv.customerId,
              "customerName" to inv.customerName,
              "customerPhone" to inv.customerPhone,
              "customerAddress" to inv.customerAddress,
              "invoiceDate" to inv.invoiceDate,
              "invoiceDateMillis" to inv.invoiceDateMillis,
              "dueDate" to inv.dueDate,
              "businessName" to inv.businessName,
              "businessPhone" to inv.businessPhone,
              "businessAddress" to inv.businessAddress,
              "businessUpi" to inv.businessUpi,
              "subtotal" to inv.subtotal,
              "discount" to inv.discount,
              "taxEnabled" to inv.taxEnabled,
              "taxRate" to inv.taxRate,
              "taxAmount" to inv.taxAmount,
              "grandTotal" to inv.grandTotal,
              "paidAmount" to inv.paidAmount,
              "remainingAmount" to inv.remainingAmount,
              "paymentStatus" to inv.paymentStatus,
              "notes" to inv.notes,
              "createdTimestamp" to inv.createdTimestamp,
              "updatedTimestamp" to inv.updatedTimestamp,
              "items" to itemsList,
              "isDeleted" to false,
              "userId" to uid
            )
            docRef.set(map, SetOptions.merge()).await()
            invoiceDao.updateSyncStatus(inv.id, "SYNCED")
          }
          invCount++
        }

        // 4. Sync Products
        val pendingProducts = productDao.getPendingProductsForUser(uid)
        var prodCount = 0
        for (prod in pendingProducts) {
          val docRef = userDoc.collection("products").document(prod.id)
          if (prod.syncStatus == "PENDING_DELETE") {
            docRef.delete().await()
            productDao.deleteProductById(prod.id)
          } else {
            val map = hashMapOf(
              "id" to prod.id,
              "name" to prod.name,
              "sku" to prod.sku,
              "category" to prod.category,
              "purchasePrice" to prod.purchasePrice,
              "sellingPrice" to prod.sellingPrice,
              "currentStock" to prod.currentStock,
              "lowStockThreshold" to prod.lowStockThreshold,
              "unit" to prod.unit,
              "isActive" to prod.isActive,
              "createdAt" to prod.createdAt,
              "updatedAt" to prod.updatedAt,
              "isDeleted" to false,
              "userId" to uid
            )
            docRef.set(map, SetOptions.merge()).await()
            productDao.updateSyncStatus(prod.id, "SYNCED")
          }
          prodCount++
        }

        // 5. Sync Stock Movements
        val pendingMovements = stockMovementDao.getPendingMovementsForUser(uid)
        var moveCount = 0
        for (move in pendingMovements) {
          val docRef = userDoc.collection("stock_movements").document(move.id)
          if (move.syncStatus == "PENDING_DELETE") {
            docRef.delete().await()
            stockMovementDao.deleteMovementById(move.id)
          } else {
            val map = hashMapOf(
              "id" to move.id,
              "productId" to move.productId,
              "productName" to move.productName,
              "quantity" to move.quantity,
              "movementType" to move.movementType,
              "reason" to move.reason,
              "referenceId" to move.referenceId,
              "timestamp" to move.timestamp,
              "isDeleted" to false,
              "userId" to uid
            )
            docRef.set(map, SetOptions.merge()).await()
            stockMovementDao.updateSyncStatus(move.id, "SYNCED")
          }
          moveCount++
        }

        // 6. Sync Profile
        currentProfile?.let { prof ->
          val profMap = hashMapOf(
            "businessName" to prof.businessName,
            "ownerName" to prof.ownerName,
            "phone" to prof.phone,
            "address" to prof.address,
            "upiId" to prof.upiId,
            "updatedTimestamp" to System.currentTimeMillis()
          )
          userDoc.collection("profile").document("main").set(profMap, SetOptions.merge()).await()
        }

        // 7. Sync Subscription Entitlement (Safe: Server is authoritative, client never writes itself as PREMIUM)
        if (userEntitlementDao != null) {
          try {
            val subDoc = userDoc.collection("subscription").document("current")
            val existingCloudSub = subDoc.get().await()
            if (!existingCloudSub.exists()) {
              // Only initialize if non-existent in cloud, strictly as FREE
              val freeMap = hashMapOf(
                "id" to "entitlement_$uid",
                "userId" to uid,
                "planType" to "FREE",
                "isActive" to true,
                "startMillis" to System.currentTimeMillis(),
                "expiryMillis" to 0L,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "syncStatus" to "SYNCED"
              )
              subDoc.set(freeMap, SetOptions.merge()).await()
            }
          } catch (_: Exception) {}
        }

        _syncStatus.value = SyncStatus.SYNCED
        _lastSyncTimestamp.value = System.currentTimeMillis()
        _lastSyncMessage.value = "All records backed up to cloud."
        Result.success(SyncSummary(custCount, txCount, invCount, prodCount, moveCount))
      } catch (e: Exception) {
        _syncStatus.value = SyncStatus.FAILED
        _lastSyncMessage.value = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
        Result.failure(e)
      }
    }

  suspend fun restoreUserData(uid: String): Result<RestoreSummary> = withContext(Dispatchers.IO) {
    if (uid.isBlank()) {
      return@withContext Result.failure(IllegalStateException("No authenticated user account to restore from."))
    }

    if (!networkMonitor.isCurrentlyOnline()) {
      return@withContext Result.failure(Exception("Cannot restore while offline. Please connect to internet."))
    }

    if (!isFirebaseConfigured()) {
      _syncStatus.value = SyncStatus.NOT_CONFIGURED
      _lastSyncMessage.value = "Local Storage Active: All records loaded from device database."
      _lastSyncTimestamp.value = System.currentTimeMillis()
      return@withContext Result.success(RestoreSummary())
    }

    _syncStatus.value = SyncStatus.SYNCING
    _lastSyncMessage.value = "Restoring cloud data..."

    try {
      val firestore = FirebaseFirestore.getInstance()
      val userDoc = firestore.collection("users").document(uid)

      // 1. Fetch Customers
      val custSnapshot = userDoc.collection("customers").get().await()
      var custCount = 0
      for (doc in custSnapshot.documents) {
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        if (isDeleted) continue

        val customer = CustomerEntity(
          id = doc.getString("id") ?: doc.id,
          name = doc.getString("name") ?: "Customer",
          phone = doc.getString("phone") ?: "",
          address = doc.getString("address") ?: "",
          createdDate = doc.getLong("createdDate") ?: System.currentTimeMillis(),
          updatedDate = doc.getLong("updatedDate") ?: System.currentTimeMillis(),
          avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF0F766E,
          isArchived = doc.getBoolean("isArchived") ?: false,
          syncStatus = "SYNCED",
          userId = uid,
          isDeleted = false
        )
        customerDao.insertCustomer(customer)
        custCount++
      }

      // 2. Fetch Transactions
      val txSnapshot = userDoc.collection("transactions").get().await()
      var txCount = 0
      for (doc in txSnapshot.documents) {
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        if (isDeleted) continue

        val tx = TransactionEntity(
          id = doc.getString("id") ?: doc.id,
          customerId = doc.getString("customerId") ?: "",
          customerName = doc.getString("customerName") ?: "",
          type = doc.getString("type") ?: "CREDIT",
          amount = doc.getDouble("amount") ?: 0.0,
          note = doc.getString("note") ?: "",
          timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
          dateFormatted = doc.getString("dateFormatted") ?: "",
          invoiceId = doc.getString("invoiceId").takeIf { !it.isNullOrBlank() },
          syncStatus = "SYNCED",
          userId = uid,
          isDeleted = false
        )
        transactionDao.insertTransaction(tx)
        txCount++
      }

      // 3. Fetch Invoices
      val invSnapshot = userDoc.collection("invoices").get().await()
      var invCount = 0
      for (doc in invSnapshot.documents) {
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        if (isDeleted) continue

        val invId = doc.getString("id") ?: doc.id
        val invoice = InvoiceEntity(
          id = invId,
          invoiceNumber = doc.getString("invoiceNumber") ?: "INV-0001",
          customerId = doc.getString("customerId") ?: "",
          customerName = doc.getString("customerName") ?: "",
          customerPhone = doc.getString("customerPhone") ?: "",
          customerAddress = doc.getString("customerAddress") ?: "",
          invoiceDate = doc.getString("invoiceDate") ?: "",
          invoiceDateMillis = doc.getLong("invoiceDateMillis") ?: System.currentTimeMillis(),
          dueDate = doc.getString("dueDate") ?: "",
          businessName = doc.getString("businessName") ?: "",
          businessPhone = doc.getString("businessPhone") ?: "",
          businessAddress = doc.getString("businessAddress") ?: "",
          businessUpi = doc.getString("businessUpi") ?: "",
          subtotal = doc.getDouble("subtotal") ?: 0.0,
          discount = doc.getDouble("discount") ?: 0.0,
          taxEnabled = doc.getBoolean("taxEnabled") ?: false,
          taxRate = doc.getDouble("taxRate") ?: 0.0,
          taxAmount = doc.getDouble("taxAmount") ?: 0.0,
          grandTotal = doc.getDouble("grandTotal") ?: 0.0,
          paidAmount = doc.getDouble("paidAmount") ?: 0.0,
          remainingAmount = doc.getDouble("remainingAmount") ?: 0.0,
          paymentStatus = doc.getString("paymentStatus") ?: "PENDING",
          notes = doc.getString("notes") ?: "",
          createdTimestamp = doc.getLong("createdTimestamp") ?: System.currentTimeMillis(),
          updatedTimestamp = doc.getLong("updatedTimestamp") ?: System.currentTimeMillis(),
          syncStatus = "SYNCED",
          userId = uid,
          isDeleted = false
        )
        invoiceDao.insertInvoice(invoice)

        // Restore Items
        @Suppress("UNCHECKED_CAST")
        val itemsRaw = doc.get("items") as? List<Map<String, Any>>
        if (itemsRaw != null) {
          val items = itemsRaw.mapIndexed { idx, map ->
            InvoiceItemEntity(
              id = (map["id"] as? String) ?: "${invId}_item_$idx",
              invoiceId = invId,
              productName = (map["productName"] as? String) ?: "Item",
              quantity = ((map["quantity"] as? Number)?.toInt()) ?: 1,
              unitPrice = ((map["unitPrice"] as? Number)?.toDouble()) ?: 0.0,
              discount = ((map["discount"] as? Number)?.toDouble()) ?: 0.0,
              taxRate = ((map["taxRate"] as? Number)?.toDouble()) ?: 0.0,
              itemTotal = ((map["itemTotal"] as? Number)?.toDouble()) ?: 0.0,
              itemOrder = ((map["itemOrder"] as? Number)?.toInt()) ?: idx
            )
          }
          invoiceDao.insertInvoiceItems(items)
        }
        invCount++
      }

      // 4. Restore Products
      val prodDocs = userDoc.collection("products").get().await()
      var prodCount = 0
      for (doc in prodDocs) {
        val prodId = doc.getString("id") ?: doc.id
        val product = ProductEntity(
          id = prodId,
          name = doc.getString("name") ?: "Product",
          sku = doc.getString("sku") ?: "",
          category = doc.getString("category") ?: "",
          purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
          sellingPrice = doc.getDouble("sellingPrice") ?: 0.0,
          currentStock = doc.getDouble("currentStock") ?: 0.0,
          lowStockThreshold = doc.getDouble("lowStockThreshold") ?: 5.0,
          unit = doc.getString("unit") ?: "piece",
          isActive = doc.getBoolean("isActive") ?: true,
          createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
          updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
          syncStatus = "SYNCED",
          userId = uid,
          isDeleted = doc.getBoolean("isDeleted") ?: false
        )
        productDao.insertProduct(product)
        prodCount++
      }

      // 5. Restore Stock Movements
      val moveDocs = userDoc.collection("stock_movements").get().await()
      var moveCount = 0
      for (doc in moveDocs) {
        val moveId = doc.getString("id") ?: doc.id
        val movement = StockMovementEntity(
          id = moveId,
          productId = doc.getString("productId") ?: "",
          productName = doc.getString("productName") ?: "",
          quantity = doc.getDouble("quantity") ?: 0.0,
          movementType = doc.getString("movementType") ?: "ADJUSTMENT",
          reason = doc.getString("reason") ?: "",
          referenceId = doc.getString("referenceId") ?: "",
          timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
          syncStatus = "SYNCED",
          userId = uid,
          isDeleted = doc.getBoolean("isDeleted") ?: false
        )
        stockMovementDao.insertMovement(movement)
        moveCount++
      }

      // 6. Restore Subscription Entitlement
      if (userEntitlementDao != null) {
        try {
          val subDoc = userDoc.collection("subscription").document("current").get().await()
          if (subDoc.exists()) {
            val rawPlan = subDoc.getString("planType") ?: "FREE"
            val expiry = subDoc.getLong("expiryMillis")?.takeIf { it > 0 }
            val now = System.currentTimeMillis()
            val isExpired = expiry != null && now > expiry
            val isPlanActive = (subDoc.getBoolean("isActive") ?: true) && !isExpired
            val effectivePlanType = if (isExpired) "FREE" else rawPlan

            val ent = UserEntitlementEntity(
              id = subDoc.getString("id") ?: "entitlement_$uid",
              userId = uid,
              planType = effectivePlanType,
              isActive = isPlanActive,
              startMillis = subDoc.getLong("startMillis") ?: now,
              expiryMillis = expiry,
              createdAt = subDoc.getLong("createdAt") ?: now,
              updatedAt = subDoc.getLong("updatedAt") ?: now,
              syncStatus = "SYNCED"
            )
            userEntitlementDao.insertOrUpdate(ent)
          }
        } catch (_: Exception) {}
      }

      // 7. Restore Business Profile
      var restoredProfile: BusinessProfile? = null
      try {
        val profDoc = userDoc.collection("profile").document("main").get().await()
        if (profDoc.exists()) {
          restoredProfile = BusinessProfile(
            businessName = profDoc.getString("businessName") ?: "KhataGo Merchant",
            ownerName = profDoc.getString("ownerName") ?: "Shop Owner",
            phone = profDoc.getString("phone") ?: "",
            address = profDoc.getString("address") ?: "",
            upiId = profDoc.getString("upiId") ?: ""
          )
        }
      } catch (_: Exception) {}

      _syncStatus.value = SyncStatus.SYNCED
      _lastSyncTimestamp.value = System.currentTimeMillis()
      _lastSyncMessage.value = "Data restored successfully ($custCount customers, $txCount transactions, $invCount invoices, $prodCount products)."
      Result.success(
        RestoreSummary(
          customersRestored = custCount,
          transactionsRestored = txCount,
          invoicesRestored = invCount,
          productsRestored = prodCount,
          movementsRestored = moveCount,
          profileRestored = restoredProfile != null,
          profile = restoredProfile
        )
      )
    } catch (e: Exception) {
      _syncStatus.value = SyncStatus.FAILED
      _lastSyncMessage.value = "Restore failed: ${e.localizedMessage ?: "Unknown error"}"
      Result.failure(e)
    }
  }

  suspend fun reassignLocalDataToUser(uid: String) = withContext(Dispatchers.IO) {
    customerDao.reassignCustomersToUser(uid)
    transactionDao.reassignTransactionsToUser(uid)
    invoiceDao.reassignInvoicesToUser(uid)
    productDao.reassignProductsToUser(uid)
    stockMovementDao.reassignMovementsToUser(uid)
  }

  suspend fun clearLocalData() = withContext(Dispatchers.IO) {
    customerDao.deleteAllCustomers()
    transactionDao.deleteAllTransactions()
    invoiceDao.deleteAllInvoices()
    invoiceDao.deleteAllInvoiceItems()
    productDao.deleteAllProducts()
    stockMovementDao.deleteAllMovements()
  }
}
