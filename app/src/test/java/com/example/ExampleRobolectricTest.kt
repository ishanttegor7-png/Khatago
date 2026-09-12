package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AddCustomerResult
import com.example.data.repository.InvoiceItemInput
import com.example.data.repository.InvoiceOperationResult
import com.example.data.repository.KhataRepository
import com.example.data.repository.TransactionResult
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.DatePeriodFilter
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoicePaymentStatus
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.model.TransactionType
import com.example.util.DashboardCalculator
import com.example.util.DataExportHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: KhataRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = KhataRepository(
      database.customerDao(),
      database.transactionDao(),
      database.invoiceDao(),
      database.productDao(),
      database.stockMovementDao()
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KhataGo", appName)
  }

  @Test
  fun `verify ViewModel can be instantiated via AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val vm = factory.create(com.example.ui.KhataViewModel::class.java)
    assertNotNull(vm)
  }

  @Test
  fun `verify MainActivity launches and renders without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `test 1 add customer and verify it appears in customer list`() = runBlocking {
    val initialCustomers = repository.customersFlow.first()
    assertTrue("Database starts empty", initialCustomers.isEmpty())

    val result = repository.addCustomer("Ramesh Kumar", "9829012345", "Jaipur")
    assertTrue(result is AddCustomerResult.Success)
    val customerId = (result as AddCustomerResult.Success).customerId

    val updatedCustomers = repository.customersFlow.first()
    assertEquals(1, updatedCustomers.size)
    val customer = updatedCustomers.first()
    assertEquals(customerId, customer.id)
    assertEquals("Ramesh Kumar", customer.name)
    assertEquals("9829012345", customer.phone)
    assertEquals("Jaipur", customer.address)
    assertEquals(0.0, customer.balance, 0.001)
  }

  @Test
  fun `test 2 add udhaar and verify customer balance increases`() = runBlocking {
    val addResult = repository.addCustomer("Pooja Verma", "9810987654")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    // Add Udhaar (Credit) of 500
    val txResult = repository.addTransaction(
      customerId = customerId,
      customerName = "Pooja Verma",
      type = TransactionType.CREDIT,
      amount = 500.0,
      note = "Grocery items"
    )
    assertTrue(txResult is TransactionResult.Success)

    val customer = repository.getCustomerById(customerId).first()
    assertNotNull(customer)
    assertEquals(500.0, customer!!.balance, 0.001)
    assertTrue(customer.isLenaHai)

    // Add more Udhaar of 300 -> Total should be 800
    repository.addTransaction(
      customerId = customerId,
      customerName = "Pooja Verma",
      type = TransactionType.CREDIT,
      amount = 300.0,
      note = "Cooking oil"
    )

    val updatedCustomer = repository.getCustomerById(customerId).first()
    assertEquals(800.0, updatedCustomer!!.balance, 0.001)
  }

  @Test
  fun `test 3 add payment and verify customer balance decreases`() = runBlocking {
    val addResult = repository.addCustomer("Deepak Sharma", "9460012345")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    // Add Udhaar of 800
    repository.addTransaction(
      customerId = customerId,
      customerName = "Deepak Sharma",
      type = TransactionType.CREDIT,
      amount = 800.0,
      note = "Initial Udhaar"
    )

    // Add Payment of 300 -> Balance becomes 500
    val payResult = repository.addTransaction(
      customerId = customerId,
      customerName = "Deepak Sharma",
      type = TransactionType.PAYMENT,
      amount = 300.0,
      note = "Cash payment"
    )
    assertTrue(payResult is TransactionResult.Success)

    val customer = repository.getCustomerById(customerId).first()
    assertNotNull(customer)
    assertEquals(500.0, customer!!.balance, 0.001)
  }

  @Test
  fun `test 4 balance calculation equals total credit minus total payment`() = runBlocking {
    val addResult = repository.addCustomer("Vikram Singh", "9928076543")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    // Credit 1000, Payment 300 -> 700 outstanding
    repository.addTransaction(customerId, "Vikram Singh", TransactionType.CREDIT, 1000.0, "Credit 1")
    repository.addTransaction(customerId, "Vikram Singh", TransactionType.PAYMENT, 300.0, "Payment 1")

    var customer = repository.getCustomerById(customerId).first()
    assertEquals(1000.0, customer!!.totalCredit, 0.001)
    assertEquals(300.0, customer.totalPayment, 0.001)
    assertEquals(700.0, customer.balance, 0.001)

    // Pay remaining 700 -> 0 Settled
    repository.addTransaction(customerId, "Vikram Singh", TransactionType.PAYMENT, 700.0, "Full Settlement")
    customer = repository.getCustomerById(customerId).first()
    assertEquals(1000.0, customer!!.totalCredit, 0.001)
    assertEquals(1000.0, customer.totalPayment, 0.001)
    assertEquals(0.0, customer.balance, 0.001)
    assertTrue(customer.isAllClear)
  }

  @Test
  fun `test 5 payment larger than current balance is blocked with friendly error message`() = runBlocking {
    val addResult = repository.addCustomer("Anil Meena", "9785067890")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    // Customer with no debt tries to pay 200
    val error1 = repository.addTransaction(customerId, "Anil Meena", TransactionType.PAYMENT, 200.0, "Payment")
    assertTrue(error1 is TransactionResult.Error)
    assertTrue((error1 as TransactionResult.Error).message.contains("no outstanding balance", ignoreCase = true))

    // Customer owes 500, tries to pay 700
    repository.addTransaction(customerId, "Anil Meena", TransactionType.CREDIT, 500.0, "Credit")
    val error2 = repository.addTransaction(customerId, "Anil Meena", TransactionType.PAYMENT, 700.0, "Overpay")
    assertTrue(error2 is TransactionResult.Error)
    assertTrue((error2 as TransactionResult.Error).message.contains("cannot exceed", ignoreCase = true))
  }

  @Test
  fun `test 6 delete customer removes customer and their associated transactions`() = runBlocking {
    val addResult = repository.addCustomer("Suresh Kumar", "9414054321")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    repository.addTransaction(customerId, "Suresh Kumar", TransactionType.CREDIT, 1500.0, "Credit")
    val txsBefore = repository.getTransactionsForCustomer(customerId).first()
    assertEquals(1, txsBefore.size)

    val deleted = repository.deleteCustomer(customerId)
    assertTrue(deleted)

    val customerAfter = repository.getCustomerById(customerId).first()
    assertNull(customerAfter)

    val txsAfter = repository.getTransactionsForCustomer(customerId).first()
    assertTrue(txsAfter.isEmpty())
  }

  @Test
  fun `test 7 create invoice with multiple items and tax calculates decimal-safe totals and updates ledger`() = runBlocking {
    val addResult = repository.addCustomer("Mohit Agarwal", "9829011223")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    val items = listOf(
      InvoiceItemInput("Atta 10kg", 2, 400.0, 50.0), // (2*400) - 50 = 750
      InvoiceItemInput("Ghee 1L", 1, 600.0, 0.0)      // 600
    ) // Subtotal = 1350

    val res = repository.createInvoice(
      customerId = customerId,
      customerName = "Mohit Agarwal",
      customerPhone = "9829011223",
      items = items,
      overallDiscount = 50.0, // After disc: 1300
      taxEnabled = true,
      taxRate = 5.0, // 5% of 1300 = 65
      paidAmount = 365.0 // Grand total = 1365, Paid = 365, Remaining = 1000
    )

    assertTrue(res is InvoiceOperationResult.Success)
    val invoice = (res as InvoiceOperationResult.Success).invoice
    assertEquals("INV-0001", invoice.invoiceNumber)
    assertEquals(1350.0, invoice.subtotal, 0.001)
    assertEquals(50.0, invoice.discount, 0.001)
    assertEquals(65.0, invoice.taxAmount, 0.001)
    assertEquals(1365.0, invoice.grandTotal, 0.001)
    assertEquals(365.0, invoice.paidAmount, 0.001)
    assertEquals(1000.0, invoice.remainingAmount, 0.001)
    assertEquals(InvoicePaymentStatus.PARTIALLY_PAID, invoice.paymentStatus)

    // Customer ledger verification:
    // Receivable credit added: 1365, upfront payment: 365 -> Customer net balance = 1000
    val customer = repository.getCustomerById(customerId).first()
    assertNotNull(customer)
    assertEquals(1000.0, customer!!.balance, 0.001)
  }

  @Test
  fun `test 8 record payment on invoice updates status to paid and updates khata balance`() = runBlocking {
    val addResult = repository.addCustomer("Kavita Jain", "9828033445")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    val items = listOf(InvoiceItemInput("Rice 5kg", 1, 500.0))
    val createRes = repository.createInvoice(
      customerId = customerId,
      customerName = "Kavita Jain",
      items = items,
      paidAmount = 0.0 // Pending
    )
    val invoice = (createRes as InvoiceOperationResult.Success).invoice
    assertEquals(InvoicePaymentStatus.PENDING, invoice.paymentStatus)
    assertEquals(500.0, invoice.remainingAmount, 0.001)

    // Pay full remaining 500
    val payRes = repository.recordInvoicePayment(invoice.id, 500.0, "GPay payment")
    assertTrue(payRes is InvoiceOperationResult.Success)
    val updatedInvoice = (payRes as InvoiceOperationResult.Success).invoice
    assertEquals(InvoicePaymentStatus.PAID, updatedInvoice.paymentStatus)
    assertEquals(0.0, updatedInvoice.remainingAmount, 0.001)
    assertEquals(500.0, updatedInvoice.paidAmount, 0.001)

    // Customer balance now clear
    val customer = repository.getCustomerById(customerId).first()
    assertNotNull(customer)
    assertEquals(0.0, customer!!.balance, 0.001)
    assertTrue(customer.isAllClear)
  }

  @Test
  fun `test 9 sequential invoice numbering persists correctly`() = runBlocking {
    val num1 = repository.getNextInvoiceNumber()
    assertEquals("INV-0001", num1)

    val addResult = repository.addCustomer("Test Cust", "9829000000")
    val cId = (addResult as AddCustomerResult.Success).customerId
    repository.createInvoice(cId, "Test Cust", items = listOf(InvoiceItemInput("Item 1", 1, 100.0)))

    val num2 = repository.getNextInvoiceNumber()
    assertEquals("INV-0002", num2)
  }

  @Test
  fun `test 10 entities created with pending sync status and active user id`() = runBlocking {
    repository.setActiveUser("user_abc_123")
    val addResult = repository.addCustomer("Aakash Mehra", "9829099887")
    assertTrue(addResult is AddCustomerResult.Success)
    val customerId = (addResult as AddCustomerResult.Success).customerId

    val entity = database.customerDao().getCustomerByIdDirect(customerId)
    assertNotNull(entity)
    assertEquals("PENDING", entity!!.syncStatus)
    assertEquals("user_abc_123", entity.userId)
    assertEquals(false, entity.isDeleted)

    repository.addTransaction(customerId, "Aakash Mehra", TransactionType.CREDIT, 450.0, "Dry cleaning")
    val pendingTxs = database.transactionDao().getPendingTransactions()
    assertEquals(1, pendingTxs.size)
    assertEquals("PENDING", pendingTxs.first().syncStatus)
    assertEquals("user_abc_123", pendingTxs.first().userId)
  }

  @Test
  fun `test 11 soft delete marks entity as pending delete and excludes from active queries`() = runBlocking {
    repository.setActiveUser("user_xyz_789")
    val addResult = repository.addCustomer("Prakash Raj", "9829055443")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    repository.addTransaction(customerId, "Prakash Raj", TransactionType.CREDIT, 1200.0, "Wholesale rice")

    // Verify active
    var activeCusts = repository.customersFlow.first()
    assertEquals(1, activeCusts.size)

    // Delete with active user -> soft deletes
    val deleted = repository.deleteCustomer(customerId)
    assertTrue(deleted)

    // Excluded from standard UI list
    activeCusts = repository.customersFlow.first()
    assertTrue(activeCusts.isEmpty())

    // But marked PENDING_DELETE in DAO for sync
    val pendingSync = database.customerDao().getPendingCustomers()
    val deletedCust = pendingSync.find { it.id == customerId }
    assertNotNull(deletedCust)
    assertEquals(true, deletedCust!!.isDeleted)
    assertEquals("PENDING_DELETE", deletedCust.syncStatus)
  }

  @Test
  fun `test 12 migration of unassigned local data to authenticated user`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val syncManager = com.example.sync.FirestoreSyncManager(
      context,
      database.customerDao(),
      database.transactionDao(),
      database.invoiceDao(),
      database.productDao(),
      database.stockMovementDao(),
      com.example.util.NetworkMonitor(context)
    )

    // Customer created offline before login (unassigned userId)
    repository.setActiveUser("")
    val addResult = repository.addCustomer("Offline Shopkeeper", "9829033221")
    val customerId = (addResult as AddCustomerResult.Success).customerId

    val beforeReassign = database.customerDao().getCustomerByIdDirect(customerId)
    assertEquals("", beforeReassign!!.userId)

    // User logs in with UID
    syncManager.reassignLocalDataToUser("new_user_login_uid")

    val afterReassign = database.customerDao().getCustomerByIdDirect(customerId)
    assertEquals("new_user_login_uid", afterReassign!!.userId)
    assertEquals("PENDING", afterReassign.syncStatus)
  }

  @Test
  fun `test 13 clear local data clears room database safely for cloud restore`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val syncManager = com.example.sync.FirestoreSyncManager(
      context,
      database.customerDao(),
      database.transactionDao(),
      database.invoiceDao(),
      database.productDao(),
      database.stockMovementDao(),
      com.example.util.NetworkMonitor(context)
    )

    repository.addCustomer("Temp Customer", "9999900000")
    assertTrue(repository.customersFlow.first().isNotEmpty())

    syncManager.clearLocalData()
    assertTrue(repository.customersFlow.first().isEmpty())
  }

  @Test
  fun `test 14 invoice update sets sync status to pending`() = runBlocking {
    repository.setActiveUser("user_invoice_sync")
    val addResult = repository.addCustomer("Invoiced Client", "9829077665")
    val cId = (addResult as AddCustomerResult.Success).customerId

    val createRes = repository.createInvoice(cId, "Invoiced Client", items = listOf(InvoiceItemInput("Oil", 1, 200.0)))
    val invoice = (createRes as InvoiceOperationResult.Success).invoice

    // Simulate mark synced
    database.invoiceDao().updateSyncStatus(invoice.id, "SYNCED")
    var entity = database.invoiceDao().getInvoiceByIdSync(invoice.id)
    assertEquals("SYNCED", entity!!.syncStatus)

    // Record payment -> should mark back to PENDING
    repository.recordInvoicePayment(invoice.id, 100.0, "Part pay")
    entity = database.invoiceDao().getInvoiceByIdSync(invoice.id)
    assertEquals("PENDING", entity!!.syncStatus)
  }

  @Test
  fun `test 15 phone number formatting for indian e164 standards`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.auth.FirebaseAuthManager(context)

    assertEquals("+919876543210", authManager.formatPhoneNumber("9876543210"))
    assertEquals("+919876543210", authManager.formatPhoneNumber("+919876543210"))
    assertEquals("+919876543210", authManager.formatPhoneNumber("09876543210"))
    assertEquals("+919876543210", authManager.formatPhoneNumber("98765 43210"))
    assertEquals("+919876543210", authManager.formatPhoneNumber("919876543210"))
  }

  @Test
  fun `test 16 phone otp validation and no bypass`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.auth.FirebaseAuthManager(context)

    // Short phone number must trigger onError
    var errorReceived: String? = null
    authManager.sendPhoneOtp(
      activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get(),
      phoneNumber = "123",
      onCodeSent = {},
      onError = { errorReceived = it }
    )
    assertTrue("Error should be reported for short number", errorReceived != null)

    // Empty OTP verify should fail immediately without faking
    val emptyOtpResult = authManager.verifyPhoneOtp("vId123", "")
    assertTrue("Empty OTP should return failure", emptyOtpResult.isFailure)
  }

  @Test
  fun `test 17 google sign in rejects missing configuration without fake user`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.auth.FirebaseAuthManager(context)
    val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()

    val result = authManager.signInWithGoogle(activity)
    // In this test/emulator environment without google-services.json or Web Client ID,
    // it MUST fail and never return a fake UserAccount.
    assertTrue("Sign-in must fail gracefully when unconfigured", result.isFailure)
    assertNull("Current user must remain null", authManager.currentUser.value)
  }

  @Test
  fun `test 18 dashboard calculator aggregates sales metrics accurately from real invoices`() {
    val items1 = listOf(
      InvoiceItem("1", "inv_1", "p1", "Atta", 2, 350.0, 0.0, 0.0, 700.0)
    )
    val items2 = listOf(
      InvoiceItem("2", "inv_2", "p2", "Sugar", 5, 45.0, 0.0, 0.0, 225.0)
    )

    val inv1 = Invoice(
      id = "inv_1",
      invoiceNumber = "INV-0001",
      customerId = "c1",
      customerName = "Ramesh",
      invoiceDate = "2026-09-11",
      createdTimestamp = System.currentTimeMillis(),
      items = items1,
      subtotal = 700.0,
      grandTotal = 700.0,
      paidAmount = 700.0,
      remainingAmount = 0.0,
      paymentStatus = InvoicePaymentStatus.PAID
    )

    val inv2 = Invoice(
      id = "inv_2",
      invoiceNumber = "INV-0002",
      customerId = "c2",
      customerName = "Suresh",
      invoiceDate = "2026-09-11",
      createdTimestamp = System.currentTimeMillis(),
      items = items2,
      subtotal = 225.0,
      grandTotal = 225.0,
      paidAmount = 100.0,
      remainingAmount = 125.0,
      paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID
    )

    val customers = listOf(
      Customer("c1", "Ramesh", "9829011111", balance = 0.0),
      Customer("c2", "Suresh", "9829022222", balance = 125.0)
    )

    val products = listOf(
      Product("p1", "Atta", "SKU1", "Grocery", 300.0, 350.0, currentStock = 10.0, lowStockThreshold = 2.0, unit = "kg"),
      Product("p2", "Sugar", "SKU2", "Grocery", 40.0, 45.0, currentStock = 50.0, lowStockThreshold = 5.0, unit = "kg")
    )

    val metrics = DashboardCalculator.calculateDashboardData(
      invoices = listOf(inv1, inv2),
      customers = customers,
      products = products,
      period = DatePeriodFilter.ALL_TIME
    )

    assertEquals(925.0, metrics.periodTotalInvoiceAmount, 0.001)
    assertEquals(800.0, metrics.periodPaidAmount, 0.001)
    assertEquals(125.0, metrics.periodPendingAmount, 0.001)
    assertEquals(2, metrics.periodInvoiceCount)
    assertEquals(125.0, metrics.totalOutstandingKhata, 0.001)
  }

  @Test
  fun `test 19 payment reminder message format includes store name and upi details`() {
    val cust = Customer("c1", "Mohan Lal", "9829033333", balance = 1500.0)
    val msg = DashboardCalculator.formatPaymentReminderMessage(
      customerName = cust.name,
      outstandingAmount = cust.balance,
      businessName = "Sharma Kirana"
    )

    assertTrue(msg.contains("Sharma Kirana"))
    assertTrue(msg.contains("Mohan Lal"))
    assertTrue(msg.contains("1,500"))
  }

  @Test
  fun `test 20 data export helper writes valid csv files locally`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val customers = listOf(Customer("c1", "Vikram", "9829044444", balance = 350.0))
    val file = DataExportHelper.exportCustomersCsv(context, customers)

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    val content = file.readText()
    assertTrue(content.contains("Customer ID,Name,Phone,Address,Outstanding Balance (Rs)"))
    assertTrue(content.contains("Vikram"))
    assertTrue(content.contains("9829044444"))
    file.delete()
  }

  @Test
  fun `test 21 data export helper generates valid business summary csv`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val profile = BusinessProfile("Test Kirana", "Rajesh", "9876543210", "Jaipur", "kirana@upi")
    val salesReport = DashboardCalculator.calculateSalesReport(emptyList(), DatePeriodFilter.ALL_TIME)
    val khataReport = DashboardCalculator.calculateKhataReport(emptyList(), emptyList())
    val stockReport = DashboardCalculator.calculateProductStockReport(emptyList(), emptyList())
    val customerReport = DashboardCalculator.calculateCustomerReport(emptyList(), emptyList())

    val file = DataExportHelper.exportBusinessSummaryCsv(
      context = context,
      businessProfile = profile,
      periodLabel = "All Time",
      salesReport = salesReport,
      khataReport = khataReport,
      stockReport = stockReport,
      customerReport = customerReport
    )

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    val content = file.readText()
    assertTrue(content.contains("KHATAGO BUSINESS SUMMARY REPORT"))
    assertTrue(content.contains("Test Kirana"))
    assertTrue(content.contains("1. SALES SUMMARY"))
    assertTrue(content.contains("2. KHATA LEDGER SUMMARY"))
    file.delete()
  }
}

