package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KhataGreenPrimary

enum class KhataTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  HOME(
    route = "home",
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_tab_home"
  ),
  CUSTOMERS(
    route = "customers",
    labelRes = R.string.nav_customers,
    selectedIcon = Icons.Filled.People,
    unselectedIcon = Icons.Outlined.People,
    testTag = "nav_tab_customers"
  ),
  PRODUCTS(
    route = "products",
    labelRes = R.string.nav_products,
    selectedIcon = Icons.Filled.Inventory2,
    unselectedIcon = Icons.Outlined.Inventory2,
    testTag = "nav_tab_products"
  ),
  KHATA(
    route = "khata",
    labelRes = R.string.nav_khata,
    selectedIcon = Icons.Filled.AccountBalanceWallet,
    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    testTag = "nav_tab_khata"
  ),
  INVOICE(
    route = "invoice",
    labelRes = R.string.nav_invoice,
    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_tab_invoice"
  ),
  SETTINGS(
    route = "settings",
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_tab_settings"
  )
}

@Composable
fun KhataBottomBar(
  currentRoute: String,
  onTabSelected: (KhataTab) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier.testTag("bottom_navigation_bar"),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = MaterialTheme.colorScheme.surfaceVariant.let { androidx.compose.ui.unit.Dp(3f) }
  ) {
    KhataTab.values().forEach { tab ->
      val isSelected = currentRoute == tab.route
      NavigationBarItem(
        modifier = Modifier.testTag(tab.testTag),
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        icon = {
          Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(tab.labelRes)
          )
        },
        label = {
          Text(
            text = stringResource(tab.labelRes),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = KhataGreenPrimary,
          selectedTextColor = KhataGreenPrimary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )
    }
  }
}
