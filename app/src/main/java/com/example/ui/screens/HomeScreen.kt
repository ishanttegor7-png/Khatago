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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Product
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.sync.SyncStatus
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
  businessProfile: BusinessProfile,
  totalLenaHai: Double,
  totalDenaHai: Double,
  todaysHisaab: Double,
  customers: List<Customer>,
  recentTransactions: List<Transaction>,
  onAddUdhaarClick: () -> Unit,
  onAddPaymentClick: () -> Unit,
  onAddCustomerClick: () -> Unit,
  onCreateInvoiceClick: () -> Unit,
  onCustomerClick: (String) -> Unit,
  onViewAllTransactionsClick: () -> Unit,
  onViewAllCustomersClick: () -> Unit,
  todayInvoicesCount: Int = 0,
  todayInvoicesAmount: Double = 0.0,
  pendingInvoicesAmount: Double = 0.0,
  onViewAllInvoicesClick: () -> Unit = {},
  syncStatus: SyncStatus = SyncStatus.NOT_CONFIGURED,
  onSyncClick: () -> Unit = {},
  products: List<Product> = emptyList(),
  onViewProductsClick: () -> Unit = {},
  onViewSalesDashboardClick: () -> Unit = {},
  onViewReportsClick: () -> Unit = {},
  onViewPaymentRemindersClick: () -> Unit = {},
  onViewKhataClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_screen_content"),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // Header Banner
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(KhataGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = KhataGreenPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = businessProfile.businessName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
              Text(
                text = "Namaste, ${businessProfile.ownerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            SyncStatusBadge(
              syncStatus = syncStatus,
              modifier = Modifier.clickable { onSyncClick() }
            )
          }
        }
      }
    }

    // Financial Overview: Lena Hai & Dena Hai Cards
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Lena Hai Card (You will receive)
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LenaHaiBgLight),
            modifier = Modifier
              .weight(1f)
              .testTag("card_lena_hai")
          ) {
            Column(
              modifier = Modifier.padding(14.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Total Lena Hai",
                  style = MaterialTheme.typography.labelMedium,
                  color = LenaHaiGreen,
                  fontWeight = FontWeight.SemiBold
                )
                Icon(
                  imageVector = Icons.Default.ArrowDownward,
                  contentDescription = null,
                  tint = LenaHaiGreen,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "₹${indianFormat.format(totalLenaHai.toInt())}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LenaHaiGreen
              )
              Text(
                text = "Aapko lena hai",
                fontSize = 11.sp,
                color = LenaHaiGreen.copy(alpha = 0.8f)
              )
            }
          }

          // Dena Hai Card (You will give)
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DenaHaiBgLight),
            modifier = Modifier
              .weight(1f)
              .testTag("card_dena_hai")
          ) {
            Column(
              modifier = Modifier.padding(14.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Total Dena Hai",
                  style = MaterialTheme.typography.labelMedium,
                  color = DenaHaiRed,
                  fontWeight = FontWeight.SemiBold
                )
                Icon(
                  imageVector = Icons.Default.ArrowUpward,
                  contentDescription = null,
                  tint = DenaHaiRed,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "₹${indianFormat.format(totalDenaHai.toInt())}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DenaHaiRed
              )
              Text(
                text = "Aapko dena hai",
                fontSize = 11.sp,
                color = DenaHaiRed.copy(alpha = 0.8f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Business Hub Quick Access Cards: Sales Dashboard, Reports, Payment Reminders
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Sales Dashboard Hub Button
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .weight(1f)
              .clickable { onViewSalesDashboardClick() }
              .testTag("home_hub_sales_dashboard")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(KhataGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = KhataGreenPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Dashboard",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Reports Hub Button
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .weight(1f)
              .clickable { onViewReportsClick() }
              .testTag("home_hub_reports")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Assessment,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Reports",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Payment Reminders Hub Button
          val pendingKhataCustomersCount = customers.count { it.balance > 0 }
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (pendingKhataCustomersCount > 0) DenaHaiBgLight else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .weight(1f)
              .clickable { onViewPaymentRemindersClick() }
              .testTag("home_hub_reminders")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (pendingKhataCustomersCount > 0) DenaHaiRed.copy(alpha = 0.15f) else KhataAmberBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = if (pendingKhataCustomersCount > 0) DenaHaiRed else KhataAmber,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (pendingKhataCustomersCount > 0) "Reminders ($pendingKhataCustomersCount)" else "Reminders",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (pendingKhataCustomersCount > 0) DenaHaiRed else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Invoices & Today's Sales Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewSalesDashboardClick() }
            .testTag("card_invoices_overview")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(KhataAmberBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                  contentDescription = null,
                  tint = KhataAmber,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Today's Sales: ₹${indianFormat.format(todayInvoicesAmount.toInt())} ($todayInvoicesCount bills)",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (pendingInvoicesAmount > 0) "Pending Invoices: ₹${indianFormat.format(pendingInvoicesAmount.toInt())}" else "All invoices fully cleared",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (pendingInvoicesAmount > 0) DenaHaiRed else LenaHaiGreen
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "View Sales Dashboard",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // Products & Stock Alert/Summary Card
        val activeProducts = products.filter { it.isActive }
        val lowStockProducts = activeProducts.filter { it.currentStock > 0 && it.currentStock <= it.lowStockThreshold }
        val outOfStockProducts = activeProducts.filter { it.currentStock <= 0.0 }
        val hasStockAlert = lowStockProducts.isNotEmpty() || outOfStockProducts.isNotEmpty()

        Spacer(modifier = Modifier.height(10.dp))
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (hasStockAlert) DenaHaiBgLight else MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProductsClick() }
            .testTag("card_stock_overview")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (hasStockAlert) DenaHaiRed.copy(alpha = 0.15f) else KhataGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Inventory2,
                  contentDescription = null,
                  tint = if (hasStockAlert) DenaHaiRed else KhataGreenPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = when {
                    outOfStockProducts.isNotEmpty() -> "⚠️ ${outOfStockProducts.size} Out of Stock • ${lowStockProducts.size} Low"
                    lowStockProducts.isNotEmpty() -> "⚠️ Low Stock Alert: ${lowStockProducts.size} item(s)"
                    else -> "Inventory: ${products.size} products cataloged"
                  },
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (hasStockAlert) DenaHaiRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (hasStockAlert) "Tap to review inventory & restock" else "All items healthy in stock",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "View Products",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // Quick Action Buttons
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "Quick Actions",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          QuickActionButton(
            label = "Add Udhaar",
            subtitle = "Gave Credit",
            icon = Icons.Default.ArrowUpward,
            iconTint = DenaHaiRed,
            iconBg = DenaHaiBgLight,
            onClick = onAddUdhaarClick,
            modifier = Modifier.weight(1f),
            testTag = "btn_quick_add_udhaar"
          )
          QuickActionButton(
            label = "Add Payment",
            subtitle = "Mila Paisa",
            icon = Icons.Default.ArrowDownward,
            iconTint = LenaHaiGreen,
            iconBg = LenaHaiBgLight,
            onClick = onAddPaymentClick,
            modifier = Modifier.weight(1f),
            testTag = "btn_quick_add_payment"
          )
          QuickActionButton(
            label = "Customer",
            subtitle = "+ Naya Jodo",
            icon = Icons.Default.PersonAdd,
            iconTint = KhataGreenPrimary,
            iconBg = KhataGreenContainer,
            onClick = onAddCustomerClick,
            modifier = Modifier.weight(1f),
            testTag = "btn_quick_add_customer"
          )
          QuickActionButton(
            label = "Invoice",
            subtitle = "Bill Banao",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = KhataAmber,
            iconBg = KhataAmberBg,
            onClick = onCreateInvoiceClick,
            modifier = Modifier.weight(1f),
            testTag = "btn_quick_create_invoice"
          )
        }
      }
    }

    // Recent Customers Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Recent Customers",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (customers.isNotEmpty()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onViewAllCustomersClick() }
                .padding(4.dp)
                .testTag("link_see_all_customers")
            ) {
              Text(
                text = "See All",
                style = MaterialTheme.typography.labelMedium,
                color = KhataGreenPrimary,
                fontWeight = FontWeight.SemiBold
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = KhataGreenPrimary,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (customers.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clickable { onAddCustomerClick() }
              .testTag("home_empty_customers_banner")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(KhataGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = KhataGreenPrimary)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "No customers yet",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Tap here to add your first customer and record udhaar.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("recent_customers_row")
          ) {
            // Add Customer Shortcut Card
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                  .width(115.dp)
                  .height(130.dp)
                  .clickable { onAddCustomerClick() }
                  .testTag("shortcut_add_customer")
              ) {
                Column(
                  modifier = Modifier.padding(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(KhataGreenContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = null,
                      tint = KhataGreenPrimary,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Add New",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KhataGreenPrimary
                  )
                  Text(
                    text = "Customer",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            items(customers.take(6), key = { it.id }) { customer ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                  .width(135.dp)
                  .height(130.dp)
                  .clickable { onCustomerClick(customer.id) }
                  .testTag("recent_customer_${customer.id}")
              ) {
                Column(
                  modifier = Modifier.padding(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(Color(customer.avatarColorHex)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = customer.name.take(1).uppercase(),
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = customer.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  val balanceColor = when {
                    customer.isLenaHai -> LenaHaiGreen
                    customer.isDenaHai -> DenaHaiRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  }
                  val balanceText = when {
                    customer.isLenaHai -> "₹${indianFormat.format(customer.balance.toInt())}"
                    customer.isDenaHai -> "₹${indianFormat.format((-customer.balance).toInt())}"
                    else -> "Settled"
                  }
                  Text(
                    text = balanceText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                  )
                  Text(
                    text = if (customer.isLenaHai) "Lena hai" else if (customer.isDenaHai) "Dena hai" else "Settled",
                    fontSize = 10.sp,
                    color = balanceColor.copy(alpha = 0.8f)
                  )
                }
              }
            }
          }
        }
      }
    }

    // Recent Transactions Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (recentTransactions.isNotEmpty()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onViewAllTransactionsClick() }
                .padding(4.dp)
                .testTag("link_see_all_transactions")
            ) {
              Text(
                text = "Dekhein (All)",
                style = MaterialTheme.typography.labelMedium,
                color = KhataGreenPrimary,
                fontWeight = FontWeight.SemiBold
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = KhataGreenPrimary,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }

    if (recentTransactions.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("home_empty_transactions_banner")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "No recent transactions",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Your recent udhaar or payment records will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    } else {
      // List of recent transactions
      items(recentTransactions.take(5), key = { it.id }) { tx ->
        val isCredit = tx.type == TransactionType.CREDIT
        val isPayment = tx.type == TransactionType.PAYMENT
        val amountColor = if (isCredit) DenaHaiRed else LenaHaiGreen

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onCustomerClick(tx.customerId) }
            .testTag("transaction_item_${tx.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isPayment) LenaHaiBgLight else DenaHaiBgLight),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isPayment) Icons.Default.CheckCircle else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (isPayment) LenaHaiGreen else DenaHaiRed,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tx.customerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = tx.note.ifBlank { if (isCredit) "Udhaar Diya" else "Payment Mila" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
              Text(
                text = tx.date,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${if (isPayment) "+" else "-"}₹${indianFormat.format(tx.amount.toInt())}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
              )
              Text(
                text = if (isPayment) "Payment (Mila)" else "Udhaar Diya",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = amountColor.copy(alpha = 0.8f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionButton(
  label: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .height(96.dp)
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 9.sp,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
