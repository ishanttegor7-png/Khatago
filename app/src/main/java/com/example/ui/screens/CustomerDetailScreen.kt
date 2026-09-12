package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SendReminderDialog
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomerDetailScreen(
  customer: Customer,
  transactions: List<Transaction>,
  businessName: String,
  onBack: () -> Unit,
  onAddTransaction: (customerId: String, customerName: String, type: TransactionType, amount: Double, note: String, onError: (String) -> Unit) -> Unit,
  onEditCustomer: (customerId: String, name: String, phone: String, address: String) -> Unit,
  onDeleteCustomer: (customerId: String) -> Unit,
  onSendReminderConfirmation: (method: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var dialogTransactionType by remember { mutableStateOf(TransactionType.CREDIT) }
  var showReminderDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

  // Add Udhaar / Add Payment Dialog
  if (showAddDialog) {
    AddTransactionDialog(
      initialType = dialogTransactionType,
      customerList = listOf(customer),
      preselectedCustomer = customer,
      onDismiss = { showAddDialog = false },
      onConfirm = { cId, cName, type, amt, note, onError ->
        onAddTransaction(cId, cName, type, amt, note, onError)
        showAddDialog = false
      }
    )
  }

  // Send Reminder Dialog
  if (showReminderDialog) {
    SendReminderDialog(
      customer = customer,
      businessName = businessName,
      onDismiss = { showReminderDialog = false },
      onSendConfirmation = { method ->
        onSendReminderConfirmation(method)
      }
    )
  }

  // Edit Customer Dialog
  if (showEditDialog) {
    var editName by remember { mutableStateOf(customer.name) }
    var editPhone by remember { mutableStateOf(customer.phone) }
    var editAddress by remember { mutableStateOf(customer.address) }
    var editError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      modifier = Modifier.testTag("dialog_edit_customer"),
      title = {
        Text("Edit Customer Details", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Customer Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_edit_customer_name")
          )
          OutlinedTextField(
            value = editPhone,
            onValueChange = { editPhone = it },
            label = { Text("Mobile Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_edit_customer_phone")
          )
          OutlinedTextField(
            value = editAddress,
            onValueChange = { editAddress = it },
            label = { Text("Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_edit_customer_address")
          )
          if (editError != null) {
            Text(editError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editName.trim().isBlank()) {
              editError = "Name cannot be empty"
              return@Button
            }
            onEditCustomer(customer.id, editName.trim(), editPhone.trim(), editAddress.trim())
            showEditDialog = false
          },
          modifier = Modifier.testTag("btn_confirm_edit_customer")
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showEditDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete Customer Safety Confirmation Dialog
  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      modifier = Modifier.testTag("dialog_delete_customer_confirmation"),
      icon = {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = DenaHaiRed,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text("Delete Customer?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "Are you sure you want to delete ${customer.name}? " +
            "This will remove the customer profile and all ${transactions.size} associated transaction history records. " +
            "This action cannot be undone.",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmation = false
            onDeleteCustomer(customer.id)
            onBack()
          },
          colors = ButtonDefaults.buttonColors(containerColor = DenaHaiRed),
          modifier = Modifier.testTag("btn_confirm_delete_customer")
        ) {
          Text("Delete Customer")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showDeleteConfirmation = false },
          modifier = Modifier.testTag("btn_cancel_delete_customer")
        ) {
          Text("Cancel")
        }
      }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("customer_detail_screen"),
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_back_customer_detail")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = customer.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = { showEditDialog = true },
          modifier = Modifier.testTag("btn_edit_customer")
        ) {
          Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
        }
        IconButton(
          onClick = { showDeleteConfirmation = true },
          modifier = Modifier.testTag("btn_delete_customer")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = DenaHaiRed)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Customer Header Profile Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth().testTag("card_customer_profile")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(Color(customer.avatarColorHex)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = customer.name.take(1).uppercase(),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 24.sp
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = customer.name,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
                if (customer.phone.isNotBlank()) {
                  Text(
                    text = "+91 ${customer.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (customer.address.isNotBlank()) {
                  Text(
                    text = customer.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact and Reminder Options
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = { /* Simulated Call */ },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_call_customer")
              ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call", fontSize = 13.sp)
              }
              OutlinedButton(
                onClick = { showReminderDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.3f).height(44.dp).testTag("btn_whatsapp_reminder")
              ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontSize = 13.sp, color = KhataGreenPrimary)
              }
            }
          }
        }
      }

      // Balance Summary Display
      item {
        val balanceColor = when {
          customer.isLenaHai -> LenaHaiGreen
          customer.isDenaHai -> DenaHaiRed
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val balanceBg = when {
          customer.isLenaHai -> LenaHaiBgLight
          customer.isDenaHai -> DenaHaiBgLight
          else -> MaterialTheme.colorScheme.surfaceVariant
        }

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = balanceBg),
          modifier = Modifier.fillMaxWidth().testTag("card_customer_balance")
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (customer.isLenaHai) "AAPKO LENA HAI" else if (customer.isDenaHai) "AAPKO DENA HAI" else "HISAAB STATUS",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = balanceColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            val formattedAmt = when {
              customer.isLenaHai -> "₹${indianFormat.format(customer.balance.toInt())}"
              customer.isDenaHai -> "₹${indianFormat.format((-customer.balance).toInt())}"
              else -> "₹0"
            }
            Text(
              text = formattedAmt,
              fontSize = 32.sp,
              fontWeight = FontWeight.ExtraBold,
              color = balanceColor,
              modifier = Modifier.testTag("text_customer_balance_amount")
            )
            Text(
              text = if (customer.isLenaHai) "Pending payment from customer" else if (customer.isDenaHai) "Advance payment from customer" else "All previous transactions settled",
              fontSize = 12.sp,
              color = balanceColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown of Total Credit vs Total Payment
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Udhaar Diya", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  "₹${indianFormat.format(customer.totalCredit.toInt())}",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = DenaHaiRed
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Payment Mila", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  "₹${indianFormat.format(customer.totalPayment.toInt())}",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = LenaHaiGreen
                )
              }
            }
          }
        }
      }

      // Action Buttons: Add Udhaar, Add Payment, Send Reminder
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              dialogTransactionType = TransactionType.CREDIT
              showAddDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DenaHaiRed),
            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_detail_add_udhaar")
          ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Udhaar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              dialogTransactionType = TransactionType.PAYMENT
              showAddDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LenaHaiGreen),
            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_detail_add_payment")
          ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = { showReminderDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_detail_send_reminder")
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = KhataAmber, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reminder", fontSize = 12.sp, color = KhataAmber, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Transaction History Title
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Transaction History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${transactions.size} Entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (transactions.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.ReceiptLong,
            title = "No transactions yet",
            description = "Start recording credit (udhaar) or payments for this customer.",
            actionButtonText = "+ Add First Udhaar",
            onActionClick = {
              dialogTransactionType = TransactionType.CREDIT
              showAddDialog = true
            },
            testTagPrefix = "detail_empty_transactions"
          )
        }
      } else {
        items(transactions, key = { it.id }) { tx ->
          val isCredit = tx.type == TransactionType.CREDIT
          val isPayment = tx.type == TransactionType.PAYMENT

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().testTag("detail_tx_${tx.id}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(if (isCredit) DenaHaiBgLight else LenaHaiBgLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isCredit) Icons.Default.ArrowUpward else Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = if (isCredit) DenaHaiRed else LenaHaiGreen,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = tx.note.ifBlank { if (isCredit) "Udhaar Diya" else "Payment Mila" },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = tx.date,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.runningBalance != 0.0) {
                  Text(
                    text = "Bal: ₹${indianFormat.format(tx.runningBalance.toInt())}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${if (isPayment) "+" else "-"}₹${indianFormat.format(tx.amount.toInt())}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isCredit) DenaHaiRed else LenaHaiGreen
                )
                Text(
                  text = if (isCredit) "Udhaar" else "Payment",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (isCredit) DenaHaiRed else LenaHaiGreen
                )
              }
            }
          }
        }
      }
    }
  }
}
