package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.DatePeriodFilter
import com.example.model.Invoice
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.model.Transaction
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.DashboardCalculator
import com.example.util.DataExportHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  businessProfile: BusinessProfile,
  invoices: List<Invoice>,
  customers: List<Customer>,
  transactions: List<Transaction>,
  products: List<Product>,
  stockMovements: List<StockMovement>,
  onBackClick: () -> Unit,
  onCustomerClick: (String) -> Unit,
  onProductClick: (String) -> Unit,
  onInvoiceClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Sales, 1: Khata, 2: Stock, 3: Customers
  var selectedPeriod by remember { mutableStateOf(DatePeriodFilter.THIS_MONTH) }

  val tabTitles = listOf("Sales", "Khata", "Stock", "Customers")

  // Calculated Real Report Datasets
  val salesReport = remember(invoices, selectedPeriod) {
    DashboardCalculator.calculateSalesReport(invoices, selectedPeriod)
  }

  val khataReport = remember(customers, transactions, selectedPeriod) {
    DashboardCalculator.calculateKhataReport(customers, transactions, selectedPeriod)
  }

  val stockReport = remember(products, stockMovements, selectedPeriod) {
    DashboardCalculator.calculateProductStockReport(products, stockMovements, selectedPeriod)
  }

  val customerReport = remember(customers, invoices) {
    DashboardCalculator.calculateCustomerReport(customers, invoices)
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("reports_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Business Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("btn_back_reports")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          // Export PDF Report Button
          IconButton(
            onClick = {
              try {
                val pdfFile = DataExportHelper.exportBusinessReportPdf(
                  context = context,
                  businessProfile = businessProfile,
                  periodLabel = selectedPeriod.displayName,
                  salesReport = salesReport,
                  khataReport = khataReport,
                  stockReport = stockReport,
                  customerReport = customerReport
                )
                DataExportHelper.shareExportFile(
                  context = context,
                  file = pdfFile,
                  mimeType = "application/pdf",
                  chooserTitle = "Share Business Report PDF"
                )
              } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("btn_export_pdf_report")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Report", tint = KhataGreenPrimary)
          }

          // Export CSV for current section
          IconButton(
            onClick = {
              try {
                val csvFile = when (selectedTab) {
                  0 -> DataExportHelper.exportInvoicesCsv(context, invoices)
                  1 -> DataExportHelper.exportTransactionsCsv(context, transactions)
                  2 -> DataExportHelper.exportProductsCsv(context, products)
                  3 -> DataExportHelper.exportCustomersCsv(context, customers)
                  else -> DataExportHelper.exportBusinessSummaryCsv(
                    context, businessProfile, selectedPeriod.displayName,
                    salesReport, khataReport, stockReport, customerReport
                  )
                }
                DataExportHelper.shareExportFile(
                  context = context,
                  file = csvFile,
                  mimeType = "text/csv",
                  chooserTitle = "Share ${tabTitles[selectedTab]} CSV"
                )
              } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("btn_export_csv_report")
          ) {
            Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Primary Tab Row
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = KhataGreenPrimary,
        modifier = Modifier.testTag("reports_tab_row")
      ) {
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
              )
            },
            modifier = Modifier.testTag("tab_report_$title")
          )
        }
      }

      // Period Filter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          DatePeriodFilter.TODAY,
          DatePeriodFilter.THIS_WEEK,
          DatePeriodFilter.THIS_MONTH,
          DatePeriodFilter.ALL_TIME
        ).forEach { period ->
          FilterChip(
            selected = selectedPeriod == period,
            onClick = { selectedPeriod = period },
            label = { Text(period.displayName) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = KhataGreenContainer,
              selectedLabelColor = KhataGreenPrimary
            ),
            modifier = Modifier.testTag("chip_report_period_${period.name}")
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Content for the active report tab
      Box(modifier = Modifier.weight(1f)) {
        when (selectedTab) {
          0 -> SalesReportView(
            salesReport = salesReport,
            indianFormat = indianFormat,
            onInvoiceClick = onInvoiceClick
          )
          1 -> KhataReportView(
            khataReport = khataReport,
            indianFormat = indianFormat,
            onCustomerClick = onCustomerClick
          )
          2 -> StockReportView(
            stockReport = stockReport,
            indianFormat = indianFormat,
            onProductClick = onProductClick
          )
          3 -> CustomerReportView(
            customerReport = customerReport,
            indianFormat = indianFormat,
            onCustomerClick = onCustomerClick
          )
        }
      }
    }
  }
}

@Composable
private fun SalesReportView(
  salesReport: com.example.model.SalesReportData,
  indianFormat: NumberFormat,
  onInvoiceClick: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("sales_report_content"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Summary Cards
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_sales_report_summary")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Sales Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Total Sales",
              value = "₹${indianFormat.format(salesReport.totalSales.toInt())}",
              subtitle = "${salesReport.invoiceCount} invoices",
              color = KhataGreenPrimary,
              modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
              title = "Paid Amount",
              value = "₹${indianFormat.format(salesReport.paidAmount.toInt())}",
              subtitle = "${salesReport.fullyPaidCount} fully paid",
              color = LenaHaiGreen,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Pending Due",
              value = "₹${indianFormat.format(salesReport.pendingAmount.toInt())}",
              subtitle = "${salesReport.unpaidCount} unpaid, ${salesReport.partiallyPaidCount} partial",
              color = DenaHaiRed,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Date-wise breakdown header
    item {
      Text(
        text = "Date-wise Sales Breakdown",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    if (salesReport.dateWiseSales.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = "No sales in this period",
          description = "Invoices generated in this period will show up here.",
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
          testTagPrefix = "sales_empty"
        )
      }
    } else {
      items(salesReport.dateWiseSales) { dayPoint ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = dayPoint.dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${dayPoint.invoiceCount} invoice(s)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "₹${indianFormat.format(dayPoint.totalSales.toInt())}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KhataGreenPrimary
              )
              Text(
                text = "Paid: ₹${indianFormat.format(dayPoint.paidAmount.toInt())} | Pending: ₹${indianFormat.format(dayPoint.pendingAmount.toInt())}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KhataReportView(
  khataReport: com.example.model.KhataReportData,
  indianFormat: NumberFormat,
  onCustomerClick: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("khata_report_content"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top Summary
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_khata_report_summary")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Khata Ledger Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Total Credit (Udhaar)",
              value = "₹${indianFormat.format(khataReport.totalCredit.toInt())}",
              subtitle = "Total given",
              color = DenaHaiRed,
              modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
              title = "Total Payment (Jama)",
              value = "₹${indianFormat.format(khataReport.totalPayments.toInt())}",
              subtitle = "Total received",
              color = LenaHaiGreen,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            color = DenaHaiBgLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Total Outstanding (Lena Hai)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DenaHaiRed)
              Text("₹${indianFormat.format(khataReport.totalOutstanding.toInt())}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DenaHaiRed)
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Customer-wise Outstanding Breakdown (${khataReport.customerOutstandingList.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    if (khataReport.customerOutstandingList.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.CheckCircle,
          title = "All Hisaab Clear",
          description = "No customers currently have outstanding balances.",
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
          testTagPrefix = "khata_report_empty"
        )
      }
    } else {
      items(khataReport.customerOutstandingList, key = { it.customer.id }) { item ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onCustomerClick(item.customer.id) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = item.customer.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = item.customer.phone,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Udhaar: ₹${indianFormat.format(item.totalCredit.toInt())} • Jama: ₹${indianFormat.format(item.totalPayment.toInt())}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.outline
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "₹${indianFormat.format(item.outstandingAmount.toInt())}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DenaHaiRed
              )
              Text(
                text = "Pending",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DenaHaiRed
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StockReportView(
  stockReport: com.example.model.ProductStockReportData,
  indianFormat: NumberFormat,
  onProductClick: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("stock_report_content"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Inventory Valuation & Health
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_stock_report_summary")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Inventory Valuation & Units", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Physical Units",
              value = "${stockReport.totalStockUnits.toInt()}",
              subtitle = "Across ${stockReport.totalProducts} items",
              color = KhataGreenPrimary,
              modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
              title = "Stock Cost Value",
              value = "₹${indianFormat.format(stockReport.totalStockValuation.toInt())}",
              subtitle = "Purchase price sum",
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Retail Valuation",
              value = "₹${indianFormat.format(stockReport.totalRetailValuation.toInt())}",
              subtitle = "Selling price sum",
              color = LenaHaiGreen,
              modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
              title = "Low / Out of Stock",
              value = "${stockReport.lowStockProducts.size} / ${stockReport.outOfStockProducts.size}",
              subtitle = "Action required",
              color = if (stockReport.lowStockProducts.isNotEmpty() || stockReport.outOfStockProducts.isNotEmpty()) DenaHaiRed else LenaHaiGreen,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Low Stock Alert Items
    if (stockReport.lowStockProducts.isNotEmpty()) {
      item {
        Text(
          text = "Low Stock Alerts (${stockReport.lowStockProducts.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = DenaHaiRed,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      items(stockReport.lowStockProducts, key = { it.id }) { product ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick(product.id) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text("Category: ${product.category.ifBlank { "General" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("${product.currentStock} ${product.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DenaHaiRed)
              Text("Min limit: ${product.lowStockThreshold}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    // Out of Stock Alert Items
    if (stockReport.outOfStockProducts.isNotEmpty()) {
      item {
        Text(
          text = "Out of Stock Items (${stockReport.outOfStockProducts.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = DenaHaiRed,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      items(stockReport.outOfStockProducts, key = { it.id }) { product ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick(product.id) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text("Category: ${product.category.ifBlank { "General" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = DenaHaiBgLight, shape = RoundedCornerShape(6.dp)) {
              Text("0 ${product.unit} (Empty)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DenaHaiRed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }
        }
      }
    }

    // Stock Movement Summary
    item {
      Text(
        text = "Stock Movement Activity (${stockReport.totalMovementsCount} logged)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          StockMovementType.values().forEach { type ->
            val count = stockReport.movementTypeSummary[type] ?: 0
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(type.displayName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
              Text("$count logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KhataGreenPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerReportView(
  customerReport: com.example.model.CustomerReportData,
  indianFormat: NumberFormat,
  onCustomerClick: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("customer_report_content"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_customer_report_summary")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Customer Directory Analytics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ReportMetricCard(
              title = "Total Customers",
              value = "${customerReport.totalCustomers}",
              subtitle = "Registered in Khata",
              color = KhataGreenPrimary,
              modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
              title = "With Pending Dues",
              value = "${customerReport.customersWithBalanceCount}",
              subtitle = "Outstanding accounts",
              color = DenaHaiRed,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Top Customers by Purchase Volume",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    if (customerReport.topCustomersByInvoice.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Default.People,
          title = "No invoice purchase history",
          description = "Customers with invoice transactions will appear ranked here.",
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
          testTagPrefix = "customer_report_empty"
        )
      }
    } else {
      items(customerReport.topCustomersByInvoice, key = { it.customerId }) { topCustomer ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onCustomerClick(topCustomer.customerId) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = topCustomer.customerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${topCustomer.customerPhone} • ${topCustomer.invoiceCount} invoice(s)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "₹${indianFormat.format(topCustomer.totalInvoiceAmount.toInt())}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KhataGreenPrimary
              )
              Text(
                text = "Paid: ₹${indianFormat.format(topCustomer.paidAmount.toInt())}",
                fontSize = 11.sp,
                color = LenaHaiGreen
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReportMetricCard(
  title: String,
  value: String,
  subtitle: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.outline)
    }
  }
}
