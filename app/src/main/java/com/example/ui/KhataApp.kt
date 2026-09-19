package com.example.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AddCustomerResult
import com.example.data.repository.InvoiceOperationResult
import com.example.data.repository.ProductOperationResult
import com.example.data.repository.StockOperationResult
import com.example.data.repository.TransactionResult
import com.example.model.PlanType
import com.example.model.SubscriptionEntitlement
import com.example.model.TransactionType
import com.example.payment.model.PaymentUiState
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.FirstLoginSyncChoiceDialog
import com.example.ui.components.KhataBottomBar
import com.example.ui.components.KhataTab
import com.example.ui.components.LimitReachedDialog
import com.example.ui.components.SignInDialog
import com.example.ui.screens.AddCustomerScreen
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceScreen
import com.example.ui.screens.KhataScreen
import com.example.ui.screens.PaymentRemindersScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun KhataApp(
  navController: NavHostController = rememberNavController(),
  viewModel: KhataViewModel = viewModel()
) {
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()
  val invoices by viewModel.invoices.collectAsStateWithLifecycle()
  val products by viewModel.products.collectAsStateWithLifecycle()
  val stockMovements by viewModel.stockMovements.collectAsStateWithLifecycle()
  val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
  val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
  val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
  val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
  val lastSyncMessage by viewModel.lastSyncMessage.collectAsStateWithLifecycle()
  val showFirstLoginDialog by viewModel.showFirstLoginDialog.collectAsStateWithLifecycle()
  val planMetrics by viewModel.planUsageMetrics.collectAsStateWithLifecycle()
  val currentEntitlement by viewModel.currentEntitlement.collectAsStateWithLifecycle()

  var limitDialogMessage by remember { mutableStateOf<String?>(null) }

  if (limitDialogMessage != null) {
    LimitReachedDialog(
      message = limitDialogMessage ?: "",
      onDismiss = { limitDialogMessage = null },
      onUpgradeClick = {
        limitDialogMessage = null
        navController.navigate("premium")
      }
    )
  }

  val context = LocalContext.current
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  if (showFirstLoginDialog) {
    FirstLoginSyncChoiceDialog(
      onDismiss = { viewModel.dismissFirstLoginDialog() },
      onChoice = { choice ->
        viewModel.handleFirstLoginChoice(choice)
        coroutineScope.launch {
          snackbarHostState.showSnackbar("Ledger sync preference applied!")
        }
      }
    )
  }

  // Home Quick Action Transaction Dialog
  var showHomeAddTransactionDialog by remember { mutableStateOf(false) }
  var homeTransactionType by remember { mutableStateOf(TransactionType.CREDIT) }

  if (showHomeAddTransactionDialog) {
    AddTransactionDialog(
      initialType = homeTransactionType,
      customerList = customers,
      onDismiss = { showHomeAddTransactionDialog = false },
      onConfirm = { cId, cName, type, amt, note, onError ->
        viewModel.addTransaction(cId, cName, type, amt, note) { res ->
          when (res) {
            is TransactionResult.Success -> {
              showHomeAddTransactionDialog = false
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Ledger entry saved successfully!")
              }
            }
            is TransactionResult.Error -> {
              if (res.message.contains("limit reached", ignoreCase = true)) {
                showHomeAddTransactionDialog = false
                limitDialogMessage = res.message
              } else {
                onError(res.message)
              }
            }
          }
        }
      }
    )
  }

  // Business Data Export Dialog
  var showExportDialog by remember { mutableStateOf(false) }

  if (showExportDialog) {
    ExportDialog(
      businessProfile = businessProfile,
      customers = customers,
      transactions = transactions,
      invoices = invoices,
      products = products,
      stockMovements = stockMovements,
      isCsvExportAllowed = planMetrics.isCsvExportAllowed,
      canExportPdf = planMetrics.monthlyPdfExportCount < planMetrics.maxMonthlyPdfExports,
      onRecordPdfExport = {
        coroutineScope.launch {
          viewModel.recordPdfExport("PDF_REPORT")
        }
      },
      onUpgradeClick = {
        showExportDialog = false
        navController.navigate("premium")
      },
      onShowLimitMessage = { msg ->
        showExportDialog = false
        limitDialogMessage = msg
      },
      onDismiss = { showExportDialog = false }
    )
  }

  val bottomBarRoutes = setOf(
    KhataTab.HOME.route,
    KhataTab.CUSTOMERS.route,
    KhataTab.PRODUCTS.route,
    KhataTab.KHATA.route,
    KhataTab.INVOICE.route,
    KhataTab.SETTINGS.route
  )

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (currentRoute in bottomBarRoutes) {
        KhataBottomBar(
          currentRoute = currentRoute,
          onTabSelected = { tab ->
            if (currentRoute != tab.route) {
              navController.navigate(tab.route) {
                popUpTo(KhataTab.HOME.route) {
                  saveState = true
                }
                launchSingleTop = true
                restoreState = true
              }
            }
          }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
      NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize()
      ) {
        // 1. Splash Screen
        composable("splash") {
          SplashScreen(
            onSplashComplete = {
              navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
              }
            }
          )
        }

        // 2. Home Screen
        composable("home") {
          HomeScreen(
            businessProfile = businessProfile,
            totalLenaHai = viewModel.totalLenaHai,
            totalDenaHai = viewModel.totalDenaHai,
            todaysHisaab = viewModel.todaysHisaab,
            customers = customers,
            recentTransactions = transactions,
            todayInvoicesCount = viewModel.todayInvoicesCount,
            todayInvoicesAmount = viewModel.todayInvoicesAmount,
            pendingInvoicesAmount = viewModel.pendingInvoicesAmount,
            syncStatus = syncStatus,
            onSyncClick = {
              navController.navigate("settings")
            },
            onAddUdhaarClick = {
              homeTransactionType = TransactionType.CREDIT
              showHomeAddTransactionDialog = true
            },
            onAddPaymentClick = {
              homeTransactionType = TransactionType.PAYMENT
              showHomeAddTransactionDialog = true
            },
            onAddCustomerClick = {
              navController.navigate("add_customer")
            },
            onCreateInvoiceClick = {
              navController.navigate("create_invoice")
            },
            onViewAllInvoicesClick = {
              navController.navigate("invoice")
            },
            onCustomerClick = { customerId ->
              navController.navigate("customer_detail/$customerId")
            },
            onViewAllTransactionsClick = {
              navController.navigate("khata")
            },
            onViewAllCustomersClick = {
              navController.navigate("customers")
            },
            products = products,
            onViewProductsClick = {
              navController.navigate("products")
            },
            onViewSalesDashboardClick = {
              navController.navigate("sales_dashboard")
            },
            onViewReportsClick = {
              navController.navigate("reports")
            },
            onViewPaymentRemindersClick = {
              navController.navigate("payment_reminders")
            },
            onViewKhataClick = {
              navController.navigate("khata")
            }
          )
        }

        // 3. Customers Screen
        composable("customers") {
          CustomersScreen(
            customers = customers,
            onCustomerClick = { customerId ->
              navController.navigate("customer_detail/$customerId")
            },
            onAddCustomerClick = {
              navController.navigate("add_customer")
            }
          )
        }

        // 4. Khata Screen
        composable("khata") {
          KhataScreen(
            transactions = transactions,
            customers = customers,
            totalLenaHai = viewModel.totalLenaHai,
            totalDenaHai = viewModel.totalDenaHai,
            onTransactionClick = { customerId ->
              navController.navigate("customer_detail/$customerId")
            },
            onAddTransaction = { cId, cName, type, amt, note, onError ->
              viewModel.addTransaction(cId, cName, type, amt, note) { res ->
                when (res) {
                  is TransactionResult.Success -> {
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Ledger entry added!")
                    }
                  }
                  is TransactionResult.Error -> {
                    if (res.message.contains("limit reached", ignoreCase = true)) {
                      limitDialogMessage = res.message
                    } else {
                      onError(res.message)
                    }
                  }
                }
              }
            }
          )
        }

        // 5. Products & Stock Screen
        composable("products") {
          ProductsScreen(
            products = products,
            stockMovements = stockMovements,
            onAddProductClick = {
              navController.navigate("add_product")
            },
            onProductClick = { productId ->
              navController.navigate("product_detail/$productId")
            },
            onStockIn = { pId, qty, reason ->
              coroutineScope.launch {
                val res = viewModel.stockInAsync(pId, qty, reason)
                when (res) {
                  is StockOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Added $qty stock!")
                  }
                  is StockOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            },
            onStockOut = { pId, qty, reason ->
              coroutineScope.launch {
                val res = viewModel.stockOutAsync(pId, qty, reason)
                when (res) {
                  is StockOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Deducted $qty stock!")
                  }
                  is StockOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            }
          )
        }

        // 6. Invoice Screen
        composable("invoice") {
          InvoiceScreen(
            invoices = invoices,
            onCreateInvoiceClick = {
              navController.navigate("create_invoice")
            },
            onInvoiceClick = { invoiceId ->
              navController.navigate("invoice_detail/$invoiceId")
            }
          )
        }

        // 7. Settings Screen
        composable("settings") {
          SettingsScreen(
            businessProfile = businessProfile,
            selectedLanguage = selectedLanguage,
            selectedTheme = selectedTheme,
            notificationsEnabled = notificationsEnabled,
            currentUser = currentUser,
            syncStatus = syncStatus,
            lastSyncTimestamp = lastSyncTimestamp,
            lastSyncMessage = lastSyncMessage,
            isFirebaseConfigured = viewModel.isFirebaseConfigured(),
            firebaseNotice = viewModel.getFirebaseNotice(),
            planMetrics = planMetrics,
            onOpenBusinessProfile = {
              navController.navigate("business_profile")
            },
            onOpenPremium = {
              navController.navigate("premium")
            },
            onLanguageChange = { lang ->
              viewModel.setLanguage(lang)
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Language changed to $lang")
              }
            },
            onThemeChange = { theme ->
              viewModel.setTheme(theme)
            },
            onToggleNotifications = {
              viewModel.toggleNotifications()
            },
            onSignInGoogle = { onResult ->
              val act = context as? Activity
              if (act != null) {
                viewModel.signInWithGoogle(act) { success, msg ->
                  onResult(success, msg)
                  if (success) {
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Signed in with Google successfully!")
                    }
                  }
                }
              } else {
                onResult(false, "Activity context unavailable")
              }
            },
            onSignInDemo = {
              viewModel.signInWithDemoAccount { success, msg ->
                if (success) {
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar("Signed in with Shop Profile!")
                  }
                }
              }
            },
            onSendPhoneOtp = { phone, onCodeSent, onError ->
              val act = context as? Activity
              if (act != null) {
                viewModel.sendPhoneOtp(
                  activity = act,
                  phoneNumber = phone,
                  onCodeSent = onCodeSent,
                  onError = onError,
                  onAutoVerified = {
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Phone verified and signed in!")
                    }
                  }
                )
              } else {
                onError("Activity context unavailable")
              }
            },
            onResendPhoneOtp = { phone, onCodeSent, onError ->
              val act = context as? Activity
              if (act != null) {
                viewModel.resendPhoneOtp(act, phone, onCodeSent, onError)
              } else {
                onError("Activity context unavailable")
              }
            },
            onVerifyPhoneOtp = { vId, otp, onResult ->
              viewModel.verifyPhoneOtp(vId, otp) { success, msg ->
                onResult(success, msg)
                if (success) {
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar("Phone verified and signed in!")
                  }
                }
              }
            },
            onSignOut = {
              viewModel.signOut()
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Signed out successfully.")
              }
            },
            onSyncNow = {
              viewModel.syncNow { success, msg ->
                coroutineScope.launch {
                  snackbarHostState.showSnackbar(msg ?: if (success) "Synced with cloud" else "Sync failed")
                }
              }
            },
            onRestoreData = {
              viewModel.restoreData { success, msg ->
                coroutineScope.launch {
                  snackbarHostState.showSnackbar(msg ?: if (success) "Restored from cloud" else "Restore failed")
                }
              }
            }
          )
        }

        // 7. Add Customer Screen
        composable("add_customer") {
          AddCustomerScreen(
            onSaveCustomer = { name, phone, address, onError ->
              viewModel.addCustomer(name, phone, address) { res ->
                when (res) {
                  is AddCustomerResult.Success -> {
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Customer $name added successfully!")
                    }
                    navController.popBackStack()
                  }
                  is AddCustomerResult.Error -> {
                    if (res.message.contains("limit reached", ignoreCase = true)) {
                      limitDialogMessage = res.message
                    } else {
                      onError(res.message)
                    }
                  }
                }
              }
            },
            onCancel = {
              navController.popBackStack()
            }
          )
        }

        // 8. Customer Detail Screen
        composable(
          route = "customer_detail/{customerId}",
          arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
          val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
          val customer by viewModel.getCustomerStream(customerId)
            .collectAsStateWithLifecycle(initialValue = viewModel.getCustomer(customerId))
          val customerTransactions by viewModel.getTransactionsStreamForCustomer(customerId)
            .collectAsStateWithLifecycle(initialValue = viewModel.getTransactionsForCustomer(customerId))

          val currentCust = customer
          if (currentCust != null) {
            CustomerDetailScreen(
              customer = currentCust,
              transactions = customerTransactions,
              businessName = businessProfile.businessName,
              onBack = { navController.popBackStack() },
              onAddTransaction = { cId, cName, type, amt, note, onError ->
                viewModel.addTransaction(cId, cName, type, amt, note) { res ->
                  when (res) {
                    is TransactionResult.Success -> {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction recorded for ${currentCust.name}!")
                      }
                    }
                    is TransactionResult.Error -> {
                      if (res.message.contains("limit reached", ignoreCase = true)) {
                        limitDialogMessage = res.message
                      } else {
                        onError(res.message)
                      }
                    }
                  }
                }
              },
              onEditCustomer = { id, name, phone, address ->
                coroutineScope.launch {
                  val updated = viewModel.updateCustomerAsync(id, name, phone, address)
                  if (updated) {
                    snackbarHostState.showSnackbar("Customer details updated!")
                  }
                }
              },
              onDeleteCustomer = { id ->
                coroutineScope.launch {
                  val deleted = viewModel.deleteCustomerAsync(id)
                  if (deleted) {
                    snackbarHostState.showSnackbar("Customer removed from hisaab")
                  }
                }
              },
              onSendReminderConfirmation = { method ->
                coroutineScope.launch {
                  snackbarHostState.showSnackbar("Payment reminder sent via $method to ${currentCust.name}!")
                }
              }
            )
          }
        }

        // 9. Create Invoice Screen
        composable("create_invoice") {
          CreateInvoiceScreen(
            customerList = customers,
            productList = products,
            onSaveInvoice = { custId, cName, cPhone, cAddr, items, disc, taxOn, taxRate, paid, notes, due ->
              coroutineScope.launch {
                val res = viewModel.createInvoiceAsync(
                  customerId = custId,
                  customerName = cName,
                  customerPhone = cPhone,
                  customerAddress = cAddr,
                  items = items,
                  overallDiscount = disc,
                  taxEnabled = taxOn,
                  taxRate = taxRate,
                  paidAmount = paid,
                  notes = notes,
                  dueDate = due
                )
                when (res) {
                  is InvoiceOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Invoice ${res.invoice.invoiceNumber} created successfully!")
                    navController.navigate("invoice_detail/${res.invoice.id}") {
                      popUpTo("invoice")
                    }
                  }
                  is InvoiceOperationResult.Error -> {
                    if (res.message.contains("limit reached", ignoreCase = true)) {
                      limitDialogMessage = res.message
                    } else {
                      snackbarHostState.showSnackbar(res.message)
                    }
                  }
                }
              }
            },
            onCancel = {
              navController.popBackStack()
            }
          )
        }

        // 10. Edit Invoice Screen
        composable(
          route = "edit_invoice/{invoiceId}",
          arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStackEntry ->
          val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
          val invoice = invoices.find { it.id == invoiceId }
          CreateInvoiceScreen(
            customerList = customers,
            productList = products,
            existingInvoice = invoice,
            onSaveInvoice = { custId, cName, cPhone, cAddr, items, disc, taxOn, taxRate, paid, notes, due ->
              coroutineScope.launch {
                val res = viewModel.updateInvoiceAsync(
                  invoiceId = invoiceId,
                  customerId = custId,
                  customerName = cName,
                  customerPhone = cPhone,
                  customerAddress = cAddr,
                  items = items,
                  overallDiscount = disc,
                  taxEnabled = taxOn,
                  taxRate = taxRate,
                  notes = notes,
                  dueDate = due
                )
                when (res) {
                  is InvoiceOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Invoice updated successfully!")
                    navController.popBackStack()
                  }
                  is InvoiceOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            },
            onCancel = {
              navController.popBackStack()
            }
          )
        }

        // 11. Invoice Detail Screen
        composable(
          route = "invoice_detail/{invoiceId}",
          arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStackEntry ->
          val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
          val invoice = invoices.find { it.id == invoiceId }
          InvoiceDetailScreen(
            invoice = invoice,
            onBackClick = { navController.popBackStack() },
            onEditClick = { id -> navController.navigate("edit_invoice/$id") },
            onDeleteClick = { id ->
              coroutineScope.launch {
                val deleted = viewModel.deleteInvoiceAsync(id)
                if (deleted) {
                  snackbarHostState.showSnackbar("Invoice deleted")
                  navController.popBackStack()
                }
              }
            },
            onRecordPayment = { amount, note ->
              coroutineScope.launch {
                val res = viewModel.recordInvoicePaymentAsync(invoiceId, amount, note)
                when (res) {
                  is InvoiceOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Payment of ₹$amount recorded!")
                  }
                  is InvoiceOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            }
          )
        }

        // 12. Add Product Screen
        composable("add_product") {
          AddEditProductScreen(
            onSave = { name, sku, category, purchasePrice, sellingPrice, initialStock, threshold, unit ->
              coroutineScope.launch {
                val res = viewModel.addProductAsync(
                  name = name,
                  sku = sku,
                  category = category,
                  purchasePrice = purchasePrice,
                  sellingPrice = sellingPrice,
                  initialStock = initialStock,
                  lowStockThreshold = threshold,
                  unit = unit
                )
                when (res) {
                  is ProductOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Product '${res.product.name}' added to catalog!")
                    navController.popBackStack()
                  }
                  is ProductOperationResult.Error -> {
                    if (res.message.contains("limit reached", ignoreCase = true)) {
                      limitDialogMessage = res.message
                    } else {
                      snackbarHostState.showSnackbar(res.message)
                    }
                  }
                }
              }
            },
            onBack = { navController.popBackStack() }
          )
        }

        // 13. Edit Product Screen
        composable(
          route = "edit_product/{productId}",
          arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
          val productId = backStackEntry.arguments?.getString("productId") ?: ""
          val currentProd = products.find { it.id == productId }
          AddEditProductScreen(
            existingProduct = currentProd,
            onSave = { name, sku, category, purchasePrice, sellingPrice, _, threshold, unit ->
              coroutineScope.launch {
                val res = viewModel.updateProductAsync(
                  id = productId,
                  name = name,
                  sku = sku,
                  category = category,
                  purchasePrice = purchasePrice,
                  sellingPrice = sellingPrice,
                  lowStockThreshold = threshold,
                  unit = unit
                )
                when (res) {
                  is ProductOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Product '${res.product.name}' updated!")
                    navController.popBackStack()
                  }
                  is ProductOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            },
            onBack = { navController.popBackStack() }
          )
        }

        // 14. Product Detail Screen
        composable(
          route = "product_detail/{productId}",
          arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
          val productId = backStackEntry.arguments?.getString("productId") ?: ""
          val currentProd = products.find { it.id == productId }
          val movementsForProd = stockMovements.filter { it.productId == productId }

          ProductDetailScreen(
            product = currentProd,
            movements = movementsForProd,
            onBack = { navController.popBackStack() },
            onEdit = { id -> navController.navigate("edit_product/$id") },
            onToggleActive = { id, active ->
              viewModel.toggleProductActive(id, active)
            },
            onDelete = { id ->
              coroutineScope.launch {
                viewModel.deleteProductAsync(id)
                snackbarHostState.showSnackbar("Product deleted")
                navController.popBackStack()
              }
            },
            onStockIn = { id, qty, reason ->
              coroutineScope.launch {
                val res = viewModel.stockInAsync(id, qty, reason)
                when (res) {
                  is StockOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Added $qty stock successfully!")
                  }
                  is StockOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            },
            onStockOut = { id, qty, reason ->
              coroutineScope.launch {
                val res = viewModel.stockOutAsync(id, qty, reason)
                when (res) {
                  is StockOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Deducted $qty stock!")
                  }
                  is StockOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            },
            onStockAdjust = { id, newStock, reason ->
              coroutineScope.launch {
                val res = viewModel.adjustStockAsync(id, newStock, reason)
                when (res) {
                  is StockOperationResult.Success -> {
                    snackbarHostState.showSnackbar("Stock adjusted to $newStock!")
                  }
                  is StockOperationResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                  }
                }
              }
            }
          )
        }

        // 15. Business Profile Screen
        composable("business_profile") {
          BusinessProfileScreen(
            profile = businessProfile,
            onSaveProfile = { bName, oName, phone, address, upi ->
              viewModel.updateBusinessProfile(bName, oName, phone, address, upi)
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Business profile updated!")
              }
            },
            onBack = {
              navController.popBackStack()
            }
          )
        }

        // 16. Sales Dashboard Screen
        composable("sales_dashboard") {
          SalesDashboardScreen(
            invoices = invoices,
            customers = customers,
            products = products,
            onBackClick = { navController.popBackStack() },
            onNavigateToReports = { navController.navigate("reports") },
            onNavigateToReminders = { navController.navigate("payment_reminders") },
            onNavigateToInvoices = { navController.navigate("invoice") },
            onNavigateToProducts = { navController.navigate("products") },
            onNavigateToKhata = { navController.navigate("khata") },
            onNavigateToCustomers = { navController.navigate("customers") },
            onExportClick = { showExportDialog = true }
          )
        }

        // 17. Reports Screen
        composable("reports") {
          ReportsScreen(
            businessProfile = businessProfile,
            customers = customers,
            transactions = transactions,
            invoices = invoices,
            products = products,
            stockMovements = stockMovements,
            onBackClick = { navController.popBackStack() },
            onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") },
            onProductClick = { productId -> navController.navigate("product_detail/$productId") },
            onInvoiceClick = { invoiceId -> navController.navigate("invoice_detail/$invoiceId") }
          )
        }

        // 18. Payment Reminders Screen
        composable("payment_reminders") {
          PaymentRemindersScreen(
            customers = customers,
            businessName = businessProfile.businessName,
            onBackClick = { navController.popBackStack() },
            onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") }
          )
        }

        // 19. Premium Screen
        composable("premium") {
          val paymentState by viewModel.paymentUiState.collectAsStateWithLifecycle()
          val currentActivity = context as? Activity
          var showSignInDialog by remember { mutableStateOf(false) }

          if (showSignInDialog) {
            SignInDialog(
              onDismiss = { showSignInDialog = false },
              onSignInGoogle = { onResult ->
                val act = context as? Activity
                if (act != null) {
                  viewModel.signInWithGoogle(act) { success, msg ->
                    onResult(success, msg)
                    if (success) {
                      showSignInDialog = false
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Signed in with Google successfully! You can now subscribe.")
                      }
                    }
                  }
                } else {
                  onResult(false, "Activity context unavailable")
                }
              },
              onSignInDemo = {
                viewModel.signInWithDemoAccount { success, msg ->
                  if (success) {
                    showSignInDialog = false
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Signed in with Shop Profile!")
                    }
                  }
                }
              },
              onSendPhoneOtp = { phone, onCodeSent, onError ->
                val act = context as? Activity
                if (act != null) {
                  viewModel.sendPhoneOtp(
                    activity = act,
                    phoneNumber = phone,
                    onCodeSent = onCodeSent,
                    onError = onError,
                    onAutoVerified = {
                      showSignInDialog = false
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Phone verified and signed in!")
                      }
                    }
                  )
                } else {
                  onError("Activity context unavailable")
                }
              },
              onVerifyPhoneOtp = { verificationId, otp, onResult ->
                viewModel.verifyPhoneOtp(verificationId, otp) { success, msg ->
                  onResult(success, msg)
                  if (success) {
                    showSignInDialog = false
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("Phone sign-in successful!")
                    }
                  }
                }
              },
              onResendPhoneOtp = { phone, onCodeSent, onError ->
                val act = context as? Activity
                if (act != null) {
                  viewModel.resendPhoneOtp(
                    activity = act,
                    phoneNumber = phone,
                    onCodeSent = onCodeSent,
                    onError = onError
                  )
                }
              }
            )
          }

          when (val state = paymentState) {
            is PaymentUiState.Processing -> {
              AlertDialog(
                onDismissRequest = {},
                title = { Text("Processing") },
                text = { Text(state.message) },
                confirmButton = {}
              )
            }
            is PaymentUiState.LoginRequired -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_login_required_premium"),
                title = { Text("Sign In Required") },
                text = { Text(state.message) },
                confirmButton = {
                  Button(
                    onClick = {
                      viewModel.resetPaymentUiState()
                      showSignInDialog = true
                    },
                    modifier = Modifier.testTag("btn_go_to_login")
                  ) {
                    Text("Sign In Now")
                  }
                },
                dismissButton = {
                  androidx.compose.material3.TextButton(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_cancel_login_required")
                  ) {
                    Text("Cancel")
                  }
                }
              )
            }
            is PaymentUiState.Message -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_message"),
                title = { Text(state.title) },
                text = { Text(state.text) },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_message")
                  ) {
                    Text("OK")
                  }
                }
              )
            }
            is PaymentUiState.PaymentStarted -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_started"),
                title = { Text("Pending Purchase Created") },
                text = {
                  Text("Pending purchase order ${state.orderId} created for ${state.planType.displayName} (₹${state.amount.toInt()}).\n\nStatus: PENDING.\n\nPremium will remain locked until verified by the trusted backend.")
                },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_started")
                  ) {
                    Text("OK")
                  }
                }
              )
            }
            is PaymentUiState.AwaitingVerification -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_verifying"),
                title = { Text("Verifying Payment") },
                text = { Text(state.message) },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_verifying")
                  ) {
                    Text("Dismiss")
                  }
                }
              )
            }
            is PaymentUiState.Success -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_success"),
                title = { Text("KhataGo Premium Unlocked") },
                text = {
                  Text("Congratulations! Your ${state.planType.displayName} is now active and verified by the server.")
                },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_success")
                  ) {
                    Text("Awesome")
                  }
                }
              )
            }
            is PaymentUiState.Failed -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_failed"),
                title = { Text("Payment Failed") },
                text = { Text(state.message) },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_failed")
                  ) {
                    Text("OK")
                  }
                }
              )
            }
            is PaymentUiState.Cancelled -> {
              AlertDialog(
                onDismissRequest = { viewModel.resetPaymentUiState() },
                modifier = Modifier.testTag("dialog_payment_cancelled"),
                title = { Text("Payment Cancelled") },
                text = { Text("The payment was cancelled. You have not been charged.") },
                confirmButton = {
                  Button(
                    onClick = { viewModel.resetPaymentUiState() },
                    modifier = Modifier.testTag("btn_close_payment_cancelled")
                  ) {
                    Text("OK")
                  }
                }
              )
            }
            else -> {}
          }

          PremiumScreen(
            planMetrics = planMetrics,
            onBackClick = { navController.popBackStack() },
            onChooseMonthly = {
              if (currentUser == null) {
                showSignInDialog = true
              } else {
                viewModel.startMonthlyPurchase(currentActivity)
              }
            },
            onChooseYearly = {
              if (currentUser == null) {
                showSignInDialog = true
              } else {
                viewModel.startYearlyPurchase(currentActivity)
              }
            },
            onRestoreClick = {
              viewModel.restorePremium()
            }
          )
        }
      }
    }
  }
}
