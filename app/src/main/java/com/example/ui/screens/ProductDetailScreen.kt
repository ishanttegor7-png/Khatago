package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.components.StockInDialog
import com.example.ui.components.StockOutDialog
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
  product: Product?,
  movements: List<StockMovement>,
  onBack: () -> Unit,
  onEdit: (String) -> Unit,
  onToggleActive: (String, Boolean) -> Unit,
  onDelete: (String) -> Unit,
  onStockIn: (productId: String, quantity: Double, reason: String) -> Unit,
  onStockOut: (productId: String, quantity: Double, reason: String) -> Unit,
  onStockAdjust: (productId: String, newStock: Double, reason: String) -> Unit,
  modifier: Modifier = Modifier
) {
  if (product == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Product not found", style = MaterialTheme.typography.titleMedium)
    }
    return
  }

  var showStockInDialog by remember { mutableStateOf(false) }
  var showStockOutDialog by remember { mutableStateOf(false) }
  var showAdjustDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

  val isLowStock = product.currentStock in 0.0001..product.lowStockThreshold
  val isOutOfStock = product.currentStock <= 0.0

  val margin = product.sellingPrice - product.purchasePrice
  val marginPercent = if (product.sellingPrice > 0 && product.purchasePrice > 0) {
    ((margin / product.purchasePrice) * 100).toInt()
  } else null

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("product_detail_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            if (product.category.isNotBlank()) {
              Text(text = product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_product_detail")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onEdit(product.id) }, modifier = Modifier.testTag("btn_edit_product")) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Product")
          }
          IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.testTag("btn_delete_product")) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))

        // Hero Card: Stock Status & Quick Actions
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = when {
              isOutOfStock -> DenaHaiBgLight
              isLowStock -> KhataAmberBg
              else -> LenaHaiBgLight
            }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = "CURRENT STOCK",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = when {
                    isOutOfStock -> DenaHaiRed
                    isLowStock -> KhataAmber
                    else -> LenaHaiGreen
                  }
                )
                Text(
                  text = "${product.currentStock} ${product.unit}",
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = when {
                    isOutOfStock -> DenaHaiRed
                    isLowStock -> KhataAmber
                    else -> LenaHaiGreen
                  }
                )
              }

              // Status Badge
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                  isOutOfStock -> DenaHaiRed
                  isLowStock -> KhataAmber
                  else -> LenaHaiGreen
                }
              ) {
                Text(
                  text = when {
                    isOutOfStock -> "Out of Stock"
                    isLowStock -> "Low Stock"
                    else -> "In Stock"
                  },
                  color = androidx.compose.ui.graphics.Color.White,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            if (isLowStock || isOutOfStock) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = if (isOutOfStock) DenaHaiRed else KhataAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isOutOfStock) "Restock needed immediately" else "Stock is at or below threshold (${product.lowStockThreshold} ${product.unit})",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isOutOfStock) DenaHaiRed else KhataAmber,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Stock Quick Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showStockInDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = LenaHaiGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("btn_stock_in")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stock In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { showStockOutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DenaHaiRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("btn_stock_out")
              ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stock Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = { showAdjustDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("btn_adjust_stock")
              ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adjust", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Pricing & Product Info Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Pricing & Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Selling Price", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("₹${indianFormat.format(product.sellingPrice)} / ${product.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Purchase Price", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("₹${indianFormat.format(product.purchasePrice)} / ${product.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            if (margin > 0) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Profit Margin", style = MaterialTheme.typography.bodyMedium, color = LenaHaiGreen, fontWeight = FontWeight.SemiBold)
                Text(
                  text = "₹${"%.2f".format(margin)} ${if (marginPercent != null) "($marginPercent%)" else ""}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = LenaHaiGreen,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            if (product.sku.isNotBlank()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SKU / Barcode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(product.sku, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Low-Stock Threshold", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${product.lowStockThreshold} ${product.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Product Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  text = if (product.isActive) "Active (Visible in catalog)" else "Inactive (Hidden)",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (product.isActive) LenaHaiGreen else DenaHaiRed
                )
              }
              Switch(
                checked = product.isActive,
                onCheckedChange = { onToggleActive(product.id, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = KhataGreenPrimary, checkedTrackColor = KhataGreenContainer),
                modifier = Modifier.testTag("switch_product_active")
              )
            }
          }
        }
      }

      // Stock Movement History Section Header
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
          Icon(Icons.Default.History, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Stock Movement History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.weight(1f))
          Text("(${movements.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      if (movements.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("No stock movements recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(4.dp))
              Text("Movements from sales, restock, or adjustments will show here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      } else {
        items(movements, key = { it.id }) { move ->
          val isPositive = move.quantity > 0
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (isPositive) LenaHaiBgLight else DenaHaiBgLight),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (isPositive) "+" else "-",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 18.sp,
                  color = if (isPositive) LenaHaiGreen else DenaHaiRed
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = move.movementType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  if (move.referenceId.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                      Text(
                        text = "#${move.referenceId.take(8)}",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                if (move.reason.isNotBlank()) {
                  Text(text = move.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = dateFormat.format(Date(move.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
              }

              Text(
                text = "${if (isPositive) "+" else ""}${move.quantity} ${product.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) LenaHaiGreen else DenaHaiRed
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Stock In Dialog
  if (showStockInDialog) {
    StockInDialog(
      product = product,
      onDismiss = { showStockInDialog = false },
      onConfirm = { qty, reason ->
        showStockInDialog = false
        onStockIn(product.id, qty, reason)
      }
    )
  }

  // Stock Out Dialog
  if (showStockOutDialog) {
    StockOutDialog(
      product = product,
      onDismiss = { showStockOutDialog = false },
      onConfirm = { qty, reason ->
        showStockOutDialog = false
        onStockOut(product.id, qty, reason)
      }
    )
  }

  // Stock Adjust Dialog
  if (showAdjustDialog) {
    StockAdjustmentDialog(
      product = product,
      onDismiss = { showAdjustDialog = false },
      onConfirm = { targetNewStock, reason ->
        showAdjustDialog = false
        onStockAdjust(product.id, targetNewStock, reason)
      }
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Product?") },
      text = {
        Text("Are you sure you want to delete ${product.name}? Its stock movement history will be preserved.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDelete(product.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("btn_confirm_delete_product")
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
