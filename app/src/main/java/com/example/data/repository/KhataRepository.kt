package com.example.data.repository

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
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoicePaymentStatus
import com.example.model.MonthDateUtils
import com.example.model.PlanLimits
import com.example.model.PlanType
import com.example.model.PlanUsageMetrics
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.model.SubscriptionEntitlement
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.util.InvoiceCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class AddCustomerResult {
  data class Success(val customerId: String) : AddCustomerResult()
  data class Error(val message: String) : AddCustomerResult()
}

sealed class TransactionResult {
  object Success : TransactionResult()
  data class Error(val message: String) : TransactionResult()
}

sealed class InvoiceOperationResult {
  data class Success(val invoice: Invoice) : InvoiceOperationResult()
  data class Error(val message: String) : InvoiceOperationResult()
}

sealed class ProductOperationResult {
  data class Success(val product: Product) : ProductOperationResult()
  data class Error(val message: String) : ProductOperationResult()
}

sealed class StockOperationResult {
  data class Success(val newStock: Double, val movement: StockMovement) : StockOperationResult()
  data class Error(val message: String) : StockOperationResult()
}

data class InvoiceItemInput(
  val productName: String,
  val quantity: Int,
  val unitPrice: Double,
  val discount: Double = 0.0,
  val taxRate: Double = 0.0,
  val productId: String? = null
)

class KhataRepository(
  private val customerDao: CustomerDao,
  private val transactionDao: TransactionDao,
  private val invoiceDao: InvoiceDao,
  private val productDao: ProductDao,
  private val stockMovementDao: StockMovementDao,
  private val userEntitlementDao: UserEntitlementDao? = null,
  private val pdfExportLogDao: PdfExportLogDao? = null
) {

  private val _activeUserId = MutableStateFlow("")
  val activeUserIdFlow: StateFlow<String> = _activeUserId.asStateFlow()
  val activeUserId: String get() = _activeUserId.value

  fun setActiveUser(userId: String) {
    _activeUserId.value = userId
  }

  fun getActiveUser(): String = _activeUserId.value

  private val avatarColors = listOf(
    0xFF0F766E, 0xFF8B5CF6, 0xFFDC2626, 0xFFD97706,
    0xFF2563EB, 0xFF059669, 0xFFEC4899, 0xFF0284C7
  )

  suspend fun getCurrentEntitlement(): SubscriptionEntitlement {
    val uid = activeUserId
    val ent = if (uid.isNotBlank()) {
      userEntitlementDao?.getEntitlementForUserDirect(uid)
    } else {
      userEntitlementDao?.getLatestEntitlementDirect()
    } ?: userEntitlementDao?.getLatestEntitlementDirect()
    return ent?.toModel() ?: SubscriptionEntitlement(
      id = "entitlement_${uid.ifBlank { "default" }}",
      userId = uid,
      planType = PlanType.FREE,
      isActive = true
    )
  }

  suspend fun getEffectivePlan(): PlanType {
    return getCurrentEntitlement().getEffectivePlan()
  }

  suspend fun getPlanLimits(): PlanLimits {
    return PlanLimits.forPlan(getEffectivePlan())
  }

  suspend fun updateEntitlement(entitlement: SubscriptionEntitlement) {
    userEntitlementDao?.insertOrUpdate(UserEntitlementEntity.fromModel(entitlement))
  }

  suspend fun canExportPdf(): Pair<Boolean, String?> {
    val limits = getPlanLimits()
    val startOfMonth = MonthDateUtils.getStartOfCurrentMonthMillis()
    val currentMonthlyPdfs = pdfExportLogDao?.getCountSince(startOfMonth) ?: 0
    if (currentMonthlyPdfs >= limits.maxPdfExportsPerMonth) {
      return Pair(
        false,
        "Free plan limit reached ($currentMonthlyPdfs/${limits.maxPdfExportsPerMonth} PDF exports this month). Upgrade to KhataGo Premium for unlimited PDF exports."
      )
    }
    return Pair(true, null)
  }

  suspend fun recordPdfExport(exportType: String = "PDF") {
    pdfExportLogDao?.insertLog(
      PdfExportLogEntity(
        id = "pdf_${UUID.randomUUID().toString().take(8)}",
        timestamp = System.currentTimeMillis(),
        exportType = exportType,
        userId = activeUserId
      )
    )
  }

  suspend fun canExportCsv(): Pair<Boolean, String?> {
    val limits = getPlanLimits()
    if (!limits.isCsvExportAllowed) {
      return Pair(
        false,
        "CSV export is available exclusively for KhataGo Premium members. Upgrade to KhataGo Premium to export CSV files."
      )
    }
    return Pair(true, null)
  }

  val entitlementFlow: Flow<SubscriptionEntitlement> = (userEntitlementDao?.getLatestEntitlement() ?: flowOf(null)).map { entity ->
    entity?.toModel() ?: SubscriptionEntitlement(userId = activeUserId)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val planUsageMetricsFlow: Flow<PlanUsageMetrics> = _activeUserId.flatMapLatest { uid ->
    combine(
      customerDao.getCustomerCountFlowForUser(uid),
      productDao.getProductCountFlowForUser(uid),
      invoiceDao.getInvoiceCountSinceFlowForUser(MonthDateUtils.getStartOfCurrentMonthMillis(), uid),
      transactionDao.getTransactionCountSinceFlowForUser(MonthDateUtils.getStartOfCurrentMonthMillis(), uid),
      combine(
        pdfExportLogDao?.getCountSinceFlow(MonthDateUtils.getStartOfCurrentMonthMillis()) ?: flowOf(0),
        userEntitlementDao?.getEntitlementForUser(uid) ?: flowOf(null)
      ) { pdf, ent -> Pair(pdf, ent) }
    ) { custCount, prodCount, invCount, txCount, extraPair ->
      val (pdfCount, entEntity) = extraPair
      val entitlement = entEntity?.toModel() ?: SubscriptionEntitlement(userId = uid)
      val effectivePlan = entitlement.getEffectivePlan()
      val limits = PlanLimits.forPlan(effectivePlan)
      PlanUsageMetrics(
        customerCount = custCount,
        maxCustomers = limits.maxCustomers,
        productCount = prodCount,
        maxProducts = limits.maxProducts,
        monthlyInvoiceCount = invCount,
        maxMonthlyInvoices = limits.maxInvoicesPerMonth,
        monthlyTransactionCount = txCount,
        maxMonthlyTransactions = limits.maxKhataTransactionsPerMonth,
        monthlyPdfExportCount = pdfCount,
        maxMonthlyPdfExports = limits.maxPdfExportsPerMonth,
        isCsvExportAllowed = limits.isCsvExportAllowed,
        isAdvancedReportsAllowed = limits.isAdvancedReportsAllowed,
        planType = effectivePlan,
        isPremiumActive = entitlement.isCurrentlyActive() && effectivePlan != PlanType.FREE,
        expiryMillis = entitlement.expiryMillis
      )
    }
  }

  /**
   * Observe all customers with their real-time dynamically computed balance.
   * Outstanding = total CREDIT - total PAYMENT
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val customersFlow: Flow<List<Customer>> = _activeUserId.flatMapLatest { uid ->
    combine(
      customerDao.getCustomersForUser(uid),
      transactionDao.getTransactionsForUser(uid)
    ) { customerEntities, transactionEntities ->
      val txByCustomer = transactionEntities.groupBy { it.customerId }

      customerEntities.map { entity ->
        val customerTxs = txByCustomer[entity.id].orEmpty()
        val totalCredit = customerTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val totalPayment = customerTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        val outstandingBalance = totalCredit - totalPayment

        val mostRecentTx = customerTxs.maxByOrNull { it.timestamp }
        val lastUpdatedStr = when {
          mostRecentTx != null -> formatFriendlyDate(mostRecentTx.timestamp)
          else -> formatFriendlyDate(entity.updatedDate)
        }

        Customer(
          id = entity.id,
          name = entity.name,
          phone = entity.phone,
          address = entity.address,
          balance = outstandingBalance,
          totalCredit = totalCredit,
          totalPayment = totalPayment,
          transactionCount = customerTxs.size,
          createdDate = entity.createdDate,
          updatedDate = entity.updatedDate,
          lastUpdated = lastUpdatedStr,
          avatarColorHex = entity.avatarColorHex
        )
      }
    }
  }

  /**
   * Observe all transactions across all customers, sorted newest first.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val transactionsFlow: Flow<List<Transaction>> = _activeUserId.flatMapLatest { uid ->
    transactionDao.getTransactionsForUser(uid).map { entities ->
      entities.map { it.toModel() }
    }
  }

  /**
   * Observe all invoices in real-time, newest first.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val invoicesFlow: Flow<List<Invoice>> = _activeUserId.flatMapLatest { uid ->
    combine(
      invoiceDao.getInvoicesForUser(uid),
      invoiceDao.getAllInvoiceItems()
    ) { invoiceEntities, allItemEntities ->
      val itemsByInvoiceId = allItemEntities.groupBy { it.invoiceId }
      invoiceEntities.map { invEntity ->
        val items = itemsByInvoiceId[invEntity.id].orEmpty().map { it.toModel() }
        invEntity.toModel(items)
      }
    }
  }

  /**
   * Observe transactions for a single customer with running balance calculated from history.
   */
  fun getTransactionsForCustomer(customerId: String): Flow<List<Transaction>> {
    return transactionDao.getTransactionsForCustomerAndUser(customerId, activeUserId).map { entities ->
      val sortedOldestFirst = entities.sortedBy { it.timestamp }
      var running = 0.0
      val modelsWithRunning = sortedOldestFirst.map { entity ->
        val isCredit = entity.type == "CREDIT"
        if (isCredit) {
          running += entity.amount
        } else {
          running -= entity.amount
        }
        entity.toModel(runningBalance = running)
      }
      modelsWithRunning.reversed()
    }
  }

  /**
   * Observe a single customer with their computed balance.
   */
  fun getCustomerById(customerId: String): Flow<Customer?> {
    return combine(
      customerDao.getCustomerById(customerId),
      transactionDao.getTransactionsForCustomer(customerId)
    ) { entity, customerTxs ->
      if (entity == null) return@combine null

      val totalCredit = customerTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
      val totalPayment = customerTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
      val outstandingBalance = totalCredit - totalPayment

      val mostRecentTx = customerTxs.maxByOrNull { it.timestamp }
      val lastUpdatedStr = when {
        mostRecentTx != null -> formatFriendlyDate(mostRecentTx.timestamp)
        else -> formatFriendlyDate(entity.updatedDate)
      }

      Customer(
        id = entity.id,
        name = entity.name,
        phone = entity.phone,
        address = entity.address,
        balance = outstandingBalance,
        totalCredit = totalCredit,
        totalPayment = totalPayment,
        transactionCount = customerTxs.size,
        createdDate = entity.createdDate,
        updatedDate = entity.updatedDate,
        lastUpdated = lastUpdatedStr,
        avatarColorHex = entity.avatarColorHex
      )
    }
  }

  /**
   * Observe a single invoice by ID with its items.
   */
  fun getInvoiceById(invoiceId: String): Flow<Invoice?> {
    return combine(
      invoiceDao.getInvoiceById(invoiceId),
      invoiceDao.getItemsForInvoice(invoiceId)
    ) { invEntity, itemEntities ->
      invEntity?.toModel(itemEntities.map { it.toModel() })
    }
  }

  suspend fun getInvoiceByIdDirect(invoiceId: String): Invoice? {
    val invEntity = invoiceDao.getInvoiceByIdSync(invoiceId) ?: return null
    val itemEntities = invoiceDao.getItemsForInvoiceSync(invoiceId)
    return invEntity.toModel(itemEntities.map { it.toModel() })
  }

  /**
   * Automatically generates the next unique invoice number (e.g. INV-0001, INV-0002).
   * Continues seamlessly after app restart.
   */
  suspend fun getNextInvoiceNumber(): String {
    val numbers = invoiceDao.getAllInvoiceNumbersForUser(activeUserId)
    val regex = Regex("""^INV-(\d+)$""", RegexOption.IGNORE_CASE)
    var maxIndex = 0
    for (num in numbers) {
      val match = regex.find(num.trim())
      if (match != null) {
        val idx = match.groupValues[1].toIntOrNull() ?: 0
        if (idx > maxIndex) {
          maxIndex = idx
        }
      }
    }
    return String.format(Locale.US, "INV-%04d", maxIndex + 1)
  }

  // ==========================================
  // PRODUCTS & STOCK MANAGEMENT (PHASE 5 PART 1)
  // ==========================================

  @OptIn(ExperimentalCoroutinesApi::class)
  val productsFlow: Flow<List<Product>> = _activeUserId.flatMapLatest { uid ->
    productDao.getProductsForUser(uid).map { entities ->
      entities.map { it.toModel() }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeProductsFlow: Flow<List<Product>> = _activeUserId.flatMapLatest { uid ->
    productDao.getActiveProductsForUser(uid).map { entities ->
      entities.map { it.toModel() }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val lowStockProductsFlow: Flow<List<Product>> = _activeUserId.flatMapLatest { uid ->
    productDao.getLowStockProductsForUser(uid).map { entities ->
      entities.map { it.toModel() }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val stockMovementsFlow: Flow<List<StockMovement>> = _activeUserId.flatMapLatest { uid ->
    stockMovementDao.getMovementsForUser(uid).map { entities ->
      entities.map { it.toModel() }
    }
  }

  fun getProductById(productId: String): Flow<Product?> {
    return productDao.getProductById(productId).map { it?.toModel() }
  }

  fun getStockMovementsForProduct(productId: String): Flow<List<StockMovement>> {
    return stockMovementDao.getMovementsForProduct(productId).map { entities ->
      entities.map { it.toModel() }
    }
  }

  suspend fun addProduct(
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    initialStock: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece"
  ): ProductOperationResult {
    val limits = getPlanLimits()
    val currentProductCount = productDao.getProductCountDirectForUser(activeUserId)
    if (currentProductCount >= limits.maxProducts) {
      return ProductOperationResult.Error(
        "Free plan limit reached ($currentProductCount/${limits.maxProducts} products). Upgrade to KhataGo Premium to add more products."
      )
    }

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
      return ProductOperationResult.Error("Product name cannot be empty")
    }
    if (sellingPrice < 0.0 || purchasePrice < 0.0) {
      return ProductOperationResult.Error("Prices cannot be negative")
    }
    if (initialStock < 0.0) {
      return ProductOperationResult.Error("Initial stock cannot be negative")
    }

    val now = System.currentTimeMillis()
    val productId = "prod_${UUID.randomUUID().toString().take(8)}"
    val entity = ProductEntity(
      id = productId,
      name = trimmedName,
      sku = sku.trim(),
      category = category.trim(),
      purchasePrice = purchasePrice,
      sellingPrice = sellingPrice,
      currentStock = initialStock,
      lowStockThreshold = lowStockThreshold.coerceAtLeast(0.0),
      unit = unit.trim().ifBlank { "piece" },
      isActive = true,
      createdAt = now,
      updatedAt = now,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )
    productDao.insertProduct(entity)

    if (initialStock > 0.0) {
      val initialMovement = StockMovementEntity(
        id = "sm_${UUID.randomUUID().toString().take(8)}",
        productId = productId,
        productName = trimmedName,
        quantity = initialStock,
        movementType = "IN",
        reason = "Initial Stock",
        referenceId = "initial",
        timestamp = now,
        syncStatus = "PENDING",
        userId = activeUserId,
        isDeleted = false
      )
      stockMovementDao.insertMovement(initialMovement)
    }

    return ProductOperationResult.Success(entity.toModel())
  }

  suspend fun updateProduct(
    id: String,
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece"
  ): ProductOperationResult {
    val existing = productDao.getProductByIdSync(id)
      ?: return ProductOperationResult.Error("Product not found")

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
      return ProductOperationResult.Error("Product name cannot be empty")
    }

    val now = System.currentTimeMillis()
    val updated = existing.copy(
      name = trimmedName,
      sku = sku.trim(),
      category = category.trim(),
      purchasePrice = purchasePrice.coerceAtLeast(0.0),
      sellingPrice = sellingPrice.coerceAtLeast(0.0),
      lowStockThreshold = lowStockThreshold.coerceAtLeast(0.0),
      unit = unit.trim().ifBlank { "piece" },
      updatedAt = now,
      syncStatus = "PENDING"
    )
    productDao.updateProduct(updated)
    return ProductOperationResult.Success(updated.toModel())
  }

  suspend fun toggleProductActive(productId: String, active: Boolean) {
    productDao.updateActiveStatus(productId, active, System.currentTimeMillis())
  }

  suspend fun deleteProduct(productId: String): Boolean {
    if (activeUserId.isNotBlank()) {
      productDao.softDeleteProduct(productId)
    } else {
      productDao.deleteProductById(productId)
    }
    return true
  }

  suspend fun recordStockIn(productId: String, quantity: Double, reason: String = ""): StockOperationResult {
    if (quantity <= 0.0) {
      return StockOperationResult.Error("Stock quantity must be greater than 0")
    }
    val product = productDao.getProductByIdSync(productId)
      ?: return StockOperationResult.Error("Product not found")

    val now = System.currentTimeMillis()
    val newStock = product.currentStock + quantity
    productDao.updateStock(productId, newStock, now)

    val movement = StockMovementEntity(
      id = "sm_${UUID.randomUUID().toString().take(8)}",
      productId = productId,
      productName = product.name,
      quantity = quantity,
      movementType = "IN",
      reason = reason.trim().ifBlank { "Stock In" },
      referenceId = "manual_in",
      timestamp = now,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )
    stockMovementDao.insertMovement(movement)
    return StockOperationResult.Success(newStock, movement.toModel())
  }

  suspend fun recordStockOut(productId: String, quantity: Double, reason: String = ""): StockOperationResult {
    if (quantity <= 0.0) {
      return StockOperationResult.Error("Stock quantity must be greater than 0")
    }
    val product = productDao.getProductByIdSync(productId)
      ?: return StockOperationResult.Error("Product not found")

    if (quantity > product.currentStock) {
      return StockOperationResult.Error("Cannot remove $quantity ${product.unit}. Current stock is only ${product.currentStock} ${product.unit}.")
    }

    val now = System.currentTimeMillis()
    val newStock = product.currentStock - quantity
    productDao.updateStock(productId, newStock, now)

    val movement = StockMovementEntity(
      id = "sm_${UUID.randomUUID().toString().take(8)}",
      productId = productId,
      productName = product.name,
      quantity = quantity,
      movementType = "OUT",
      reason = reason.trim().ifBlank { "Stock Out" },
      referenceId = "manual_out",
      timestamp = now,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )
    stockMovementDao.insertMovement(movement)
    return StockOperationResult.Success(newStock, movement.toModel())
  }

  suspend fun recordStockAdjustment(productId: String, newStockQuantity: Double, reason: String = ""): StockOperationResult {
    if (newStockQuantity < 0.0) {
      return StockOperationResult.Error("Stock level cannot be negative")
    }
    val product = productDao.getProductByIdSync(productId)
      ?: return StockOperationResult.Error("Product not found")

    val now = System.currentTimeMillis()
    val diff = newStockQuantity - product.currentStock
    productDao.updateStock(productId, newStockQuantity, now)

    val movement = StockMovementEntity(
      id = "sm_${UUID.randomUUID().toString().take(8)}",
      productId = productId,
      productName = product.name,
      quantity = kotlin.math.abs(diff),
      movementType = "ADJUSTMENT",
      reason = reason.trim().ifBlank { "Adjusted to $newStockQuantity ${product.unit}" },
      referenceId = "manual_adj",
      timestamp = now,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )
    stockMovementDao.insertMovement(movement)
    return StockOperationResult.Success(newStockQuantity, movement.toModel())
  }

  /**
   * Adds a new customer with input validation and duplicate prevention.
   */
  suspend fun addCustomer(name: String, phone: String, address: String = ""): AddCustomerResult {
    val limits = getPlanLimits()
    val currentCustomerCount = customerDao.getCustomerCountDirectForUser(activeUserId)
    if (currentCustomerCount >= limits.maxCustomers) {
      return AddCustomerResult.Error(
        "Free plan limit reached ($currentCustomerCount/${limits.maxCustomers} customers). Upgrade to KhataGo Premium to add more customers."
      )
    }

    val trimmedName = name.trim()
    val trimmedPhone = phone.trim()
    val trimmedAddress = address.trim()

    if (trimmedName.isBlank()) {
      return AddCustomerResult.Error("Customer name is required")
    }

    if (trimmedPhone.isNotBlank()) {
      val existingByPhone = customerDao.findCustomerByPhoneForUser(trimmedPhone, activeUserId)
      if (existingByPhone != null) {
        return AddCustomerResult.Error("Customer already exists with this phone: ${existingByPhone.name}")
      }
    }

    val existingByName = customerDao.findCustomerByNameForUser(trimmedName, activeUserId)
    if (existingByName != null) {
      return AddCustomerResult.Error("A customer named '$trimmedName' already exists")
    }

    val newId = "c_${UUID.randomUUID().toString().take(8)}"
    val color = avatarColors[(Math.abs(trimmedName.hashCode())) % avatarColors.size]
    val now = System.currentTimeMillis()

    val entity = CustomerEntity(
      id = newId,
      name = trimmedName,
      phone = trimmedPhone,
      address = trimmedAddress,
      createdDate = now,
      updatedDate = now,
      avatarColorHex = color,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )

    customerDao.insertCustomer(entity)
    return AddCustomerResult.Success(newId)
  }

  suspend fun updateCustomer(id: String, name: String, phone: String, address: String): Boolean {
    val trimmedName = name.trim()
    if (trimmedName.isBlank()) return false

    val existing = customerDao.getCustomerByIdDirect(id) ?: return false
    val updated = existing.copy(
      name = trimmedName,
      phone = phone.trim(),
      address = address.trim(),
      updatedDate = System.currentTimeMillis(),
      syncStatus = "PENDING"
    )
    customerDao.updateCustomer(updated)
    return true
  }

  suspend fun deleteCustomer(customerId: String): Boolean {
    if (activeUserId.isNotBlank()) {
      customerDao.softDeleteCustomer(customerId)
      transactionDao.softDeleteTransactionsForCustomer(customerId)
    } else {
      transactionDao.deleteTransactionsForCustomer(customerId)
      customerDao.deleteCustomerById(customerId)
    }
    return true
  }

  /**
   * Adds a manual transaction (Udhaar / Credit or Payment) with balance rules.
   */
  suspend fun addTransaction(
    customerId: String,
    customerName: String,
    type: TransactionType,
    amount: Double,
    note: String,
    timestamp: Long = System.currentTimeMillis()
  ): TransactionResult {
    val limits = getPlanLimits()
    val startOfMonth = MonthDateUtils.getStartOfCurrentMonthMillis()
    val monthlyTxCount = transactionDao.getTransactionCountSinceForUser(startOfMonth, activeUserId)
    if (monthlyTxCount >= limits.maxKhataTransactionsPerMonth) {
      return TransactionResult.Error(
        "Free plan limit reached ($monthlyTxCount/${limits.maxKhataTransactionsPerMonth} transactions this month). Upgrade to KhataGo Premium to record more entries."
      )
    }

    if (amount <= 0.0) {
      return TransactionResult.Error("Amount must be greater than 0")
    }

    val existingCustomer = customerDao.getCustomerByIdDirect(customerId)
      ?: return TransactionResult.Error("Customer not found")

    if (type == TransactionType.PAYMENT) {
      val existingTxs = transactionDao.getTransactionsForCustomerAscending(customerId)
      val totalCredit = existingTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
      val totalPayment = existingTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
      val currentBalance = totalCredit - totalPayment

      if (currentBalance <= 0.0) {
        return TransactionResult.Error("Customer has no outstanding balance to pay (Current balance: ₹0)")
      }

      if (amount > currentBalance) {
        val formattedBal = if (currentBalance % 1.0 == 0.0) currentBalance.toInt().toString() else "%.2f".format(currentBalance)
        return TransactionResult.Error("Payment amount cannot exceed outstanding balance of ₹$formattedBal")
      }
    }

    val txTypeString = if (type == TransactionType.PAYMENT) "PAYMENT" else "CREDIT"
    val defaultNote = if (type == TransactionType.PAYMENT) "Payment Mila" else "Udhaar Diya"
    val finalNote = note.trim().ifBlank { defaultNote }
    val formattedDate = formatFriendlyDate(timestamp)

    val txEntity = TransactionEntity(
      id = "t_${UUID.randomUUID().toString().take(8)}",
      customerId = customerId,
      customerName = customerName.ifBlank { existingCustomer.name },
      type = txTypeString,
      amount = amount,
      note = finalNote,
      timestamp = timestamp,
      dateFormatted = formattedDate,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )

    transactionDao.insertTransaction(txEntity)
    customerDao.updateCustomer(existingCustomer.copy(updatedDate = timestamp))

    return TransactionResult.Success
  }

  /**
   * Creates an invoice, calculates decimal-safe totals, and links with customer's khata.
   */
  suspend fun createInvoice(
    customerId: String,
    customerName: String,
    customerPhone: String = "",
    customerAddress: String = "",
    items: List<InvoiceItemInput>,
    overallDiscount: Double = 0.0,
    taxEnabled: Boolean = false,
    taxRate: Double = 0.0,
    paidAmount: Double = 0.0,
    notes: String = "",
    dueDate: String = "",
    businessProfile: BusinessProfile? = null
  ): InvoiceOperationResult {
    val limits = getPlanLimits()
    val startOfMonth = MonthDateUtils.getStartOfCurrentMonthMillis()
    val monthlyInvoiceCount = invoiceDao.getInvoiceCountSinceForUser(startOfMonth, activeUserId)
    if (monthlyInvoiceCount >= limits.maxInvoicesPerMonth) {
      return InvoiceOperationResult.Error(
        "Free plan limit reached ($monthlyInvoiceCount/${limits.maxInvoicesPerMonth} invoices this month). Upgrade to KhataGo Premium to create more invoices."
      )
    }

    val trimmedName = customerName.trim()
    if (trimmedName.isBlank()) {
      return InvoiceOperationResult.Error("Please select or enter a customer")
    }
    if (items.isEmpty()) {
      return InvoiceOperationResult.Error("Please add at least one item")
    }

    for ((index, item) in items.withIndex()) {
      if (item.productName.trim().isBlank()) {
        return InvoiceOperationResult.Error("Item #${index + 1} name cannot be empty")
      }
      if (item.quantity <= 0) {
        return InvoiceOperationResult.Error("Item #${index + 1} quantity must be at least 1")
      }
      if (item.unitPrice < 0.0) {
        return InvoiceOperationResult.Error("Item #${index + 1} price cannot be negative")
      }
    }

    val invoiceId = "inv_${UUID.randomUUID().toString().take(8)}"
    val invoiceNumber = getNextInvoiceNumber()
    val now = System.currentTimeMillis()
    val invoiceDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))

    // Convert items and calculate item totals
    val domainItems = items.mapIndexed { index, input ->
      val itemTotal = InvoiceCalculator.calculateItemTotal(
        quantity = input.quantity,
        unitPrice = input.unitPrice,
        discount = input.discount
      )
      InvoiceItem(
        id = "item_${UUID.randomUUID().toString().take(8)}",
        invoiceId = invoiceId,
        productId = input.productId,
        productName = input.productName.trim(),
        quantity = input.quantity,
        unitPrice = input.unitPrice,
        discount = input.discount.coerceAtLeast(0.0),
        taxRate = input.taxRate.coerceAtLeast(0.0),
        itemTotal = itemTotal
      )
    }

    val calc = InvoiceCalculator.calculateInvoice(
      items = domainItems,
      overallDiscount = overallDiscount,
      taxEnabled = taxEnabled,
      taxRatePercent = taxRate,
      paidAmount = paidAmount
    )

    val invoiceEntity = InvoiceEntity(
      id = invoiceId,
      invoiceNumber = invoiceNumber,
      customerId = customerId,
      customerName = trimmedName,
      customerPhone = customerPhone.trim(),
      customerAddress = customerAddress.trim(),
      invoiceDate = invoiceDateStr,
      invoiceDateMillis = now,
      dueDate = dueDate.trim(),
      businessName = businessProfile?.businessName ?: "KhataGo Merchant",
      businessPhone = businessProfile?.phone ?: "",
      businessAddress = businessProfile?.address ?: "",
      businessUpi = businessProfile?.upiId ?: "",
      subtotal = calc.subtotal,
      discount = calc.discount,
      taxEnabled = taxEnabled,
      taxRate = calc.taxRate,
      taxAmount = calc.taxAmount,
      grandTotal = calc.grandTotal,
      paidAmount = calc.paidAmount,
      remainingAmount = calc.remainingAmount,
      paymentStatus = calc.paymentStatus.name,
      notes = notes.trim(),
      createdTimestamp = now,
      updatedTimestamp = now,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )

    val itemEntities = domainItems.mapIndexed { idx, it ->
      InvoiceItemEntity(
        id = it.id,
        invoiceId = invoiceId,
        productId = it.productId,
        productName = it.productName,
        quantity = it.quantity,
        unitPrice = it.unitPrice,
        discount = it.discount,
        taxRate = it.taxRate,
        itemTotal = it.itemTotal,
        itemOrder = idx
      )
    }

    // Save invoice and items
    invoiceDao.insertInvoice(invoiceEntity)
    invoiceDao.insertInvoiceItems(itemEntities)

    // Stock Integration: Deduct stock for each item referencing a valid Product exactly ONCE
    val existingMovements = stockMovementDao.getMovementsByReferenceId(invoiceId)
    if (existingMovements.isEmpty()) {
      for (item in domainItems) {
        val pId = item.productId
        if (!pId.isNullOrBlank()) {
          val product = productDao.getProductByIdSync(pId)
          if (product != null) {
            val deductQty = item.quantity.toDouble()
            val newStock = (product.currentStock - deductQty).coerceAtLeast(0.0)
            productDao.updateStock(pId, newStock, now)
            val moveEntity = StockMovementEntity(
              id = "sm_${UUID.randomUUID().toString().take(8)}",
              productId = pId,
              productName = product.name,
              quantity = deductQty,
              movementType = "INVOICE_SALE",
              reason = "Sold in Invoice #$invoiceNumber",
              referenceId = invoiceId,
              timestamp = now,
              syncStatus = "PENDING",
              userId = activeUserId,
              isDeleted = false
            )
            stockMovementDao.insertMovement(moveEntity)
          }
        }
      }
    }

    // Khata Integration:
    // 1. CREDIT transaction for the invoice grand total (increases receivable)
    if (calc.grandTotal > 0.0) {
      val creditTx = TransactionEntity(
        id = "t_inv_${UUID.randomUUID().toString().take(8)}",
        customerId = customerId,
        customerName = trimmedName,
        type = "CREDIT",
        amount = calc.grandTotal,
        note = "Invoice #$invoiceNumber",
        timestamp = now,
        dateFormatted = formatFriendlyDate(now),
        invoiceId = invoiceId,
        syncStatus = "PENDING",
        userId = activeUserId,
        isDeleted = false
      )
      transactionDao.insertTransaction(creditTx)
    }

    // 2. If an upfront payment was made, add PAYMENT transaction (decreases receivable)
    if (calc.paidAmount > 0.0) {
      val paymentTx = TransactionEntity(
        id = "t_pay_${UUID.randomUUID().toString().take(8)}",
        customerId = customerId,
        customerName = trimmedName,
        type = "PAYMENT",
        amount = calc.paidAmount,
        note = "Payment for #$invoiceNumber",
        timestamp = now + 1,
        dateFormatted = formatFriendlyDate(now + 1),
        invoiceId = invoiceId,
        syncStatus = "PENDING",
        userId = activeUserId,
        isDeleted = false
      )
      transactionDao.insertTransaction(paymentTx)
    }

    val savedInvoice = invoiceEntity.toModel(domainItems)
    return InvoiceOperationResult.Success(savedInvoice)
  }

  /**
   * Records payment against an invoice and updates both the invoice and customer's khata.
   */
  suspend fun recordInvoicePayment(
    invoiceId: String,
    paymentAmount: Double,
    paymentNote: String = ""
  ): InvoiceOperationResult {
    if (paymentAmount <= 0.0) {
      return InvoiceOperationResult.Error("Payment amount must be greater than 0")
    }

    val invoiceEntity = invoiceDao.getInvoiceByIdSync(invoiceId)
      ?: return InvoiceOperationResult.Error("Invoice not found")

    if (invoiceEntity.remainingAmount <= 0.0) {
      return InvoiceOperationResult.Error("This invoice is already fully paid")
    }

    if (paymentAmount > invoiceEntity.remainingAmount) {
      val maxPayable = "%.2f".format(invoiceEntity.remainingAmount)
      return InvoiceOperationResult.Error("Payment cannot exceed remaining amount of ₹$maxPayable")
    }

    val now = System.currentTimeMillis()
    val newPaid = (invoiceEntity.paidAmount + paymentAmount).coerceAtMost(invoiceEntity.grandTotal)
    val newRemaining = (invoiceEntity.grandTotal - newPaid).coerceAtLeast(0.0)
    val newStatus = if (newRemaining == 0.0) "PAID" else "PARTIALLY_PAID"

    val updatedInvoiceEntity = invoiceEntity.copy(
      paidAmount = newPaid,
      remainingAmount = newRemaining,
      paymentStatus = newStatus,
      updatedTimestamp = now,
      syncStatus = "PENDING"
    )
    invoiceDao.updateInvoice(updatedInvoiceEntity)

    // Add PAYMENT transaction linked to invoice
    val payNote = paymentNote.trim().ifBlank { "Payment for Invoice #${invoiceEntity.invoiceNumber}" }
    val payTx = TransactionEntity(
      id = "t_pay_${UUID.randomUUID().toString().take(8)}",
      customerId = invoiceEntity.customerId,
      customerName = invoiceEntity.customerName,
      type = "PAYMENT",
      amount = paymentAmount,
      note = payNote,
      timestamp = now,
      dateFormatted = formatFriendlyDate(now),
      invoiceId = invoiceId,
      syncStatus = "PENDING",
      userId = activeUserId,
      isDeleted = false
    )
    transactionDao.insertTransaction(payTx)

    val items = invoiceDao.getItemsForInvoiceSync(invoiceId).map { it.toModel() }
    return InvoiceOperationResult.Success(updatedInvoiceEntity.toModel(items))
  }

  /**
   * Updates an existing invoice, recalculating totals and synchronizing customer khata.
   */
  suspend fun updateInvoice(
    invoiceId: String,
    customerId: String,
    customerName: String,
    customerPhone: String = "",
    customerAddress: String = "",
    items: List<InvoiceItemInput>,
    overallDiscount: Double = 0.0,
    taxEnabled: Boolean = false,
    taxRate: Double = 0.0,
    notes: String = "",
    dueDate: String = ""
  ): InvoiceOperationResult {
    val existingEntity = invoiceDao.getInvoiceByIdSync(invoiceId)
      ?: return InvoiceOperationResult.Error("Invoice not found")

    if (items.isEmpty()) {
      return InvoiceOperationResult.Error("Please add at least one item")
    }

    val domainItems = items.mapIndexed { index, input ->
      val itemTotal = InvoiceCalculator.calculateItemTotal(
        quantity = input.quantity,
        unitPrice = input.unitPrice,
        discount = input.discount
      )
      InvoiceItem(
        id = "item_${UUID.randomUUID().toString().take(8)}",
        invoiceId = invoiceId,
        productId = input.productId,
        productName = input.productName.trim(),
        quantity = input.quantity,
        unitPrice = input.unitPrice,
        discount = input.discount.coerceAtLeast(0.0),
        taxRate = input.taxRate.coerceAtLeast(0.0),
        itemTotal = itemTotal
      )
    }

    // Keep existing paidAmount intact
    val calc = InvoiceCalculator.calculateInvoice(
      items = domainItems,
      overallDiscount = overallDiscount,
      taxEnabled = taxEnabled,
      taxRatePercent = taxRate,
      paidAmount = existingEntity.paidAmount
    )

    val now = System.currentTimeMillis()
    val updatedEntity = existingEntity.copy(
      customerId = customerId,
      customerName = customerName.trim().ifBlank { existingEntity.customerName },
      customerPhone = customerPhone.trim(),
      customerAddress = customerAddress.trim(),
      dueDate = dueDate.trim(),
      subtotal = calc.subtotal,
      discount = calc.discount,
      taxEnabled = taxEnabled,
      taxRate = calc.taxRate,
      taxAmount = calc.taxAmount,
      grandTotal = calc.grandTotal,
      paidAmount = calc.paidAmount,
      remainingAmount = calc.remainingAmount,
      paymentStatus = calc.paymentStatus.name,
      notes = notes.trim(),
      updatedTimestamp = now,
      syncStatus = "PENDING"
    )

    invoiceDao.updateInvoice(updatedEntity)

    // Refresh items
    invoiceDao.deleteItemsForInvoice(invoiceId)
    val itemEntities = domainItems.mapIndexed { idx, it ->
      InvoiceItemEntity(
        id = it.id,
        invoiceId = invoiceId,
        productId = it.productId,
        productName = it.productName,
        quantity = it.quantity,
        unitPrice = it.unitPrice,
        discount = it.discount,
        taxRate = it.taxRate,
        itemTotal = it.itemTotal,
        itemOrder = idx
      )
    }
    invoiceDao.insertInvoiceItems(itemEntities)

    // Stock Integration on Update:
    // 1. Revert prior stock deductions for this invoice
    val priorMovements = stockMovementDao.getMovementsByReferenceId(invoiceId)
    for (prior in priorMovements) {
      if (prior.movementType == "INVOICE_SALE") {
        val prod = productDao.getProductByIdSync(prior.productId)
        if (prod != null) {
          productDao.updateStock(prod.id, prod.currentStock + prior.quantity, now)
        }
      }
    }
    stockMovementDao.deleteMovementsByReferenceId(invoiceId)

    // 2. Apply new stock deductions for items in updated invoice
    for (item in domainItems) {
      val pId = item.productId
      if (!pId.isNullOrBlank()) {
        val product = productDao.getProductByIdSync(pId)
        if (product != null) {
          val deductQty = item.quantity.toDouble()
          val newStock = (product.currentStock - deductQty).coerceAtLeast(0.0)
          productDao.updateStock(pId, newStock, now)
          val moveEntity = StockMovementEntity(
            id = "sm_${UUID.randomUUID().toString().take(8)}",
            productId = pId,
            productName = product.name,
            quantity = deductQty,
            movementType = "INVOICE_SALE",
            reason = "Sold in Invoice #${existingEntity.invoiceNumber}",
            referenceId = invoiceId,
            timestamp = now,
            syncStatus = "PENDING",
            userId = activeUserId,
            isDeleted = false
          )
          stockMovementDao.insertMovement(moveEntity)
        }
      }
    }

    // Update the linked CREDIT transaction to new grandTotal
    val existingCreditTx = transactionDao.getCreditTransactionForInvoice(invoiceId)
    if (existingCreditTx != null) {
      transactionDao.insertTransaction(
        existingCreditTx.copy(
          amount = calc.grandTotal,
          customerName = updatedEntity.customerName,
          syncStatus = "PENDING"
        )
      )
    } else if (calc.grandTotal > 0.0) {
      val creditTx = TransactionEntity(
        id = "t_inv_${UUID.randomUUID().toString().take(8)}",
        customerId = customerId,
        customerName = updatedEntity.customerName,
        type = "CREDIT",
        amount = calc.grandTotal,
        note = "Invoice #${existingEntity.invoiceNumber}",
        timestamp = now,
        dateFormatted = formatFriendlyDate(now),
        invoiceId = invoiceId,
        syncStatus = "PENDING",
        userId = activeUserId,
        isDeleted = false
      )
      transactionDao.insertTransaction(creditTx)
    }

    return InvoiceOperationResult.Success(updatedEntity.toModel(domainItems))
  }

  /**
   * Cancels an invoice safely: marks as CANCELLED, restores deducted product stock,
   * and soft/hard deletes linked credit transactions so customer balance is adjusted.
   */
  suspend fun cancelInvoice(invoiceId: String): Boolean {
    val invoice = invoiceDao.getInvoiceByIdSync(invoiceId) ?: return false
    if (invoice.paymentStatus == "CANCELLED") return true

    val now = System.currentTimeMillis()
    // 1. Revert stock for all INVOICE_SALE movements
    val movements = stockMovementDao.getMovementsByReferenceId(invoiceId)
    for (move in movements) {
      if (move.movementType == "INVOICE_SALE") {
        val product = productDao.getProductByIdSync(move.productId)
        if (product != null) {
          productDao.updateStock(product.id, product.currentStock + move.quantity, now)
          val restoreMove = StockMovementEntity(
            id = "sm_c_${UUID.randomUUID().toString().take(8)}",
            productId = product.id,
            productName = product.name,
            quantity = move.quantity,
            movementType = "INVOICE_CANCEL_RESTORE",
            reason = "Restored from cancelled Invoice #${invoice.invoiceNumber}",
            referenceId = invoiceId,
            timestamp = now,
            syncStatus = "PENDING",
            userId = activeUserId,
            isDeleted = false
          )
          stockMovementDao.insertMovement(restoreMove)
        }
      }
    }

    // 2. Mark invoice CANCELLED
    invoiceDao.updateInvoice(
      invoice.copy(
        paymentStatus = "CANCELLED",
        updatedTimestamp = now,
        syncStatus = "PENDING"
      )
    )

    // 3. Mark or delete the linked CREDIT transaction so customer khata balance is updated
    if (activeUserId.isNotBlank()) {
      transactionDao.softDeleteTransactionsForInvoice(invoiceId)
    } else {
      transactionDao.deleteTransactionsForInvoice(invoiceId)
    }
    return true
  }

  /**
   * Deletes an invoice, its items, and all linked transactions safely, restoring stock if not cancelled.
   */
  suspend fun deleteInvoice(invoiceId: String): Boolean {
    val invoice = invoiceDao.getInvoiceByIdSync(invoiceId)
    if (invoice != null && invoice.paymentStatus != "CANCELLED") {
      val now = System.currentTimeMillis()
      val movements = stockMovementDao.getMovementsByReferenceId(invoiceId)
      for (move in movements) {
        if (move.movementType == "INVOICE_SALE") {
          val product = productDao.getProductByIdSync(move.productId)
          if (product != null) {
            productDao.updateStock(product.id, product.currentStock + move.quantity, now)
          }
        }
      }
      stockMovementDao.deleteMovementsByReferenceId(invoiceId)
    }

    if (activeUserId.isNotBlank()) {
      invoiceDao.softDeleteInvoice(invoiceId)
      transactionDao.softDeleteTransactionsForInvoice(invoiceId)
    } else {
      invoiceDao.deleteItemsForInvoice(invoiceId)
      invoiceDao.deleteInvoiceById(invoiceId)
      transactionDao.deleteTransactionsForInvoice(invoiceId)
    }
    return true
  }

  // Mapper helpers
  private fun InvoiceEntity.toModel(items: List<InvoiceItem>): Invoice {
    return Invoice(
      id = this.id,
      invoiceNumber = this.invoiceNumber,
      customerId = this.customerId,
      customerName = this.customerName,
      customerPhone = this.customerPhone,
      customerAddress = this.customerAddress,
      invoiceDate = this.invoiceDate,
      invoiceDateMillis = this.invoiceDateMillis,
      dueDate = this.dueDate,
      businessName = this.businessName,
      businessPhone = this.businessPhone,
      businessAddress = this.businessAddress,
      businessUpi = this.businessUpi,
      items = items,
      subtotal = this.subtotal,
      discount = this.discount,
      taxEnabled = this.taxEnabled,
      taxRate = this.taxRate,
      taxAmount = this.taxAmount,
      grandTotal = this.grandTotal,
      paidAmount = this.paidAmount,
      remainingAmount = this.remainingAmount,
      paymentStatus = InvoicePaymentStatus.fromString(this.paymentStatus),
      notes = this.notes,
      createdTimestamp = this.createdTimestamp,
      updatedTimestamp = this.updatedTimestamp
    )
  }

  private fun InvoiceItemEntity.toModel(): InvoiceItem {
    return InvoiceItem(
      id = this.id,
      invoiceId = this.invoiceId,
      productId = this.productId,
      productName = this.productName,
      quantity = this.quantity,
      unitPrice = this.unitPrice,
      discount = this.discount,
      taxRate = this.taxRate,
      itemTotal = this.itemTotal
    )
  }

  private fun TransactionEntity.toModel(runningBalance: Double = 0.0): Transaction {
    val txType = TransactionType.fromString(this.type)
    return Transaction(
      id = this.id,
      customerId = this.customerId,
      customerName = this.customerName,
      type = txType,
      amount = this.amount,
      note = this.note,
      date = this.dateFormatted.ifBlank { formatFriendlyDate(this.timestamp) },
      timestamp = this.timestamp,
      runningBalance = runningBalance,
      isPaid = (txType == TransactionType.PAYMENT)
    )
  }

  companion object {
    fun formatFriendlyDate(timestamp: Long): String {
      val now = Calendar.getInstance()
      val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }

      val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
      val timeStr = timeFormat.format(Date(timestamp))

      val isSameYear = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
      val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

      val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
      val isYesterday = isSameYear && yesterdayCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

      return when {
        isSameDay -> "Today, $timeStr"
        isYesterday -> "Yesterday, $timeStr"
        isSameYear -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
      }
    }
  }
}
