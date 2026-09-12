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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
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
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomersScreen(
  customers: List<Customer>,
  onCustomerClick: (String) -> Unit,
  onAddCustomerClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") } // "All", "Outstanding", "Dena Hai", "Settled"
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  val filteredCustomers = remember(customers, searchQuery, selectedFilter) {
    customers.filter { customer ->
      val matchesSearch = searchQuery.isBlank() ||
        customer.name.contains(searchQuery, ignoreCase = true) ||
        customer.phone.contains(searchQuery)
      val matchesFilter = when (selectedFilter) {
        "Outstanding" -> customer.isLenaHai
        "Dena Hai" -> customer.isDenaHai
        "Settled" -> customer.isAllClear
        else -> true
      }
      matchesSearch && matchesFilter
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("customers_screen"),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddCustomerClick,
        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
        text = { Text("+ Add Customer", fontWeight = FontWeight.Bold) },
        containerColor = KhataGreenPrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_customer")
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Screen Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "Customer Hisaab",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (customers.isEmpty()) "No customers added yet" else "${customers.size} total registered customers",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Box (by name or mobile number)
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by name or mobile...", fontSize = 14.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = KhataGreenPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search")
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
            .testTag("customer_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("All", "Outstanding", "Dena Hai", "Settled").forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
              selected = isSelected,
              onClick = { selectedFilter = filter },
              label = {
                Text(
                  if (filter == "Outstanding") "Outstanding (Lena Hai)" else filter,
                  fontSize = 12.sp
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = when (filter) {
                  "Outstanding" -> LenaHaiBgLight
                  "Dena Hai" -> DenaHaiBgLight
                  else -> MaterialTheme.colorScheme.primaryContainer
                },
                selectedLabelColor = when (filter) {
                  "Outstanding" -> LenaHaiGreen
                  "Dena Hai" -> DenaHaiRed
                  else -> KhataGreenPrimary
                }
              ),
              modifier = Modifier.testTag("filter_chip_$filter")
            )
          }
        }
      }

      // Customer List or Empty State
      if (filteredCustomers.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.PersonOff,
          title = if (searchQuery.isNotBlank()) "No matching customers" else "No customers yet",
          description = if (searchQuery.isNotBlank()) "Try searching with a different name or number." else "Add your first customer to start keeping their hisaab.",
          actionButtonText = if (searchQuery.isBlank()) "+ Add First Customer" else null,
          onActionClick = if (searchQuery.isBlank()) onAddCustomerClick else null,
          testTagPrefix = "customers_empty_state"
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("customers_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredCustomers, key = { it.id }) { customer ->
            CustomerCard(
              customer = customer,
              indianFormat = indianFormat,
              onClick = { onCustomerClick(customer.id) }
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
fun CustomerCard(
  customer: Customer,
  indianFormat: NumberFormat,
  onClick: () -> Unit
) {
  val balanceColor = when {
    customer.isLenaHai -> LenaHaiGreen
    customer.isDenaHai -> DenaHaiRed
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  val statusBg = when {
    customer.isLenaHai -> LenaHaiBgLight
    customer.isDenaHai -> DenaHaiBgLight
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  val statusText = when {
    customer.isLenaHai -> "Lena Hai ₹${indianFormat.format(customer.balance.toInt())}"
    customer.isDenaHai -> "Dena Hai ₹${indianFormat.format((-customer.balance).toInt())}"
    else -> "Settled"
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("customer_card_${customer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(customer.avatarColorHex)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = customer.name.take(1).uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Customer Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = customer.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )
        if (customer.phone.isNotBlank()) {
          Text(
            text = customer.phone,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (customer.address.isNotBlank()) {
          Text(
            text = customer.address,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Status Badge & Amount
      Column(horizontalAlignment = Alignment.End) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusBg
        ) {
          Text(
            text = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = balanceColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = customer.lastUpdated,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "View detail",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
