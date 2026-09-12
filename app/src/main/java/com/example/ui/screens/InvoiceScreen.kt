package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DatePeriodFilter
import com.example.model.Invoice
import com.example.model.InvoicePaymentStatus
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InvoiceScreen(
  invoices: List<Invoice>,
  onCreateInvoiceClick: () -> Unit,
  onInvoiceClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("All") } // "All", "Pending", "Partially Paid", "Paid"
  var dateFilter by remember { mutableStateOf("All") } // "All", "Today", "This Week", "This Month"
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  val filteredInvoices = remember(invoices, searchQuery, statusFilter, dateFilter) {
    val dateRange = when (dateFilter) {
      "Today" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.TODAY)
      "This Week" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.THIS_WEEK)
      "This Month" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.THIS_MONTH)
      else -> null
    }

    invoices.filter { inv ->
      val matchesSearch = searchQuery.isBlank() ||
        inv.customerName.contains(searchQuery, ignoreCase = true) ||
        inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
        inv.customerPhone.contains(searchQuery, ignoreCase = true) ||
        inv.items.any { it.productName.contains(searchQuery, ignoreCase = true) }
      val matchesFilter = when (statusFilter) {
        "Paid" -> inv.paymentStatus == InvoicePaymentStatus.PAID
        "Partially Paid" -> inv.paymentStatus == InvoicePaymentStatus.PARTIALLY_PAID
        "Pending" -> inv.paymentStatus == InvoicePaymentStatus.PENDING
        else -> true
      }
      val matchesDate = if (dateRange != null) {
        inv.createdTimestamp in dateRange.startMillis..dateRange.endMillis
      } else {
        true
      }
      matchesSearch && matchesFilter && matchesDate
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("invoice_screen"),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onCreateInvoiceClick,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("+ Create Invoice", fontWeight = FontWeight.Bold) },
        containerColor = KhataGreenPrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_create_invoice")
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Header Section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "Invoices & Billing",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Professional bills for your customers with PDF & Khata sync",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search invoice #, customer, item...", fontSize = 14.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = KhataGreenPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KhataGreenPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status & Date Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("All", "Pending", "Partially Paid", "Paid").forEach { filter ->
            val isSelected = statusFilter == filter
            FilterChip(
              selected = isSelected,
              onClick = { statusFilter = filter },
              label = { Text(filter, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = when (filter) {
                  "Pending" -> DenaHaiBgLight
                  "Partially Paid" -> KhataAmberBg
                  "Paid" -> LenaHaiBgLight
                  else -> KhataGreenContainer
                },
                selectedLabelColor = when (filter) {
                  "Pending" -> DenaHaiRed
                  "Partially Paid" -> KhataAmber
                  "Paid" -> LenaHaiGreen
                  else -> KhataGreenPrimary
                }
              ),
              modifier = Modifier.testTag("invoice_filter_$filter")
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          listOf("All", "Today", "This Week", "This Month").forEach { dateF ->
            val isSelected = dateFilter == dateF
            FilterChip(
              selected = isSelected,
              onClick = { dateFilter = dateF },
              label = { Text(if (dateF == "All") "All Dates" else dateF, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              modifier = Modifier.testTag("invoice_date_filter_$dateF")
            )
          }
        }
      }

      // Invoice List or Empty State
      if (filteredInvoices.isEmpty()) {
        EmptyStateView(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = if (searchQuery.isNotBlank()) "No invoices found" else "No invoices yet",
          description = if (searchQuery.isNotBlank()) "Try searching for another invoice number or customer name." else "Create your first professional shop invoice in seconds.",
          actionButtonText = if (searchQuery.isBlank()) "+ Create First Invoice" else null,
          onActionClick = if (searchQuery.isBlank()) onCreateInvoiceClick else null,
          testTagPrefix = "invoice_empty_state"
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("invoice_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredInvoices, key = { it.id }) { invoice ->
            InvoiceCard(
              invoice = invoice,
              indianFormat = indianFormat,
              onClick = { onInvoiceClick(invoice.id) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
          }
        }
      }
    }
  }
}

@Composable
fun InvoiceCard(
  invoice: Invoice,
  indianFormat: NumberFormat,
  onClick: () -> Unit
) {
  val statusBg = when (invoice.paymentStatus) {
    InvoicePaymentStatus.PAID -> LenaHaiBgLight
    InvoicePaymentStatus.PARTIALLY_PAID -> KhataAmberBg
    InvoicePaymentStatus.PENDING -> DenaHaiBgLight
    InvoicePaymentStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
  }

  val statusColor = when (invoice.paymentStatus) {
    InvoicePaymentStatus.PAID -> LenaHaiGreen
    InvoicePaymentStatus.PARTIALLY_PAID -> KhataAmber
    InvoicePaymentStatus.PENDING -> DenaHaiRed
    InvoicePaymentStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("invoice_card_${invoice.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
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
              imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = null,
              tint = KhataAmber,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = invoice.invoiceNumber,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = KhataGreenPrimary
            )
            Text(
              text = invoice.invoiceDate,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusBg
        ) {
          Text(
            text = invoice.paymentStatus.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = invoice.customerName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          val itemSummary = when {
            invoice.items.isEmpty() -> "General Invoice"
            invoice.items.size == 1 -> "${invoice.items.first().productName} (Qty: ${invoice.items.first().quantity})"
            else -> "${invoice.items.first().productName} + ${invoice.items.size - 1} more items"
          }
          Text(
            text = itemSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "₹${indianFormat.format(invoice.grandTotal.toInt())}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (invoice.remainingAmount > 0) {
            Text(
              text = "Due: ₹${indianFormat.format(invoice.remainingAmount.toInt())}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = DenaHaiRed
            )
          } else {
            Text(
              text = "Paid in Full",
              fontSize = 11.sp,
              color = LenaHaiGreen,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
