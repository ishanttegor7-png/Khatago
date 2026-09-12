package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.model.DatePeriodFilter
import com.example.model.Invoice
import com.example.model.Product
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.DashboardCalculator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
  invoices: List<Invoice>,
  customers: List<Customer>,
  products: List<Product>,
  onBackClick: () -> Unit,
  onNavigateToReports: () -> Unit,
  onNavigateToReminders: () -> Unit,
  onNavigateToInvoices: () -> Unit,
  onNavigateToProducts: () -> Unit,
  onNavigateToKhata: () -> Unit,
  onNavigateToCustomers: () -> Unit,
  onExportClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedPeriod by remember { mutableStateOf(DatePeriodFilter.THIS_MONTH) }
  var customStartMillis by remember { mutableStateOf<Long?>(null) }
  var customEndMillis by remember { mutableStateOf<Long?>(null) }
  var showCustomDatePicker by remember { mutableStateOf(false) }

  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  val dashboardData = remember(invoices, customers, products, selectedPeriod, customStartMillis, customEndMillis) {
    DashboardCalculator.calculateDashboardData(
      invoices = invoices,
      customers = customers,
      products = products,
      period = selectedPeriod,
      customStartMillis = customStartMillis,
      customEndMillis = customEndMillis
    )
  }

  val datePickerState = rememberDatePickerState()

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("sales_dashboard_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Sales & Business Dashboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("btn_back_sales_dashboard")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = onExportClick,
            modifier = Modifier.testTag("btn_dashboard_export")
          ) {
            Icon(Icons.Default.Download, contentDescription = "Export Data")
          }
          IconButton(
            onClick = onNavigateToReports,
            modifier = Modifier.testTag("btn_dashboard_reports")
          ) {
            Icon(Icons.Default.Assessment, contentDescription = "Reports")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .testTag("sales_dashboard_content"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Period Filter Chips
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            DatePeriodFilter.TODAY,
            DatePeriodFilter.THIS_WEEK,
            DatePeriodFilter.THIS_MONTH,
            DatePeriodFilter.ALL_TIME,
            DatePeriodFilter.CUSTOM_RANGE
          ).forEach { filter ->
            FilterChip(
              selected = selectedPeriod == filter,
              onClick = {
                if (filter == DatePeriodFilter.CUSTOM_RANGE) {
                  showCustomDatePicker = true
                }
                selectedPeriod = filter
              },
              label = {
                Text(
                  text = filter.displayName,
                  fontWeight = if (selectedPeriod == filter) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = KhataGreenContainer,
                selectedLabelColor = KhataGreenPrimary
              ),
              modifier = Modifier.testTag("chip_period_${filter.name}")
            )
          }
        }

        if (selectedPeriod == DatePeriodFilter.CUSTOM_RANGE && customStartMillis != null && customEndMillis != null) {
          Text(
            text = "Showing data from ${dateFormat.format(Date(customStartMillis!!))} to ${dateFormat.format(Date(customEndMillis!!))}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
          )
        }
      }

      // Sales Time-Period Comparison Cards (Today, Week, Month)
      item {
        Text(
          text = "Sales Trends (Real Data)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TrendCard(
            label = "Today's Sales",
            amount = dashboardData.todaySales,
            containerColor = KhataGreenContainer,
            contentColor = KhataGreenPrimary,
            modifier = Modifier.weight(1f).testTag("card_trend_today")
          )
          TrendCard(
            label = "This Week",
            amount = dashboardData.thisWeekSales,
            containerColor = KhataAmberBg,
            contentColor = KhataAmber,
            modifier = Modifier.weight(1f).testTag("card_trend_week")
          )
          TrendCard(
            label = "This Month",
            amount = dashboardData.thisMonthSales,
            containerColor = LenaHaiBgLight,
            contentColor = LenaHaiGreen,
            modifier = Modifier.weight(1f).testTag("card_trend_month")
          )
        }
      }

      // Filtered Period Invoices Breakdown (Total Invoiced, Paid, Pending)
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("card_period_financials")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${selectedPeriod.displayName} Invoice Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${dashboardData.periodInvoiceCount} Invoices",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KhataGreenPrimary
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              MetricItem(
                label = "Total Invoiced",
                value = "₹${indianFormat.format(dashboardData.periodTotalInvoiceAmount.toInt())}",
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              MetricItem(
                label = "Paid / Collected",
                value = "₹${indianFormat.format(dashboardData.periodPaidAmount.toInt())}",
                textColor = LenaHaiGreen,
                modifier = Modifier.weight(1f)
              )
              MetricItem(
                label = "Pending Due",
                value = "₹${indianFormat.format(dashboardData.periodPendingAmount.toInt())}",
                textColor = DenaHaiRed,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // Khata Outstanding vs Dena Hai Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToKhata() }
            .testTag("card_dashboard_khata")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KhataGreenContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = KhataGreenPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Khata Ledger Balance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Outstanding customer hisaab",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                color = LenaHaiBgLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Total Lena Hai", fontSize = 11.sp, color = LenaHaiGreen, fontWeight = FontWeight.SemiBold)
                  Text(
                    "₹${indianFormat.format(dashboardData.totalOutstandingKhata.toInt())}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LenaHaiGreen
                  )
                }
              }

              Surface(
                color = DenaHaiBgLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Total Dena Hai", fontSize = 11.sp, color = DenaHaiRed, fontWeight = FontWeight.SemiBold)
                  Text(
                    "₹${indianFormat.format(dashboardData.totalDenaHaiKhata.toInt())}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DenaHaiRed
                  )
                }
              }
            }
          }
        }
      }

      // Products & Inventory Status Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToProducts() }
            .testTag("card_dashboard_inventory")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KhataAmberBg),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = KhataAmber,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Inventory & Stock Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${dashboardData.totalProducts} Total Products",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              StockBadge(
                label = "Total Items",
                count = "${dashboardData.totalProducts}",
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              StockBadge(
                label = "Low Stock",
                count = "${dashboardData.lowStockCount}",
                bgColor = if (dashboardData.lowStockCount > 0) DenaHaiBgLight else MaterialTheme.colorScheme.surfaceVariant,
                textColor = if (dashboardData.lowStockCount > 0) DenaHaiRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              StockBadge(
                label = "Out of Stock",
                count = "${dashboardData.outOfStockCount}",
                bgColor = if (dashboardData.outOfStockCount > 0) DenaHaiBgLight else MaterialTheme.colorScheme.surfaceVariant,
                textColor = if (dashboardData.outOfStockCount > 0) DenaHaiRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // Quick Hub Shortcuts: Reports, Reminders, Invoices, Export
      item {
        Text(
          text = "Business Hub & Actions",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ActionHubCard(
            title = "Business Reports",
            subtitle = "Detailed PDF & CSV reports",
            icon = Icons.Default.Assessment,
            iconTint = KhataGreenPrimary,
            onClick = onNavigateToReports,
            modifier = Modifier.weight(1f).testTag("card_hub_reports")
          )

          ActionHubCard(
            title = "Payment Reminders",
            subtitle = "Collect pending dues",
            icon = Icons.Default.NotificationsActive,
            iconTint = DenaHaiRed,
            onClick = onNavigateToReminders,
            modifier = Modifier.weight(1f).testTag("card_hub_reminders")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ActionHubCard(
            title = "All Invoices",
            subtitle = "Create & track bills",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = KhataAmber,
            onClick = onNavigateToInvoices,
            modifier = Modifier.weight(1f).testTag("card_hub_invoices")
          )

          ActionHubCard(
            title = "Export Data",
            subtitle = "CSV & PDF offline backup",
            icon = Icons.Default.Download,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = onExportClick,
            modifier = Modifier.weight(1f).testTag("card_hub_export")
          )
        }
      }
    }
  }

  // Custom Date Picker Dialog
  if (showCustomDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showCustomDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { selectedMillis ->
              customStartMillis = selectedMillis
              customEndMillis = selectedMillis + (24 * 60 * 60 * 1000L - 1)
            }
            showCustomDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomDatePicker = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
private fun TrendCard(
  label: String,
  amount: Double,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = contentColor
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "₹${indianFormat.format(amount.toInt())}",
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = contentColor
      )
    }
  }
}

@Composable
private fun MetricItem(
  label: String,
  value: String,
  textColor: Color,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = textColor
    )
  }
}

@Composable
private fun StockBadge(
  label: String,
  count: String,
  bgColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = bgColor,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
      )
      Text(
        text = label,
        fontSize = 10.sp,
        color = textColor.copy(alpha = 0.85f)
      )
    }
  }
}

@Composable
private fun ActionHubCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = subtitle,
          fontSize = 10.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
      }
    }
  }
}
