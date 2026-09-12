package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.Customer
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SendReminderDialog
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.DashboardCalculator
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRemindersScreen(
  customers: List<Customer>,
  businessName: String,
  onBackClick: () -> Unit,
  onCustomerClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCustomerForReminder by remember { mutableStateOf<Customer?>(null) }

  val outstandingCustomers = remember(customers, searchQuery) {
    customers
      .filter { it.balance > 0 }
      .filter { cust ->
        if (searchQuery.isBlank()) true
        else cust.name.contains(searchQuery, ignoreCase = true) || cust.phone.contains(searchQuery)
      }
      .sortedByDescending { it.balance }
  }

  val totalOutstanding = remember(customers) {
    customers.filter { it.balance > 0 }.sumOf { it.balance }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("payment_reminders_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Payment Reminders",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${outstandingCustomers.size} customers with pending balance",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("btn_back_reminders")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
      // Summary Card
      Surface(
        color = DenaHaiBgLight,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("card_reminders_summary")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Total Pending Collection",
              fontSize = 12.sp,
              color = DenaHaiRed,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "₹${indianFormat.format(totalOutstanding.toInt())}",
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              color = DenaHaiRed
            )
            Text(
              text = "${customers.count { it.balance > 0 }} customer(s) to collect from",
              fontSize = 11.sp,
              color = DenaHaiRed.copy(alpha = 0.8f)
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(DenaHaiRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = DenaHaiRed,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by customer name or phone") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("search_reminders_input")
      )

      if (outstandingCustomers.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.CheckCircle,
          title = if (searchQuery.isNotBlank()) "No matching customers" else "All Khata Cleared!",
          description = if (searchQuery.isNotBlank()) "Try a different search query" else "No customers have outstanding balances. Hisaab is all clear!",
          actionButtonText = if (searchQuery.isNotBlank()) "Clear Filter" else null,
          onActionClick = if (searchQuery.isNotBlank()) { { searchQuery = "" } } else null,
          modifier = Modifier.weight(1f),
          testTagPrefix = "reminders_empty"
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("reminders_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(outstandingCustomers, key = { it.id }) { customer ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCustomerClick(customer.id) }
                .testTag("reminder_customer_card_${customer.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  // Customer Avatar
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(KhataGreenContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = customer.name.take(1).uppercase(),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = KhataGreenPrimary
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Text(
                      text = customer.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = customer.phone,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "Pending: ₹${indianFormat.format(customer.balance.toInt())}",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = DenaHaiRed
                    )
                  }
                }

                // Send Reminder Action Button
                Button(
                  onClick = {
                    selectedCustomerForReminder = customer
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("btn_remind_${customer.id}")
                ) {
                  Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Remind", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Reminder Preview & Share Dialog
  selectedCustomerForReminder?.let { cust ->
    SendReminderDialog(
      customer = cust,
      businessName = businessName,
      onDismiss = { selectedCustomerForReminder = null },
      onSendConfirmation = { _ ->
        selectedCustomerForReminder = null
      }
    )
  }
}
