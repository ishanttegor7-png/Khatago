package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductPickerDialog(
  products: List<Product>,
  onDismiss: () -> Unit,
  onProductSelected: (Product) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  val filtered = remember(products, searchQuery) {
    products.filter { p ->
      p.isActive && (
        searchQuery.isBlank() ||
          p.name.contains(searchQuery, ignoreCase = true) ||
          p.sku.contains(searchQuery, ignoreCase = true) ||
          p.category.contains(searchQuery, ignoreCase = true)
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.fillMaxWidth(0.95f).testTag("dialog_product_picker"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.size(8.dp))
        Text("Select Product from Catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search catalog...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          shape = RoundedCornerShape(10.dp),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_picker_search")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (searchQuery.isNotBlank()) "No matching products found" else "No active products available in catalog",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filtered, key = { it.id }) { prod ->
              val isOutOfStock = prod.currentStock <= 0.0
              val isLowStock = prod.currentStock in 0.0001..prod.lowStockThreshold

              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onProductSelected(prod) }
                  .testTag("picker_item_${prod.id}")
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (prod.category.isNotBlank()) {
                        Text(text = "${prod.category} • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      Text(
                        text = "₹${indianFormat.format(prod.sellingPrice)} / ${prod.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }

                  // Stock Badge
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                      isOutOfStock -> DenaHaiBgLight
                      isLowStock -> KhataAmberBg
                      else -> LenaHaiBgLight
                    }
                  ) {
                    Text(
                      text = "${prod.currentStock} ${prod.unit}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = when {
                        isOutOfStock -> DenaHaiRed
                        isLowStock -> KhataAmber
                        else -> LenaHaiGreen
                      },
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
