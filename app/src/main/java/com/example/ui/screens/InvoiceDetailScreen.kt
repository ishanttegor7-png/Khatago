package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Invoice
import com.example.model.InvoicePaymentStatus
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.InvoiceShareHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
  invoice: Invoice?,
  onBackClick: () -> Unit,
  onEditClick: (String) -> Unit,
  onDeleteClick: (String) -> Unit,
  onRecordPayment: (amount: Double, note: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  var showPaymentDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var paymentAmountText by remember { mutableStateOf("") }
  var paymentNoteText by remember { mutableStateOf("") }
  var paymentError by remember { mutableStateOf<String?>(null) }

  if (invoice == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Invoice Not Found") },
          navigationIcon = {
            IconButton(onClick = onBackClick) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        )
      }
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("The requested invoice does not exist or was deleted.")
      }
    }
    return
  }

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

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("invoice_detail_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = invoice.invoiceNumber,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = invoice.customerName,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick, modifier = Modifier.testTag("invoice_detail_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { InvoiceShareHelper.printInvoicePdf(context, invoice) },
            modifier = Modifier.testTag("invoice_action_print")
          ) {
            Icon(Icons.Default.Print, contentDescription = "Print PDF")
          }
          IconButton(
            onClick = { onEditClick(invoice.id) },
            modifier = Modifier.testTag("invoice_action_edit")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
          }
          IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.testTag("invoice_action_delete")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = { InvoiceShareHelper.shareInvoicePdf(context, invoice) },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("invoice_bottom_share_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share PDF")
          }

          if (invoice.remainingAmount > 0.0) {
            Button(
              onClick = {
                paymentAmountText = if (invoice.remainingAmount % 1.0 == 0.0) {
                  invoice.remainingAmount.toInt().toString()
                } else {
                  "%.2f".format(invoice.remainingAmount)
                }
                paymentError = null
                showPaymentDialog = true
              },
              modifier = Modifier
                .weight(1.2f)
                .height(48.dp)
                .testTag("invoice_bottom_pay_btn"),
              colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record Payment", fontWeight = FontWeight.Bold)
            }
          } else {
            Button(
              onClick = { InvoiceShareHelper.printInvoicePdf(context, invoice) },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("invoice_bottom_print_btn"),
              colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Print", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Professional Invoice Card Container
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("invoice_paper_preview")
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {

          // Header: Business and Invoice Number
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = invoice.businessName.ifBlank { "KhataGo Merchant" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KhataGreenPrimary
              )
              if (invoice.businessPhone.isNotBlank()) {
                Text(
                  text = "Ph: ${invoice.businessPhone}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (invoice.businessAddress.isNotBlank()) {
                Text(
                  text = invoice.businessAddress,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (invoice.businessUpi.isNotBlank()) {
                Text(
                  text = "UPI: ${invoice.businessUpi}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg
              ) {
                Text(
                  text = invoice.paymentStatus.displayName.uppercase(),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = statusColor,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = if (invoice.taxEnabled) "TAX INVOICE" else "INVOICE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          Spacer(modifier = Modifier.height(14.dp))

          // Bill To & Invoice Info
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Bill To
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "BILL TO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = invoice.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (invoice.customerPhone.isNotBlank()) {
                Text(
                  text = "Mob: ${invoice.customerPhone}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (invoice.customerAddress.isNotBlank()) {
                Text(
                  text = invoice.customerAddress,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Invoice Date & Due Date
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "INVOICE DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Date: ${invoice.invoiceDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (invoice.dueDate.isNotBlank()) {
                Text(
                  text = "Due: ${invoice.dueDate}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Items Table
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("ITEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
              Text("QTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
              Text("RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
              Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          invoice.items.forEachIndexed { index, item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(2f)) {
                Text(
                  text = item.productName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium
                )
                if (item.discount > 0.0) {
                  Text(
                    text = "Item Disc: -₹${indianFormat.format(item.discount.toInt())}",
                    fontSize = 11.sp,
                    color = DenaHaiRed
                  )
                }
              }
              Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.7f)
              )
              Text(
                text = "₹${indianFormat.format(item.unitPrice.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "₹${indianFormat.format(item.itemTotal.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
              )
            }
            if (index < invoice.items.size - 1) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          Spacer(modifier = Modifier.height(12.dp))

          // Financial Breakdown
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("₹${indianFormat.format(invoice.subtotal.toInt())}", style = MaterialTheme.typography.bodyMedium)
            }

            if (invoice.discount > 0.0) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Special Discount", style = MaterialTheme.typography.bodyMedium, color = DenaHaiRed)
                Text("- ₹${indianFormat.format(invoice.discount.toInt())}", style = MaterialTheme.typography.bodyMedium, color = DenaHaiRed)
              }
            }

            if (invoice.taxEnabled && invoice.taxAmount > 0.0) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("GST (${invoice.taxRate.toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+ ₹${indianFormat.format(invoice.taxAmount.toInt())}", style = MaterialTheme.typography.bodyMedium)
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Grand Total Card
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = KhataGreenContainer,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Grand Total",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = KhataGreenPrimary
                )
                Text(
                  text = "₹${indianFormat.format(invoice.grandTotal.toInt())}",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = KhataGreenPrimary
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Amount Paid", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LenaHaiGreen)
              Text("₹${indianFormat.format(invoice.paidAmount.toInt())}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LenaHaiGreen)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Balance Due",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (invoice.remainingAmount > 0) DenaHaiRed else LenaHaiGreen
              )
              Text(
                text = "₹${indianFormat.format(invoice.remainingAmount.toInt())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (invoice.remainingAmount > 0) DenaHaiRed else LenaHaiGreen
              )
            }
          }

          if (invoice.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Notes / Terms:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = invoice.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Payment Recording Dialog
  if (showPaymentDialog) {
    AlertDialog(
      onDismissRequest = { showPaymentDialog = false },
      title = { Text("Record Invoice Payment") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Customer: ${invoice.customerName}\nRemaining Balance: ₹${indianFormat.format(invoice.remainingAmount.toInt())}",
            style = MaterialTheme.typography.bodyMedium
          )

          OutlinedTextField(
            value = paymentAmountText,
            onValueChange = {
              paymentAmountText = it
              paymentError = null
            },
            label = { Text("Payment Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = paymentError != null,
            supportingText = { if (paymentError != null) Text(paymentError!!, color = MaterialTheme.colorScheme.error) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
          )

          OutlinedTextField(
            value = paymentNoteText,
            onValueChange = { paymentNoteText = it },
            label = { Text("Payment Note (Optional)") },
            placeholder = { Text("e.g. Cash / GPay") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("payment_note_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) {
              paymentError = "Enter a valid amount greater than 0"
            } else if (amt > invoice.remainingAmount) {
              paymentError = "Cannot exceed balance ₹${invoice.remainingAmount}"
            } else {
              onRecordPayment(amt, paymentNoteText)
              showPaymentDialog = false
              Toast.makeText(context, "Payment of ₹$amt recorded", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
          modifier = Modifier.testTag("confirm_payment_btn")
        ) {
          Text("Save Payment")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPaymentDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Invoice?") },
      text = {
        Text("Are you sure you want to delete Invoice #${invoice.invoiceNumber}? Linked transaction records for this invoice will be removed and customer balance will update accordingly.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDeleteClick(invoice.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_invoice_btn")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
