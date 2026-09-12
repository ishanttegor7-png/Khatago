package com.example.model

/**
 * Period filter for dashboard and business reports.
 */
enum class DatePeriodFilter(val displayName: String) {
  TODAY("Today"),
  THIS_WEEK("This Week"),
  THIS_MONTH("This Month"),
  ALL_TIME("All Time"),
  CUSTOM_RANGE("Custom Range")
}

data class DateRange(
  val startMillis: Long,
  val endMillis: Long,
  val label: String
)

data class SalesDashboardData(
  val todaySales: Double,
  val thisWeekSales: Double,
  val thisMonthSales: Double,
  val periodTotalInvoiceAmount: Double,
  val periodPaidAmount: Double,
  val periodPendingAmount: Double,
  val periodInvoiceCount: Int,
  val totalCustomers: Int,
  val totalOutstandingKhata: Double,
  val totalDenaHaiKhata: Double,
  val totalProducts: Int,
  val lowStockCount: Int,
  val outOfStockCount: Int
)

data class DailySalesPoint(
  val dateLabel: String,
  val dateMillis: Long,
  val invoiceCount: Int,
  val totalSales: Double,
  val paidAmount: Double,
  val pendingAmount: Double
)

data class SalesReportData(
  val totalSales: Double,
  val invoiceCount: Int,
  val paidAmount: Double,
  val pendingAmount: Double,
  val partiallyPaidCount: Int,
  val fullyPaidCount: Int,
  val unpaidCount: Int,
  val dateWiseSales: List<DailySalesPoint>
)

data class CustomerOutstandingItem(
  val customer: Customer,
  val outstandingAmount: Double,
  val totalCredit: Double,
  val totalPayment: Double,
  val transactionCount: Int,
  val lastTransactionDate: String
)

data class KhataReportData(
  val totalCredit: Double,
  val totalPayments: Double,
  val totalOutstanding: Double,
  val customerOutstandingList: List<CustomerOutstandingItem>
)

data class ProductStockReportData(
  val totalProducts: Int,
  val totalStockUnits: Double,
  val totalStockValuation: Double,
  val totalRetailValuation: Double,
  val lowStockProducts: List<Product>,
  val outOfStockProducts: List<Product>,
  val movementTypeSummary: Map<StockMovementType, Int>,
  val totalMovementsCount: Int
)

data class TopCustomerItem(
  val customerId: String,
  val customerName: String,
  val customerPhone: String,
  val invoiceCount: Int,
  val totalInvoiceAmount: Double,
  val paidAmount: Double,
  val outstandingAmount: Double
)

data class CustomerReportData(
  val totalCustomers: Int,
  val customersWithBalanceCount: Int,
  val customersWithBalance: List<Customer>,
  val topCustomersByInvoice: List<TopCustomerItem>
)
