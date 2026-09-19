package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.FirebaseAuthManager
import com.example.auth.UserAccount
import com.example.data.KhataDemoData
import com.example.data.local.AppDatabase
import com.example.data.repository.AddCustomerResult
import com.example.data.repository.InvoiceItemInput
import com.example.data.repository.InvoiceOperationResult
import com.example.data.repository.KhataRepository
import com.example.data.repository.PaymentRepository
import com.example.data.repository.PaymentRepositoryImpl
import com.example.data.repository.ProductOperationResult
import com.example.data.repository.StockOperationResult
import com.example.data.repository.TransactionResult
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.PlanUsageMetrics
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.SubscriptionEntitlement
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.payment.PaymentServiceImpl
import com.example.payment.backend.FirestoreAuthoritativeVerificationService
import com.example.payment.model.PaymentCheckResult
import com.example.payment.model.PaymentInitiationResult
import com.example.payment.model.PaymentUiState
import com.example.payment.model.PaymentVerificationResult
import com.example.payment.model.RestorePremiumResult
import com.example.sync.FirestoreSyncManager
import com.example.sync.SyncStatus
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FirstLoginChoice {
  USE_LOCAL_DATA,
  START_EMPTY_FROM_CLOUD,
  MERGE_BOTH
}

class KhataViewModel @JvmOverloads constructor(
  application: Application,
  val repository: KhataRepository = KhataRepository(
    AppDatabase.getDatabase(application).customerDao(),
    AppDatabase.getDatabase(application).transactionDao(),
    AppDatabase.getDatabase(application).invoiceDao(),
    AppDatabase.getDatabase(application).productDao(),
    AppDatabase.getDatabase(application).stockMovementDao(),
    AppDatabase.getDatabase(application).userEntitlementDao(),
    AppDatabase.getDatabase(application).pdfExportLogDao()
  ),
  val authManager: FirebaseAuthManager = FirebaseAuthManager(application),
  val networkMonitor: NetworkMonitor = NetworkMonitor(application),
  val syncManager: FirestoreSyncManager = FirestoreSyncManager(
    application,
    AppDatabase.getDatabase(application).customerDao(),
    AppDatabase.getDatabase(application).transactionDao(),
    AppDatabase.getDatabase(application).invoiceDao(),
    AppDatabase.getDatabase(application).productDao(),
    AppDatabase.getDatabase(application).stockMovementDao(),
    networkMonitor,
    AppDatabase.getDatabase(application).userEntitlementDao()
  ),
  val paymentRepository: PaymentRepository = PaymentRepositoryImpl(
    AppDatabase.getDatabase(application).paymentDao(),
    PaymentServiceImpl(
      paymentDao = AppDatabase.getDatabase(application).paymentDao(),
      userEntitlementDao = AppDatabase.getDatabase(application).userEntitlementDao(),
      authManager = authManager,
      networkMonitor = networkMonitor,
      verificationService = FirestoreAuthoritativeVerificationService(networkMonitor)
    )
  )
) : AndroidViewModel(application) {

  // Reactive real Room-backed Customer state
  val customers: StateFlow<List<Customer>> = repository.customersFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Reactive real Room-backed Transactions state
  val transactions: StateFlow<List<Transaction>> = repository.transactionsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Reactive real Room-backed Invoices state
  val invoices: StateFlow<List<Invoice>> = repository.invoicesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Reactive real Room-backed Products state
  val products: StateFlow<List<Product>> = repository.productsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val activeProducts: StateFlow<List<Product>> = repository.activeProductsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProductsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val stockMovements: StateFlow<List<StockMovement>> = repository.stockMovementsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Business Profile
  private val _businessProfile = MutableStateFlow(KhataDemoData.initialBusinessProfile)
  val businessProfile: StateFlow<BusinessProfile> = _businessProfile.asStateFlow()

  // Settings
  private val _selectedLanguage = MutableStateFlow("Hinglish")
  val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

  private val _selectedTheme = MutableStateFlow("System")
  val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

  private val _notificationsEnabled = MutableStateFlow(true)
  val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

  // Dynamically derived Financial Totals
  val totalLenaHai: Double
    get() = customers.value.filter { it.balance > 0 }.sumOf { it.balance }

  val totalDenaHai: Double
    get() = customers.value.filter { it.balance < 0 }.sumOf { -it.balance }

  val todaysHisaab: Double
    get() {
      val now = Calendar.getInstance()
      val todayTxs = transactions.value.filter { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
          cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
      }
      return todayTxs.sumOf { it.amount }
    }

  // Real-time Invoice Dashboard Metrics
  val todayInvoicesCount: Int
    get() {
      val now = Calendar.getInstance()
      return invoices.value.count { inv ->
        val cal = Calendar.getInstance().apply { timeInMillis = inv.createdTimestamp }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
          cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
      }
    }

  val todayInvoicesAmount: Double
    get() {
      val now = Calendar.getInstance()
      return invoices.value.filter { inv ->
        val cal = Calendar.getInstance().apply { timeInMillis = inv.createdTimestamp }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
          cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
      }.sumOf { it.grandTotal }
    }

  val pendingInvoicesAmount: Double
    get() = invoices.value.filter { !it.isPaid }.sumOf { it.remainingAmount }

  fun getCustomer(id: String): Customer? {
    return customers.value.find { it.id == id }
  }

  fun getCustomerStream(id: String): Flow<Customer?> {
    return repository.getCustomerById(id)
  }

  fun getTransactionsForCustomer(customerId: String): List<Transaction> {
    return transactions.value.filter { it.customerId == customerId }
  }

  fun getTransactionsStreamForCustomer(customerId: String): Flow<List<Transaction>> {
    return repository.getTransactionsForCustomer(customerId)
  }

  fun getInvoiceStream(invoiceId: String): Flow<Invoice?> {
    return repository.getInvoiceById(invoiceId)
  }

  suspend fun getInvoiceDirect(invoiceId: String): Invoice? {
    return repository.getInvoiceByIdDirect(invoiceId)
  }

  suspend fun getNextInvoiceNumber(): String {
    return repository.getNextInvoiceNumber()
  }

  suspend fun addCustomerAsync(name: String, phone: String, address: String = ""): AddCustomerResult {
    val res = repository.addCustomer(name, phone, address)
    if (res is AddCustomerResult.Success) triggerAutoSync()
    return res
  }

  suspend fun updateCustomerAsync(id: String, name: String, phone: String, address: String): Boolean {
    val res = repository.updateCustomer(id, name, phone, address)
    if (res) triggerAutoSync()
    return res
  }

  suspend fun deleteCustomerAsync(id: String): Boolean {
    val res = repository.deleteCustomer(id)
    if (res) triggerAutoSync()
    return res
  }

  suspend fun addTransactionAsync(
    customerId: String,
    customerName: String,
    type: TransactionType,
    amount: Double,
    note: String
  ): TransactionResult {
    val res = repository.addTransaction(
      customerId = customerId,
      customerName = customerName,
      type = type,
      amount = amount,
      note = note
    )
    if (res is TransactionResult.Success) triggerAutoSync()
    return res
  }

  suspend fun createInvoiceAsync(
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
    dueDate: String = ""
  ): InvoiceOperationResult {
    val res = repository.createInvoice(
      customerId = customerId,
      customerName = customerName,
      customerPhone = customerPhone,
      customerAddress = customerAddress,
      items = items,
      overallDiscount = overallDiscount,
      taxEnabled = taxEnabled,
      taxRate = taxRate,
      paidAmount = paidAmount,
      notes = notes,
      dueDate = dueDate,
      businessProfile = businessProfile.value
    )
    if (res is InvoiceOperationResult.Success) triggerAutoSync()
    return res
  }

  suspend fun updateInvoiceAsync(
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
    val res = repository.updateInvoice(
      invoiceId = invoiceId,
      customerId = customerId,
      customerName = customerName,
      customerPhone = customerPhone,
      customerAddress = customerAddress,
      items = items,
      overallDiscount = overallDiscount,
      taxEnabled = taxEnabled,
      taxRate = taxRate,
      notes = notes,
      dueDate = dueDate
    )
    if (res is InvoiceOperationResult.Success) triggerAutoSync()
    return res
  }

  suspend fun recordInvoicePaymentAsync(
    invoiceId: String,
    amount: Double,
    note: String = ""
  ): InvoiceOperationResult {
    val res = repository.recordInvoicePayment(invoiceId, amount, note)
    if (res is InvoiceOperationResult.Success) triggerAutoSync()
    return res
  }

  suspend fun deleteInvoiceAsync(invoiceId: String): Boolean {
    val res = repository.deleteInvoice(invoiceId)
    if (res) triggerAutoSync()
    return res
  }

  // Synchronous convenience helpers for existing screens
  fun addCustomer(
    name: String,
    phone: String,
    address: String = "",
    onResult: (AddCustomerResult) -> Unit = {}
  ): Boolean {
    viewModelScope.launch {
      val result = repository.addCustomer(name, phone, address)
      onResult(result)
    }
    return true
  }

  fun addTransaction(
    customerId: String,
    customerName: String,
    type: TransactionType,
    amount: Double,
    note: String,
    onResult: (TransactionResult) -> Unit = {}
  ): Boolean {
    viewModelScope.launch {
      val res = repository.addTransaction(customerId, customerName, type, amount, note)
      onResult(res)
    }
    return true
  }

  fun updateBusinessProfile(
    businessName: String,
    ownerName: String,
    phone: String,
    address: String,
    upiId: String
  ): Boolean {
    if (businessName.isBlank() || ownerName.isBlank()) return false
    _businessProfile.value = BusinessProfile(
      businessName = businessName.trim(),
      ownerName = ownerName.trim(),
      phone = phone.trim(),
      address = address.trim(),
      upiId = upiId.trim()
    )
    triggerAutoSync()
    return true
  }

  fun createInvoice(
    customerName: String,
    phone: String,
    productName: String,
    quantity: Int,
    pricePerUnit: Double,
    discountPercent: Double,
    taxPercent: Double
  ): Boolean {
    viewModelScope.launch {
      val matchedCustomer = customers.value.find { it.name.equals(customerName.trim(), ignoreCase = true) }
      val custId = matchedCustomer?.id ?: "c_walkin"
      val itemInput = InvoiceItemInput(
        productName = productName,
        quantity = quantity,
        unitPrice = pricePerUnit,
        discount = 0.0,
        taxRate = taxPercent
      )
      repository.createInvoice(
        customerId = custId,
        customerName = customerName,
        customerPhone = phone,
        items = listOf(itemInput),
        overallDiscount = 0.0,
        taxEnabled = taxPercent > 0.0,
        taxRate = taxPercent,
        paidAmount = 0.0,
        businessProfile = businessProfile.value
      )
    }
    return true
  }

  fun setLanguage(lang: String) {
    _selectedLanguage.value = lang
  }

  fun setTheme(theme: String) {
    _selectedTheme.value = theme
  }

  fun toggleNotifications() {
    _notificationsEnabled.value = !_notificationsEnabled.value
  }

  // Account & Sync State
  val currentUser: StateFlow<UserAccount?> = authManager.currentUser
  val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
  val lastSyncTimestamp: StateFlow<Long> = syncManager.lastSyncTimestamp
  val lastSyncMessage: StateFlow<String> = syncManager.lastSyncMessage

  private val _showFirstLoginDialog = MutableStateFlow(false)
  val showFirstLoginDialog: StateFlow<Boolean> = _showFirstLoginDialog.asStateFlow()

  init {
    viewModelScope.launch {
      currentUser.collect { user ->
        if (user != null) {
          val previousActiveUser = repository.getActiveUser()
          if (previousActiveUser != user.uid) {
            // Reassign any local guest/unassigned data created before login
            syncManager.reassignLocalDataToUser(user.uid)
            repository.setActiveUser(user.uid)
            // Push any pending local data to Firestore under their UID
            syncManager.syncPending(user.uid, businessProfile.value)
            // Automatically load and restore previous history from Firestore under their UID
            val res = syncManager.restoreUserData(user.uid)
            res.onSuccess { summary ->
              summary.profile?.let { prof ->
                _businessProfile.value = prof
              }
            }
          }
        } else {
          repository.setActiveUser("")
        }
      }
    }

    // Auto-sync when online and user is authenticated
    viewModelScope.launch {
      networkMonitor.isOnline.collect { online ->
        val user = currentUser.value
        if (online && user != null && (syncStatus.value == SyncStatus.OFFLINE || syncStatus.value == SyncStatus.SYNCED)) {
          syncManager.syncPending(user.uid, businessProfile.value)
        }
      }
    }
  }

  fun triggerAutoSync() {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      if (networkMonitor.isCurrentlyOnline()) {
        syncManager.syncPending(user.uid, businessProfile.value)
      }
    }
  }

  fun dismissFirstLoginDialog() {
    _showFirstLoginDialog.value = false
  }

  fun handleFirstLoginChoice(choice: FirstLoginChoice) {
    _showFirstLoginDialog.value = false
    val user = currentUser.value ?: return
    viewModelScope.launch {
      when (choice) {
        FirstLoginChoice.USE_LOCAL_DATA -> {
          syncManager.reassignLocalDataToUser(user.uid)
          repository.setActiveUser(user.uid)
          syncManager.syncPending(user.uid, businessProfile.value)
        }
        FirstLoginChoice.START_EMPTY_FROM_CLOUD -> {
          syncManager.clearLocalData()
          repository.setActiveUser(user.uid)
          syncManager.restoreUserData(user.uid)
        }
        FirstLoginChoice.MERGE_BOTH -> {
          syncManager.reassignLocalDataToUser(user.uid)
          repository.setActiveUser(user.uid)
          syncManager.syncPending(user.uid, businessProfile.value)
          syncManager.restoreUserData(user.uid)
        }
      }
    }
  }

  fun signInWithGoogle(activity: Activity, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = authManager.signInWithGoogle(activity)
      res.fold(
        onSuccess = { account ->
          repository.setActiveUser(account.uid)
          onResult(true, null)
        },
        onFailure = { err ->
          onResult(false, err.localizedMessage)
        }
      )
    }
  }

  fun signInWithDemoAccount(displayName: String = "Shop Owner", onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    val account = authManager.signInWithDemoAccount(displayName = displayName)
    repository.setActiveUser(account.uid)
    onResult(true, null)
  }

  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onError: (String) -> Unit,
    onAutoVerified: ((UserAccount) -> Unit)? = null
  ) {
    authManager.sendPhoneOtp(
      activity = activity,
      phoneNumber = phoneNumber,
      onCodeSent = onCodeSent,
      onError = onError,
      onAutoVerified = { account ->
        repository.setActiveUser(account.uid)
        onAutoVerified?.invoke(account)
      }
    )
  }

  fun resendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    authManager.sendPhoneOtp(
      activity = activity,
      phoneNumber = phoneNumber,
      onCodeSent = onCodeSent,
      onError = onError,
      onAutoVerified = { account ->
        repository.setActiveUser(account.uid)
      },
      resendToken = authManager.lastResendingToken
    )
  }

  fun verifyPhoneOtp(
    verificationId: String,
    otp: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = authManager.verifyPhoneOtp(verificationId, otp)
      res.fold(
        onSuccess = { account ->
          repository.setActiveUser(account.uid)
          onResult(true, null)
        },
        onFailure = { err ->
          onResult(false, err.localizedMessage)
        }
      )
    }
  }

  fun signOut() {
    authManager.signOut()
    repository.setActiveUser("")
  }

  fun syncNow(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    val user = currentUser.value
    if (user == null) {
      onResult(false, "Please sign in to sync your business ledger.")
      return
    }
    viewModelScope.launch {
      val res = syncManager.syncPending(user.uid, businessProfile.value)
      res.fold(
        onSuccess = { summary ->
          onResult(true, "Synced: ${summary.customersUploaded} customers, ${summary.transactionsUploaded} entries, ${summary.invoicesUploaded} invoices.")
        },
        onFailure = { err ->
          onResult(false, err.localizedMessage)
        }
      )
    }
  }

  fun restoreData(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    val user = currentUser.value
    if (user == null) {
      onResult(false, "Please sign in to restore your business ledger.")
      return
    }
    viewModelScope.launch {
      val res = syncManager.restoreUserData(user.uid)
      res.fold(
        onSuccess = { summary ->
          onResult(true, "Restored: ${summary.customersRestored} customers, ${summary.transactionsRestored} transactions, ${summary.invoicesRestored} invoices.")
        },
        onFailure = { err ->
          onResult(false, err.localizedMessage)
        }
      )
    }
  }

  fun isFirebaseConfigured(): Boolean = authManager.isFirebaseConfigured()
  fun getFirebaseNotice(): String = authManager.getFirebaseConfigurationNotice()

  // ==========================================
  // PRODUCT & STOCK OPERATIONS (PHASE 5 PART 1)
  // ==========================================

  fun addProduct(
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    initialStock: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      when (val res = repository.addProduct(name, sku, category, purchasePrice, sellingPrice, initialStock, lowStockThreshold, unit)) {
        is ProductOperationResult.Success -> {
          triggerAutoSync()
          onResult(true, null)
        }
        is ProductOperationResult.Error -> onResult(false, res.message)
      }
    }
  }

  fun updateProduct(
    id: String,
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      when (val res = repository.updateProduct(id, name, sku, category, purchasePrice, sellingPrice, lowStockThreshold, unit)) {
        is ProductOperationResult.Success -> {
          triggerAutoSync()
          onResult(true, null)
        }
        is ProductOperationResult.Error -> onResult(false, res.message)
      }
    }
  }

  fun toggleProductActive(productId: String, active: Boolean) {
    viewModelScope.launch {
      repository.toggleProductActive(productId, active)
      triggerAutoSync()
    }
  }

  fun deleteProduct(productId: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val success = repository.deleteProduct(productId)
      if (success) triggerAutoSync()
      onResult(success)
    }
  }

  fun recordStockIn(
    productId: String,
    quantity: Double,
    reason: String = "",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      when (val res = repository.recordStockIn(productId, quantity, reason)) {
        is StockOperationResult.Success -> {
          triggerAutoSync()
          onResult(true, null)
        }
        is StockOperationResult.Error -> onResult(false, res.message)
      }
    }
  }

  fun recordStockOut(
    productId: String,
    quantity: Double,
    reason: String = "",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      when (val res = repository.recordStockOut(productId, quantity, reason)) {
        is StockOperationResult.Success -> {
          triggerAutoSync()
          onResult(true, null)
        }
        is StockOperationResult.Error -> onResult(false, res.message)
      }
    }
  }

  fun recordStockAdjustment(
    productId: String,
    newStockQuantity: Double,
    reason: String = "",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      when (val res = repository.recordStockAdjustment(productId, newStockQuantity, reason)) {
        is StockOperationResult.Success -> {
          triggerAutoSync()
          onResult(true, null)
        }
        is StockOperationResult.Error -> onResult(false, res.message)
      }
    }
  }

  fun cancelInvoice(invoiceId: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val success = repository.cancelInvoice(invoiceId)
      onResult(success)
    }
  }

  fun getProductStockMovements(productId: String): Flow<List<StockMovement>> {
    return repository.getStockMovementsForProduct(productId)
  }

  suspend fun addProductAsync(
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    initialStock: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece"
  ): ProductOperationResult {
    return repository.addProduct(name, sku, category, purchasePrice, sellingPrice, initialStock, lowStockThreshold, unit)
  }

  suspend fun updateProductAsync(
    id: String,
    name: String,
    sku: String = "",
    category: String = "",
    purchasePrice: Double = 0.0,
    sellingPrice: Double = 0.0,
    lowStockThreshold: Double = 5.0,
    unit: String = "piece"
  ): ProductOperationResult {
    return repository.updateProduct(id, name, sku, category, purchasePrice, sellingPrice, lowStockThreshold, unit)
  }

  suspend fun deleteProductAsync(productId: String): Boolean {
    return repository.deleteProduct(productId)
  }

  suspend fun stockInAsync(productId: String, quantity: Double, reason: String = ""): StockOperationResult {
    return repository.recordStockIn(productId, quantity, reason)
  }

  suspend fun stockOutAsync(productId: String, quantity: Double, reason: String = ""): StockOperationResult {
    return repository.recordStockOut(productId, quantity, reason)
  }

  suspend fun adjustStockAsync(productId: String, newStockQuantity: Double, reason: String = ""): StockOperationResult {
    return repository.recordStockAdjustment(productId, newStockQuantity, reason)
  }

  // Reactive Monetization / Plan state
  val planUsageMetrics: StateFlow<PlanUsageMetrics> = repository.planUsageMetricsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = PlanUsageMetrics()
    )

  val currentEntitlement: StateFlow<SubscriptionEntitlement> = repository.entitlementFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = SubscriptionEntitlement()
    )

  suspend fun canExportPdf(): Pair<Boolean, String?> = repository.canExportPdf()

  suspend fun recordPdfExport(exportType: String = "PDF") = repository.recordPdfExport(exportType)

  suspend fun canExportCsv(): Pair<Boolean, String?> = repository.canExportCsv()

  // Payment & Subscription Integration Foundation
  val paymentUiState: StateFlow<PaymentUiState> = paymentRepository.paymentUiState

  fun startMonthlyPurchase(activity: Activity? = null, onResult: ((PaymentInitiationResult) -> Unit)? = null) {
    viewModelScope.launch {
      val res = paymentRepository.startMonthlyPurchase(activity)
      onResult?.invoke(res)
    }
  }

  fun startYearlyPurchase(activity: Activity? = null, onResult: ((PaymentInitiationResult) -> Unit)? = null) {
    viewModelScope.launch {
      val res = paymentRepository.startYearlyPurchase(activity)
      onResult?.invoke(res)
    }
  }

  fun checkPaymentStatus(orderId: String, onResult: ((PaymentCheckResult) -> Unit)? = null) {
    viewModelScope.launch {
      val res = paymentRepository.checkPaymentStatus(orderId)
      onResult?.invoke(res)
    }
  }

  fun restorePremium(onResult: ((RestorePremiumResult) -> Unit)? = null) {
    viewModelScope.launch {
      val res = paymentRepository.restorePremium()
      onResult?.invoke(res)
    }
  }

  fun handlePaymentResult(
    orderId: String,
    gatewayPaymentId: String?,
    gatewayStatus: String,
    failureReason: String? = null,
    onResult: ((PaymentVerificationResult) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val res = paymentRepository.handlePaymentResult(orderId, gatewayPaymentId, gatewayStatus, failureReason)
      onResult?.invoke(res)
    }
  }

  fun resetPaymentUiState() {
    paymentRepository.resetUiState()
  }
}
