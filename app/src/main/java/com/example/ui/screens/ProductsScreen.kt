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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.StockMovementType
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
fun ProductsScreen(
  products: List<Product>,
  stockMovements: List<StockMovement>,
  onAddProductClick: () -> Unit,
  onProductClick: (String) -> Unit,
  onStockIn: (productId: String, quantity: Double, reason: String) -> Unit,
  onStockOut: (productId: String, quantity: Double, reason: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var showLowStockOnly by remember { mutableStateOf(false) }
  var showOutOfStockOnly by remember { mutableStateOf(false) }
  var showActiveOnly by remember { mutableStateOf(true) }

  var quickStockInProduct by remember { mutableStateOf<Product?>(null) }
  var quickStockOutProduct by remember { mutableStateOf<Product?>(null) }

  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
  val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

  // Derive categories
  val availableCategories = remember(products) {
    listOf("All") + products.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
  }

  // Filtered Products
  val filteredProducts = remember(products, searchQuery, selectedCategory, showLowStockOnly, showOutOfStockOnly, showActiveOnly) {
    products.filter { p ->
      val matchesSearch = searchQuery.isBlank() ||
        p.name.contains(searchQuery, ignoreCase = true) ||
        p.sku.contains(searchQuery, ignoreCase = true) ||
        p.category.contains(searchQuery, ignoreCase = true)
      val matchesCategory = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
      val matchesLowStock = !showLowStockOnly || (p.currentStock <= p.lowStockThreshold)
      val matchesOutOfStock = !showOutOfStockOnly || (p.currentStock <= 0.0)
      val matchesActive = !showActiveOnly || p.isActive
      matchesSearch && matchesCategory && matchesLowStock && matchesOutOfStock && matchesActive
    }
  }

  val lowStockCount = remember(products) {
    products.count { it.isActive && it.currentStock <= it.lowStockThreshold }
  }
  val outOfStockCount = remember(products) {
    products.count { it.isActive && it.currentStock <= 0.0 }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("products_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Products & Stock",
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    floatingActionButton = {
      if (selectedTab == 0) {
        FloatingActionButton(
          onClick = onAddProductClick,
          containerColor = KhataGreenPrimary,
          contentColor = Color.White,
          modifier = Modifier.testTag("fab_add_product")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Primary Tabs
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = KhataGreenPrimary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Products (${products.size})", fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.testTag("tab_products_list")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Stock Log", fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.testTag("tab_stock_log")
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Low Stock", fontWeight = FontWeight.SemiBold)
              if (lowStockCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  shape = CircleShape,
                  color = KhataAmber,
                  modifier = Modifier.size(18.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "$lowStockCount",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          },
          modifier = Modifier.testTag("tab_low_stock")
        )
      }

      when (selectedTab) {
        // TAB 0: ALL PRODUCTS
        0 -> {
          Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search by name, SKU or category...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("input_search_products")
            )

            // Category & Filter Chips
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(availableCategories) { cat ->
                FilterChip(
                  selected = selectedCategory == cat,
                  onClick = { selectedCategory = cat },
                  label = { Text(cat, fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KhataGreenContainer,
                    selectedLabelColor = KhataGreenPrimary
                  )
                )
              }

              item {
                FilterChip(
                  selected = showLowStockOnly,
                  onClick = { showLowStockOnly = !showLowStockOnly },
                  label = { Text("⚠️ Low Stock (${lowStockCount})", fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KhataAmberBg,
                    selectedLabelColor = KhataAmber
                  ),
                  modifier = Modifier.testTag("chip_filter_low_stock")
                )
              }

              item {
                FilterChip(
                  selected = showOutOfStockOnly,
                  onClick = { showOutOfStockOnly = !showOutOfStockOnly },
                  label = { Text("⛔ Out of Stock (${outOfStockCount})", fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DenaHaiBgLight,
                    selectedLabelColor = DenaHaiRed
                  ),
                  modifier = Modifier.testTag("chip_filter_out_of_stock")
                )
              }

              item {
                FilterChip(
                  selected = !showActiveOnly,
                  onClick = { showActiveOnly = !showActiveOnly },
                  label = { Text(if (showActiveOnly) "Active Only" else "Show Inactive", fontSize = 12.sp) }
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredProducts.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = if (searchQuery.isNotBlank() || selectedCategory != "All" || showLowStockOnly) {
                      "No products match your filters"
                    } else {
                      "No products in catalog yet"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Tap the '+' button below to add products with pricing and stock levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(filteredProducts, key = { it.id }) { product ->
                  ProductItemCard(
                    product = product,
                    indianFormat = indianFormat,
                    onClick = { onProductClick(product.id) },
                    onStockInClick = { quickStockInProduct = product },
                    onStockOutClick = { quickStockOutProduct = product }
                  )
                }
              }
            }
          }
        }

        // TAB 1: GLOBAL STOCK MOVEMENTS LOG
        1 -> {
          if (stockMovements.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.History,
                  contentDescription = null,
                  modifier = Modifier.size(56.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No stock movements recorded", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  "When you stock in, stock out, create invoices, or adjust inventory, entries will appear here.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(stockMovements, key = { it.id }) { move ->
                val isPositive = move.quantity > 0
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                  modifier = Modifier.fillMaxWidth().testTag("movement_item_${move.id}")
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(38.dp)
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
                      Text(
                        text = move.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                      )
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = move.movementType.displayName,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = FontWeight.SemiBold
                        )
                        if (move.referenceId.isNotBlank()) {
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = "• #${move.referenceId.take(8)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }
                      if (move.reason.isNotBlank()) {
                        Text(
                          text = move.reason,
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                      }
                      Text(
                        text = dateFormat.format(Date(move.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                      )
                    }

                    Text(
                      text = "${if (isPositive) "+" else ""}${move.quantity}",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = if (isPositive) LenaHaiGreen else DenaHaiRed
                    )
                  }
                }
              }
            }
          }
        }

        // TAB 2: LOW STOCK ALERTS
        2 -> {
          val lowStockList = remember(products) {
            products.filter { it.isActive && it.currentStock <= it.lowStockThreshold }
          }

          if (lowStockList.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                  shape = CircleShape,
                  color = LenaHaiBgLight,
                  modifier = Modifier.size(64.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text("✓", color = LenaHaiGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("All Stock Healthy!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  "None of your active products are below their low-stock thresholds.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              item {
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = KhataAmberBg),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = KhataAmber, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = "${lowStockList.size} Product(s) Need Restocking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KhataAmber
                      )
                      Text(
                        text = "Current stock is at or below threshold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KhataAmber.copy(alpha = 0.9f)
                      )
                    }
                  }
                }
              }

              items(lowStockList, key = { it.id }) { product ->
                val isOutOfStock = product.currentStock <= 0.0
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick(product.id) }
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = product.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold
                        )
                        if (product.category.isNotBlank()) {
                          Text(text = product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      }

                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOutOfStock) DenaHaiBgLight else KhataAmberBg
                      ) {
                        Text(
                          text = if (isOutOfStock) "Out of Stock" else "Low Stock",
                          color = if (isOutOfStock) DenaHaiRed else KhataAmber,
                          fontWeight = FontWeight.Bold,
                          fontSize = 11.sp,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          text = "Current: ${product.currentStock} ${product.unit}",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = if (isOutOfStock) DenaHaiRed else KhataAmber
                        )
                        Text(
                          text = "Threshold: ${product.lowStockThreshold} ${product.unit}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      Button(
                        onClick = { quickStockInProduct = product },
                        colors = ButtonDefaults.buttonColors(containerColor = LenaHaiGreen),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Quick Stock In Dialog
  quickStockInProduct?.let { prod ->
    StockInDialog(
      product = prod,
      onDismiss = { quickStockInProduct = null },
      onConfirm = { qty, reason ->
        quickStockInProduct = null
        onStockIn(prod.id, qty, reason)
      }
    )
  }

  // Quick Stock Out Dialog
  quickStockOutProduct?.let { prod ->
    StockOutDialog(
      product = prod,
      onDismiss = { quickStockOutProduct = null },
      onConfirm = { qty, reason ->
        quickStockOutProduct = null
        onStockOut(prod.id, qty, reason)
      }
    )
  }
}

@Composable
fun ProductItemCard(
  product: Product,
  indianFormat: NumberFormat,
  onClick: () -> Unit,
  onStockInClick: () -> Unit,
  onStockOutClick: () -> Unit
) {
  val isOutOfStock = product.currentStock <= 0.0
  val isLowStock = product.currentStock in 0.0001..product.lowStockThreshold

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("product_card_${product.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (product.category.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
              ) {
                Text(
                  text = product.category,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (!product.isActive) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = DenaHaiBgLight
              ) {
                Text(
                  text = "Inactive",
                  fontSize = 11.sp,
                  color = DenaHaiRed,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        // Stock Status Indicator
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = when {
            isOutOfStock -> DenaHaiBgLight
            isLowStock -> KhataAmberBg
            else -> LenaHaiBgLight
          }
        ) {
          Text(
            text = when {
              isOutOfStock -> "Out of Stock"
              isLowStock -> "Low Stock"
              else -> "In Stock"
            },
            color = when {
              isOutOfStock -> DenaHaiRed
              isLowStock -> KhataAmber
              else -> LenaHaiGreen
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Middle Row: Price & Stock
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Selling Price",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "₹${indianFormat.format(product.sellingPrice)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Current Stock",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${product.currentStock} ${product.unit}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = when {
              isOutOfStock -> DenaHaiRed
              isLowStock -> KhataAmber
              else -> LenaHaiGreen
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      Spacer(modifier = Modifier.height(8.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onStockOutClick,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp).testTag("quick_stock_out_${product.id}")
        ) {
          Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = DenaHaiRed)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Stock Out", fontSize = 11.sp, color = DenaHaiRed, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = onStockInClick,
          colors = ButtonDefaults.buttonColors(containerColor = LenaHaiGreen),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp).testTag("quick_stock_in_${product.id}")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Stock In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
