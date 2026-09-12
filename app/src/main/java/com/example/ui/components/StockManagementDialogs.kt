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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TextButton
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
import com.example.model.Product
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen

@Composable
fun StockInDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Double, reason: String) -> Unit
) {
  var quantityText by remember { mutableStateOf("") }
  var selectedReason by remember { mutableStateOf("Supplier Purchase") }
  var customReasonText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val commonReasons = listOf("Supplier Purchase", "Restock", "Customer Return", "Correction", "Other")

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.testTag("dialog_stock_in"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(LenaHaiBgLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = LenaHaiGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = "Stock In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "${product.name} (Current: ${product.currentStock} ${product.unit})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = quantityText,
          onValueChange = {
            quantityText = it
            errorMessage = null
          },
          label = { Text("Quantity to Add (${product.unit}) *") },
          placeholder = { Text("e.g. 10") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = errorMessage != null,
          supportingText = {
            if (errorMessage != null) {
              Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_stock_in_qty")
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Reason / Source", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          commonReasons.take(3).forEach { r ->
            FilterChip(
              selected = selectedReason == r,
              onClick = { selectedReason = r },
              label = { Text(r, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LenaHaiBgLight,
                selectedLabelColor = LenaHaiGreen
              )
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          commonReasons.drop(3).forEach { r ->
            FilterChip(
              selected = selectedReason == r,
              onClick = { selectedReason = r },
              label = { Text(r, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LenaHaiBgLight,
                selectedLabelColor = LenaHaiGreen
              )
            )
          }
        }

        if (selectedReason == "Other") {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = customReasonText,
            onValueChange = { customReasonText = it },
            label = { Text("Specify Reason") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = quantityText.toDoubleOrNull()
          if (qty == null || qty <= 0.0) {
            errorMessage = "Enter a valid positive quantity"
          } else {
            val finalReason = if (selectedReason == "Other" && customReasonText.isNotBlank()) customReasonText else selectedReason
            onConfirm(qty, finalReason)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = LenaHaiGreen),
        modifier = Modifier.testTag("btn_confirm_stock_in")
      ) {
        Text("Add Stock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun StockOutDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Double, reason: String) -> Unit
) {
  var quantityText by remember { mutableStateOf("") }
  var selectedReason by remember { mutableStateOf("Damaged / Expired") }
  var customReasonText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val commonReasons = listOf("Damaged / Expired", "Internal Use", "Customer Return", "Loss / Theft", "Other")

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.testTag("dialog_stock_out"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DenaHaiBgLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Remove, contentDescription = null, tint = DenaHaiRed, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = "Stock Out / Deduct", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "${product.name} (Available: ${product.currentStock} ${product.unit})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = quantityText,
          onValueChange = {
            quantityText = it
            errorMessage = null
          },
          label = { Text("Quantity to Deduct (${product.unit}) *") },
          placeholder = { Text("e.g. 2") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = errorMessage != null,
          supportingText = {
            if (errorMessage != null) {
              Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_stock_out_qty")
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Reason for Deduction", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          commonReasons.take(3).forEach { r ->
            FilterChip(
              selected = selectedReason == r,
              onClick = { selectedReason = r },
              label = { Text(r, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DenaHaiBgLight,
                selectedLabelColor = DenaHaiRed
              )
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          commonReasons.drop(3).forEach { r ->
            FilterChip(
              selected = selectedReason == r,
              onClick = { selectedReason = r },
              label = { Text(r, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DenaHaiBgLight,
                selectedLabelColor = DenaHaiRed
              )
            )
          }
        }

        if (selectedReason == "Other") {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = customReasonText,
            onValueChange = { customReasonText = it },
            label = { Text("Specify Reason") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = quantityText.toDoubleOrNull()
          if (qty == null || qty <= 0.0) {
            errorMessage = "Enter a valid positive quantity"
          } else if (qty > product.currentStock) {
            errorMessage = "Cannot deduct more than available stock (${product.currentStock} ${product.unit})"
          } else {
            val finalReason = if (selectedReason == "Other" && customReasonText.isNotBlank()) customReasonText else selectedReason
            onConfirm(qty, finalReason)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = DenaHaiRed),
        modifier = Modifier.testTag("btn_confirm_stock_out")
      ) {
        Text("Deduct Stock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun StockAdjustmentDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (newStock: Double, reason: String) -> Unit
) {
  var newStockText by remember { mutableStateOf(product.currentStock.toString()) }
  var selectedReason by remember { mutableStateOf("Physical Inventory Count") }
  var customReasonText by remember { mutableStateOf("") }
  var showConfirmStep by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val targetNewStock = newStockText.toDoubleOrNull() ?: product.currentStock
  val diff = targetNewStock - product.currentStock

  val commonReasons = listOf("Physical Inventory Count", "Audit Reconciliation", "Initial Stock Correction", "Other")

  if (!showConfirmStep) {
    AlertDialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(decorFitsSystemWindows = false),
      modifier = Modifier.testTag("dialog_stock_adjust"),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(KhataAmberBg),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = KhataAmber, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(text = "Adjust Stock Count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = "${product.name} (Current: ${product.currentStock} ${product.unit})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = newStockText,
            onValueChange = {
              newStockText = it
              errorMessage = null
            },
            label = { Text("Actual Counted Stock (${product.unit}) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errorMessage != null,
            supportingText = {
              if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
              } else {
                val diffFormatted = if (diff >= 0) "+$diff" else "$diff"
                Text(
                  text = "Difference: $diffFormatted ${product.unit}",
                  color = if (diff >= 0) LenaHaiGreen else DenaHaiRed,
                  fontWeight = FontWeight.Bold
                )
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_adjust_stock")
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "Adjustment Reason *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(6.dp))

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            commonReasons.chunked(2).forEach { rowReasons ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                rowReasons.forEach { r ->
                  FilterChip(
                    selected = selectedReason == r,
                    onClick = { selectedReason = r },
                    label = { Text(r, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = KhataAmberBg,
                      selectedLabelColor = KhataAmber
                    )
                  )
                }
              }
            }
          }

          if (selectedReason == "Other") {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = customReasonText,
              onValueChange = { customReasonText = it },
              label = { Text("Specify Reason *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val count = newStockText.toDoubleOrNull()
            if (count == null || count < 0.0) {
              errorMessage = "Stock cannot be negative"
            } else if (selectedReason == "Other" && customReasonText.isBlank()) {
              errorMessage = "Please enter a reason for adjustment"
            } else {
              // Ask confirmation especially if reducing stock or large difference
              showConfirmStep = true
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
          modifier = Modifier.testTag("btn_continue_adjust_stock")
        ) {
          Text("Continue")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    )
  } else {
    // Confirmation step
    AlertDialog(
      onDismissRequest = { showConfirmStep = false },
      icon = {
        Icon(Icons.Default.Warning, contentDescription = null, tint = KhataAmber, modifier = Modifier.size(32.dp))
      },
      title = {
        Text("Confirm Stock Adjustment")
      },
      text = {
        Column {
          Text(
            text = "Are you sure you want to update stock for ${product.name} from ${product.currentStock} ${product.unit} to $targetNewStock ${product.unit}?",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Reason: ${if (selectedReason == "Other") customReasonText else selectedReason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val finalReason = if (selectedReason == "Other") customReasonText else selectedReason
            onConfirm(targetNewStock, finalReason)
          },
          colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
          modifier = Modifier.testTag("btn_confirm_adjust_stock")
        ) {
          Text("Confirm Adjustment")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmStep = false }) {
          Text("Back")
        }
      }
    )
  }
}
