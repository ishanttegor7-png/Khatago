package com.example.util

import com.example.model.Customer
import com.example.model.CustomerOutstandingItem
import com.example.model.CustomerReportData
import com.example.model.DailySalesPoint
import com.example.model.DatePeriodFilter
import com.example.model.DateRange
import com.example.model.Invoice
import com.example.model.InvoicePaymentStatus
import com.example.model.KhataReportData
import com.example.model.Product
import com.example.model.ProductStockReportData
import com.example.model.SalesDashboardData
import com.example.model.SalesReportData
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.model.TopCustomerItem
import com.example.model.Transaction
import com.example.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DashboardCalculator {

  private val indianCurrencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

  fun getPeriodDateRange(
    period: DatePeriodFilter,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): DateRange {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }

    return when (period) {
      DatePeriodFilter.TODAY -> {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        DateRange(start, end, "Today")
      }
      DatePeriodFilter.THIS_WEEK -> {
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        DateRange(start, end, "This Week")
      }
      DatePeriodFilter.THIS_MONTH -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        DateRange(start, end, "This Month")
      }
      DatePeriodFilter.ALL_TIME -> {
        DateRange(0L, Long.MAX_VALUE, "All Time")
      }
      DatePeriodFilter.CUSTOM_RANGE -> {
        val start = customStartMillis ?: 0L
        val end = customEndMillis ?: Long.MAX_VALUE
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val label = if (customStartMillis != null && customEndMillis != null) {
          "${dateFormat.format(Date(customStartMillis))} - ${dateFormat.format(Date(customEndMillis))}"
        } else {
          "Custom Range"
        }
        DateRange(start, end, label)
      }
    }
  }

  fun calculateDashboardData(
    invoices: List<Invoice>,
    customers: List<Customer>,
    products: List<Product>,
    period: DatePeriodFilter = DatePeriodFilter.THIS_MONTH,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): SalesDashboardData {
    val todayRange = getPeriodDateRange(DatePeriodFilter.TODAY, nowMillis = nowMillis)
    val weekRange = getPeriodDateRange(DatePeriodFilter.THIS_WEEK, nowMillis = nowMillis)
    val monthRange = getPeriodDateRange(DatePeriodFilter.THIS_MONTH, nowMillis = nowMillis)
    val activePeriodRange = getPeriodDateRange(period, customStartMillis, customEndMillis, nowMillis)

    val todaySales = invoices
      .filter { it.createdTimestamp in todayRange.startMillis..todayRange.endMillis }
      .sumOf { it.grandTotal }

    val thisWeekSales = invoices
      .filter { it.createdTimestamp in weekRange.startMillis..weekRange.endMillis }
      .sumOf { it.grandTotal }

    val thisMonthSales = invoices
      .filter { it.createdTimestamp in monthRange.startMillis..monthRange.endMillis }
      .sumOf { it.grandTotal }

    val periodInvoices = invoices.filter {
      it.createdTimestamp in activePeriodRange.startMillis..activePeriodRange.endMillis
    }

    val periodTotalInvoiceAmount = periodInvoices.sumOf { it.grandTotal }
    val periodPaidAmount = periodInvoices.sumOf { it.paidAmount }
    val periodPendingAmount = periodInvoices.sumOf { it.remainingAmount }

    val totalOutstandingKhata = customers.filter { it.balance > 0 }.sumOf { it.balance }
    val totalDenaHaiKhata = customers.filter { it.balance < 0 }.sumOf { -it.balance }

    val activeProducts = products.filter { it.isActive }
    val lowStockCount = activeProducts.count { it.currentStock > 0 && it.currentStock <= it.lowStockThreshold }
    val outOfStockCount = activeProducts.count { it.currentStock <= 0.0 }

    return SalesDashboardData(
      todaySales = todaySales,
      thisWeekSales = thisWeekSales,
      thisMonthSales = thisMonthSales,
      periodTotalInvoiceAmount = periodTotalInvoiceAmount,
      periodPaidAmount = periodPaidAmount,
      periodPendingAmount = periodPendingAmount,
      periodInvoiceCount = periodInvoices.size,
      totalCustomers = customers.size,
      totalOutstandingKhata = totalOutstandingKhata,
      totalDenaHaiKhata = totalDenaHaiKhata,
      totalProducts = products.size,
      lowStockCount = lowStockCount,
      outOfStockCount = outOfStockCount
    )
  }

  fun calculateSalesReport(
    invoices: List<Invoice>,
    period: DatePeriodFilter = DatePeriodFilter.THIS_MONTH,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): SalesReportData {
    val dateRange = getPeriodDateRange(period, customStartMillis, customEndMillis, nowMillis)
    val periodInvoices = invoices.filter {
      it.createdTimestamp in dateRange.startMillis..dateRange.endMillis
    }

    val totalSales = periodInvoices.sumOf { it.grandTotal }
    val paidAmount = periodInvoices.sumOf { it.paidAmount }
    val pendingAmount = periodInvoices.sumOf { it.remainingAmount }

    val partiallyPaidCount = periodInvoices.count { it.paymentStatus == InvoicePaymentStatus.PARTIALLY_PAID }
    val fullyPaidCount = periodInvoices.count { it.paymentStatus == InvoicePaymentStatus.PAID }
    val unpaidCount = periodInvoices.count { it.paymentStatus == InvoicePaymentStatus.PENDING }

    // Group invoices by calendar day (newest first)
    val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val groupedByDay = periodInvoices
      .groupBy { inv -> dayFormat.format(Date(inv.createdTimestamp)) }
      .map { (dateLabel, dayInvoices) ->
        val representativeMillis = dayInvoices.maxOfOrNull { it.createdTimestamp } ?: 0L
        DailySalesPoint(
          dateLabel = dateLabel,
          dateMillis = representativeMillis,
          invoiceCount = dayInvoices.size,
          totalSales = dayInvoices.sumOf { it.grandTotal },
          paidAmount = dayInvoices.sumOf { it.paidAmount },
          pendingAmount = dayInvoices.sumOf { it.remainingAmount }
        )
      }
      .sortedByDescending { it.dateMillis }

    return SalesReportData(
      totalSales = totalSales,
      invoiceCount = periodInvoices.size,
      paidAmount = paidAmount,
      pendingAmount = pendingAmount,
      partiallyPaidCount = partiallyPaidCount,
      fullyPaidCount = fullyPaidCount,
      unpaidCount = unpaidCount,
      dateWiseSales = groupedByDay
    )
  }

  fun calculateKhataReport(
    customers: List<Customer>,
    transactions: List<Transaction>,
    period: DatePeriodFilter = DatePeriodFilter.ALL_TIME,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): KhataReportData {
    val dateRange = getPeriodDateRange(period, customStartMillis, customEndMillis, nowMillis)

    val periodTransactions = if (period == DatePeriodFilter.ALL_TIME) {
      transactions
    } else {
      transactions.filter { it.timestamp in dateRange.startMillis..dateRange.endMillis }
    }

    val totalCredit = periodTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    val totalPayments = periodTransactions.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }

    // Total outstanding across all customers: Total Lena Hai
    val totalOutstanding = customers.filter { it.balance > 0 }.sumOf { it.balance }

    val customerOutstandingList = customers
      .filter { it.balance > 0 }
      .map { cust ->
        CustomerOutstandingItem(
          customer = cust,
          outstandingAmount = cust.balance,
          totalCredit = cust.totalCredit,
          totalPayment = cust.totalPayment,
          transactionCount = cust.transactionCount,
          lastTransactionDate = cust.lastUpdated
        )
      }
      .sortedByDescending { it.outstandingAmount }

    return KhataReportData(
      totalCredit = totalCredit,
      totalPayments = totalPayments,
      totalOutstanding = totalOutstanding,
      customerOutstandingList = customerOutstandingList
    )
  }

  fun calculateProductStockReport(
    products: List<Product>,
    stockMovements: List<StockMovement>,
    period: DatePeriodFilter = DatePeriodFilter.ALL_TIME,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): ProductStockReportData {
    val dateRange = getPeriodDateRange(period, customStartMillis, customEndMillis, nowMillis)

    val periodMovements = if (period == DatePeriodFilter.ALL_TIME) {
      stockMovements
    } else {
      stockMovements.filter { it.timestamp in dateRange.startMillis..dateRange.endMillis }
    }

    val activeProducts = products.filter { it.isActive }
    val totalStockUnits = activeProducts.sumOf { it.currentStock }
    val totalStockValuation = activeProducts.sumOf { it.currentStock * it.purchasePrice }
    val totalRetailValuation = activeProducts.sumOf { it.currentStock * it.sellingPrice }

    val lowStockProducts = activeProducts.filter { it.currentStock > 0 && it.currentStock <= it.lowStockThreshold }
      .sortedBy { it.currentStock }

    val outOfStockProducts = activeProducts.filter { it.currentStock <= 0.0 }
      .sortedBy { it.name }

    val movementTypeSummary = periodMovements.groupBy { it.movementType }
      .mapValues { it.value.size }

    return ProductStockReportData(
      totalProducts = products.size,
      totalStockUnits = totalStockUnits,
      totalStockValuation = totalStockValuation,
      totalRetailValuation = totalRetailValuation,
      lowStockProducts = lowStockProducts,
      outOfStockProducts = outOfStockProducts,
      movementTypeSummary = movementTypeSummary,
      totalMovementsCount = periodMovements.size
    )
  }

  fun calculateCustomerReport(
    customers: List<Customer>,
    invoices: List<Invoice>
  ): CustomerReportData {
    val customersWithBalance = customers.filter { it.balance > 0 }.sortedByDescending { it.balance }

    val invoicesByCustomer = invoices.groupBy { it.customerId }
    val topCustomersByInvoice = invoicesByCustomer.map { (customerId, custInvoices) ->
      val firstInv = custInvoices.first()
      val matchedCustomer = customers.find { it.id == customerId }
      val name = matchedCustomer?.name ?: firstInv.customerName
      val phone = matchedCustomer?.phone ?: firstInv.customerPhone
      val totalInvAmt = custInvoices.sumOf { it.grandTotal }
      val totalPaid = custInvoices.sumOf { it.paidAmount }
      val totalRemaining = custInvoices.sumOf { it.remainingAmount }

      TopCustomerItem(
        customerId = customerId,
        customerName = name,
        customerPhone = phone,
        invoiceCount = custInvoices.size,
        totalInvoiceAmount = totalInvAmt,
        paidAmount = totalPaid,
        outstandingAmount = totalRemaining
      )
    }.sortedByDescending { it.totalInvoiceAmount }

    return CustomerReportData(
      totalCustomers = customers.size,
      customersWithBalanceCount = customersWithBalance.size,
      customersWithBalance = customersWithBalance,
      topCustomersByInvoice = topCustomersByInvoice
    )
  }

  /**
   * Pre-filled reminder message strictly following user prompt:
   * "Hello [Customer Name], your pending amount with us is ₹[Amount]. Please make the payment when convenient. Thank you."
   */
  fun formatPaymentReminderMessage(
    customerName: String,
    outstandingAmount: Double,
    businessName: String = ""
  ): String {
    val formattedAmt = indianCurrencyFormat.format(outstandingAmount.toInt())
    val withUs = if (businessName.isNotBlank()) "with $businessName" else "with us"
    return "Hello $customerName, your pending amount $withUs is ₹$formattedAmt. Please make the payment when convenient. Thank you."
  }
}
