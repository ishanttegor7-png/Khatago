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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
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
import com.example.model.Customer
import com.example.model.DatePeriodFilter
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.DashboardCalculator
import java.text.NumberFormat
import java.util.Locale

@Composable
fun KhataScreen(
  transactions: List<Transaction>,
  customers: List<Customer>,
  totalLenaHai: Double,
  totalDenaHai: Double,
  onTransactionClick: (customerId: String) -> Unit,
  onAddTransaction: (customerId: String, customerName: String, type: TransactionType, amount: Double, note: String, onError: (String) -> Unit) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "CREDIT", "PAYMENT"
  var selectedDateFilter by remember { mutableStateOf("All") } // "All", "Today", "This Week", "This Month"
  var showAddDialog by remember { mutableStateOf(false) }

  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  val settledCustomersCount = remember(customers) { customers.count { it.isAllClear && it.transactionCount > 0 } }

  val filteredTransactions = remember(transactions, searchQuery, selectedTypeFilter, selectedDateFilter) {
    val dateRange = when (selectedDateFilter) {
      "Today" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.TODAY)
      "This Week" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.THIS_WEEK)
      "This Month" -> DashboardCalculator.getPeriodDateRange(DatePeriodFilter.THIS_MONTH)
      else -> null
    }

    transactions.filter { tx ->
      val matchesSearch = searchQuery.isBlank() || tx.customerName.contains(searchQuery, ignoreCase = true)
      val matchesType = when (selectedTypeFilter) {
        "CREDIT" -> tx.type == TransactionType.CREDIT
        "PAYMENT" -> tx.type == TransactionType.PAYMENT
        else -> true
      }
      val matchesDate = if (dateRange != null) {
        tx.timestamp in dateRange.startMillis..dateRange.endMillis
      } else {
        true
      }
      matchesSearch && matchesType && matchesDate
    }
  }

  if (showAddDialog) {
    AddTransactionDialog(
      initialType = TransactionType.CREDIT,
      customerList = customers,
      onDismiss = { showAddDialog = false },
      onConfirm = { cId, cName, type, amt, note, onError ->
        onAddTransaction(cId, cName, type, amt, note, onError)
        showAddDialog = false
      }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("khata_screen"),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("+ Add Entry", fontWeight = FontWeight.Bold) },
        containerColor = KhataGreenPrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_khata_entry")
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Header and Total Cards
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "Khata Ledger",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "All credit & payment ledger entries in one place",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Financial Overview Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Lena Hai Card
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LenaHaiBgLight),
            modifier = Modifier.weight(1f).testTag("khata_card_lena_hai")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Total Lena Hai",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LenaHaiGreen
              )
              Text(
                text = "₹${indianFormat.format(totalLenaHai.toInt())}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LenaHaiGreen
              )
            }
          }

          // Dena Hai Card
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DenaHaiBgLight),
            modifier = Modifier.weight(1f).testTag("khata_card_dena_hai")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Total Dena Hai",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DenaHaiRed
              )
              Text(
                text = "₹${indianFormat.format(totalDenaHai.toInt())}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DenaHaiRed
              )
            }
          }

          // Settled Customers Count Card
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.weight(1f).testTag("khata_card_settled")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Settled",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$settledCustomersCount Accounts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar for Customer Name
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search customer name...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = KhataGreenPrimary, modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("khata_search_input"),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KhataGreenPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Options: Transaction Type & Date Range
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("All", "CREDIT", "PAYMENT").forEach { typeFilter ->
            val isSelected = selectedTypeFilter == typeFilter
            FilterChip(
              selected = isSelected,
              onClick = { selectedTypeFilter = typeFilter },
              label = {
                Text(
                  when (typeFilter) {
                    "CREDIT" -> "Udhaar (Credit)"
                    "PAYMENT" -> "Jama (Payment)"
                    else -> "All Types"
                  },
                  fontSize = 11.5.sp
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = when (typeFilter) {
                  "CREDIT" -> DenaHaiBgLight
                  "PAYMENT" -> LenaHaiBgLight
                  else -> MaterialTheme.colorScheme.primaryContainer
                },
                selectedLabelColor = when (typeFilter) {
                  "CREDIT" -> DenaHaiRed
                  "PAYMENT" -> LenaHaiGreen
                  else -> KhataGreenPrimary
                }
              ),
              modifier = Modifier.testTag("khata_filter_type_$typeFilter")
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          listOf("All", "Today", "This Week", "This Month").forEach { dateFilter ->
            val isSelected = selectedDateFilter == dateFilter
            FilterChip(
              selected = isSelected,
              onClick = { selectedDateFilter = dateFilter },
              label = {
                Text(
                  if (dateFilter == "All") "All Time" else dateFilter,
                  fontSize = 11.5.sp
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              modifier = Modifier.testTag("khata_filter_date_$dateFilter")
            )
          }
        }
      }

      // Transactions List or Empty State
      if (filteredTransactions.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.ReceiptLong,
          title = "No transactions yet",
          description = if (transactions.isEmpty()) "No entries in the ledger yet. Add your first customer and transaction." else "No entries found matching filters.",
          actionButtonText = if (customers.isNotEmpty()) "+ Add First Entry" else null,
          onActionClick = if (customers.isNotEmpty()) { { showAddDialog = true } } else null,
          testTagPrefix = "khata_empty_state"
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("khata_transactions_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredTransactions, key = { it.id }) { tx ->
            val isCredit = tx.type == TransactionType.CREDIT
            val isPayment = tx.type == TransactionType.PAYMENT
            val amountColor = if (isCredit) DenaHaiRed else LenaHaiGreen

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onTransactionClick(tx.customerId) }
                .testTag("khata_tx_${tx.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isPayment) LenaHaiBgLight else DenaHaiBgLight),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isPayment) Icons.Default.CheckCircle else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isPayment) LenaHaiGreen else DenaHaiRed,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = tx.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = tx.note.ifBlank { if (isCredit) "Udhaar Diya" else "Payment Mila" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = tx.date,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${if (isPayment) "+" else "-"}₹${indianFormat.format(tx.amount.toInt())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPayment) LenaHaiBgLight else DenaHaiBgLight
                  ) {
                    Text(
                      text = if (isPayment) "Payment (Mila)" else "Udhaar Diya",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = amountColor,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }
          item {
            Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
          }
        }
      }
    }
  }
}
