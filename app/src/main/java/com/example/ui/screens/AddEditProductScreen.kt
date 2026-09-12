package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
  existingProduct: Product? = null,
  onSave: (
    name: String,
    sku: String,
    category: String,
    purchasePrice: Double,
    sellingPrice: Double,
    initialStock: Double,
    lowStockThreshold: Double,
    unit: String
  ) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf(existingProduct?.name ?: "") }
  var sku by remember { mutableStateOf(existingProduct?.sku ?: "") }
  var category by remember { mutableStateOf(existingProduct?.category ?: "") }
  var purchasePriceText by remember { mutableStateOf(if (existingProduct != null && existingProduct.purchasePrice > 0) existingProduct.purchasePrice.toString() else "") }
  var sellingPriceText by remember { mutableStateOf(if (existingProduct != null && existingProduct.sellingPrice > 0) existingProduct.sellingPrice.toString() else "") }
  var initialStockText by remember { mutableStateOf(if (existingProduct != null) existingProduct.currentStock.toString() else "0") }
  var lowStockThresholdText by remember { mutableStateOf(if (existingProduct != null) existingProduct.lowStockThreshold.toString() else "5") }
  var unit by remember { mutableStateOf(existingProduct?.unit ?: "piece") }

  var nameError by remember { mutableStateOf<String?>(null) }
  var priceError by remember { mutableStateOf<String?>(null) }
  var unitDropdownExpanded by remember { mutableStateOf(false) }

  val commonUnits = listOf("piece", "kg", "g", "litre", "mL", "packet", "box", "dozen", "metre")
  val commonCategories = listOf("Groceries", "Dairy", "Grains & Atta", "Beverages", "Snacks", "Personal Care", "Hardware", "Stationery", "General")

  val purchase = purchasePriceText.toDoubleOrNull() ?: 0.0
  val selling = sellingPriceText.toDoubleOrNull() ?: 0.0
  val margin = selling - purchase
  val marginPercent = if (selling > 0 && purchase > 0) ((margin / purchase) * 100).toInt() else null

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("add_edit_product_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingProduct != null) "Edit Product" else "Add New Product",
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_add_edit_product")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Card 1: Basic Details
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sell, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Product Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              nameError = null
            },
            label = { Text("Product Name *") },
            placeholder = { Text("e.g. Aashirvaad Atta 10kg") },
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_product_name")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = sku,
              onValueChange = { sku = it },
              label = { Text("SKU / Barcode (Optional)") },
              placeholder = { Text("e.g. 890123") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_product_sku")
            )

            // Unit Selector
            Box(modifier = Modifier.weight(1f)) {
              OutlinedTextField(
                value = unit,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = {
                  Text(
                    text = "▼",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { unitDropdownExpanded = true }
                  .testTag("input_product_unit")
              )
              DropdownMenu(
                expanded = unitDropdownExpanded,
                onDismissRequest = { unitDropdownExpanded = false }
              ) {
                commonUnits.forEach { u ->
                  DropdownMenuItem(
                    text = { Text(u) },
                    onClick = {
                      unit = u
                      unitDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (e.g. Groceries, Dairy)") },
            placeholder = { Text("Enter or select below") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_product_category")
          )

          Spacer(modifier = Modifier.height(8.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(commonCategories) { cat ->
              FilterChip(
                selected = category.equals(cat, ignoreCase = true),
                onClick = { category = cat },
                label = { Text(cat, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = KhataGreenContainer,
                  selectedLabelColor = KhataGreenPrimary
                )
              )
            }
          }
        }
      }

      // Card 2: Pricing & Margins
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pricing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = purchasePriceText,
              onValueChange = {
                purchasePriceText = it
                priceError = null
              },
              label = { Text("Purchase Price (₹)") },
              placeholder = { Text("0.00") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_product_purchase_price")
            )

            OutlinedTextField(
              value = sellingPriceText,
              onValueChange = {
                sellingPriceText = it
                priceError = null
              },
              label = { Text("Selling Price (₹) *") },
              placeholder = { Text("0.00") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              isError = priceError != null,
              supportingText = { if (priceError != null) Text(priceError!!, color = MaterialTheme.colorScheme.error) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_product_selling_price")
            )
          }

          if (selling > 0 && purchase > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(LenaHaiBgLight, RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Profit Margin per $unit:",
                  style = MaterialTheme.typography.bodySmall,
                  color = LenaHaiGreen,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "₹${"%.2f".format(margin)} ${if (marginPercent != null) "($marginPercent%)" else ""}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = LenaHaiGreen,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // Card 3: Stock & Inventory
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stock & Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (existingProduct == null) {
            OutlinedTextField(
              value = initialStockText,
              onValueChange = { initialStockText = it },
              label = { Text("Opening Stock Quantity ($unit)") },
              placeholder = { Text("0") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_product_initial_stock")
            )
            Spacer(modifier = Modifier.height(10.dp))
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Current Stock: ${existingProduct.currentStock} $unit. (To change stock, use Stock In / Stock Out on product detail)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          OutlinedTextField(
            value = lowStockThresholdText,
            onValueChange = { lowStockThresholdText = it },
            label = { Text("Low Stock Alert Threshold ($unit)") },
            placeholder = { Text("5") },
            supportingText = { Text("Alerts you when stock falls to or below this amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_product_low_stock_threshold")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Save Button
      Button(
        onClick = {
          val trimmedName = name.trim()
          if (trimmedName.isBlank()) {
            nameError = "Product name is required"
            return@Button
          }
          val sellPrice = sellingPriceText.toDoubleOrNull()
          if (sellPrice == null || sellPrice < 0.0) {
            priceError = "Enter a valid selling price"
            return@Button
          }
          val purchPrice = purchasePriceText.toDoubleOrNull() ?: 0.0
          val initStock = initialStockText.toDoubleOrNull() ?: 0.0
          val threshold = lowStockThresholdText.toDoubleOrNull() ?: 5.0

          onSave(
            trimmedName,
            sku.trim(),
            category.trim(),
            purchPrice,
            sellPrice,
            initStock,
            threshold,
            unit.trim().ifBlank { "piece" }
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("btn_save_product")
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (existingProduct != null) "Update Product" else "Save Product",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
