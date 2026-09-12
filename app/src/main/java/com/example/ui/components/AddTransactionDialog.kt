package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.Customer
import com.example.model.TransactionType
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTransactionDialog(
  initialType: TransactionType,
  customerList: List<Customer>,
  preselectedCustomer: Customer? = null,
  onDismiss: () -> Unit,
  onConfirm: (customerId: String, customerName: String, type: TransactionType, amount: Double, note: String, onError: (String) -> Unit) -> Unit
) {
  var selectedType by remember { mutableStateOf(initialType) }
  var amountText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var selectedCustomerId by remember {
    mutableStateOf(preselectedCustomer?.id ?: customerList.firstOrNull()?.id ?: "")
  }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
  val currentDateStr = remember {
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
  }

  val activeCustomer = preselectedCustomer ?: customerList.find { it.id == selectedCustomerId }
  val isCreditGiven = selectedType == TransactionType.CREDIT
  val dialogTitle = if (isCreditGiven) "Add Udhaar (Gave Credit)" else "Add Payment (Received)"

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.testTag("add_transaction_dialog"),
    title = {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = if (isCreditGiven) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = if (isCreditGiven) DenaHaiRed else LenaHaiGreen
          )
          Text(
            text = dialogTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = if (isCreditGiven) "Gave goods/cash on credit (Aapko lena hai)" else "Customer paid hisaab (Aapko mila)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Transaction Type Switcher
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Button(
            onClick = {
              selectedType = TransactionType.CREDIT
              errorMessage = null
            },
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("type_lena_hai_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isCreditGiven) DenaHaiRed else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (isCreditGiven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
          ) {
            Text("Udhaar Diya", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              selectedType = TransactionType.PAYMENT
              errorMessage = null
            },
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("type_payment_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (!isCreditGiven) LenaHaiGreen else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (!isCreditGiven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
          ) {
            Text("Payment Mila", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        // Active Customer Info & Current Balance banner
        if (activeCustomer != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (activeCustomer.balance > 0) LenaHaiBgLight else if (activeCustomer.balance < 0) DenaHaiBgLight else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = activeCustomer.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (activeCustomer.phone.isNotBlank()) {
                  Text(
                    text = activeCustomer.phone,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "Current Balance",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val balColor = if (activeCustomer.balance > 0) LenaHaiGreen else if (activeCustomer.balance < 0) DenaHaiRed else MaterialTheme.colorScheme.onSurfaceVariant
                val balText = if (activeCustomer.balance > 0) {
                  "₹${indianFormat.format(activeCustomer.balance.toInt())} (Lena)"
                } else if (activeCustomer.balance < 0) {
                  "₹${indianFormat.format((-activeCustomer.balance).toInt())} (Dena)"
                } else {
                  "₹0 (Settled)"
                }
                Text(
                  text = balText,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = balColor
                )
              }
            }
          }
        } else if (customerList.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "No customers available. Please add a customer first.",
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontSize = 12.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // Amount Field
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            if (it.all { ch -> ch.isDigit() || ch == '.' }) {
              amountText = it
              errorMessage = null
            }
          },
          label = { Text("Amount (₹) *") },
          placeholder = { Text("e.g. 500") },
          leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = errorMessage != null,
          supportingText = {
            if (errorMessage != null) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("transaction_error_message")
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_amount_input")
        )

        // Note / Item Description
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Item / Note (Optional)") },
          placeholder = { Text(if (isCreditGiven) "e.g. 5kg Atta, Oil, Grocery" else "e.g. Cash settlement, UPI") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_note_input")
        )

        // Date Display
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Date: $currentDateStr",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || amt <= 0.0) {
            errorMessage = "Please enter a valid amount greater than 0"
            return@Button
          }

          if (activeCustomer == null) {
            errorMessage = "Please select or add a customer first"
            return@Button
          }

          // Client-side Overpayment check for fast user feedback
          if (selectedType == TransactionType.PAYMENT) {
            if (activeCustomer.balance <= 0.0) {
              errorMessage = "Customer has no outstanding balance to pay"
              return@Button
            }
            if (amt > activeCustomer.balance) {
              val formattedBal = indianFormat.format(activeCustomer.balance.toInt())
              errorMessage = "Payment (₹${indianFormat.format(amt.toInt())}) cannot exceed current balance of ₹$formattedBal"
              return@Button
            }
          }

          onConfirm(
            activeCustomer.id,
            activeCustomer.name,
            selectedType,
            amt,
            noteText
          ) { backendError ->
            errorMessage = backendError
          }
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp).testTag("save_transaction_button")
      ) {
        Text("Save Entry", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp).testTag("cancel_transaction_button")
      ) {
        Text("Cancel")
      }
    }
  )
}
