package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.DatePeriodFilter
import com.example.model.Invoice
import com.example.model.Product
import com.example.model.StockMovement
import com.example.model.Transaction
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataGreenPrimary
import com.example.util.DashboardCalculator
import com.example.util.DataExportHelper

@Composable
fun ExportDialog(
  businessProfile: BusinessProfile,
  customers: List<Customer>,
  transactions: List<Transaction>,
  invoices: List<Invoice>,
  products: List<Product>,
  stockMovements: List<StockMovement>,
  isCsvExportAllowed: Boolean = false,
  canExportPdf: Boolean = true,
  onRecordPdfExport: () -> Unit = {},
  onUpgradeClick: () -> Unit = {},
  onShowLimitMessage: (String) -> Unit = {},
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.testTag("export_dialog"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = null,
          tint = KhataGreenPrimary
        )
        Text(
          text = "Export Business Data",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Choose an export format below to backup, share, or analyze your records in PDF or CSV formats.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // PDF Report
        ExportOptionRow(
          title = "Executive PDF Report",
          subtitle = "Complete shop summary with sales, khata & stock",
          icon = Icons.Default.PictureAsPdf,
          iconTint = Color(0xFFD32F2F),
          badge = if (!canExportPdf) "LIMIT REACHED" else null,
          onClick = {
            if (!canExportPdf) {
              onDismiss()
              onShowLimitMessage("Free plan limit reached (10/10 PDF exports this month). Upgrade to KhataGo Premium for unlimited PDF exports.")
              return@ExportOptionRow
            }
            try {
              val sales = DashboardCalculator.calculateSalesReport(invoices, DatePeriodFilter.THIS_MONTH)
              val khata = DashboardCalculator.calculateKhataReport(customers, transactions, DatePeriodFilter.ALL_TIME)
              val stock = DashboardCalculator.calculateProductStockReport(products, stockMovements, DatePeriodFilter.ALL_TIME)
              val cust = DashboardCalculator.calculateCustomerReport(customers, invoices)

              val pdfFile = DataExportHelper.exportBusinessReportPdf(
                context, businessProfile, "This Month", sales, khata, stock, cust
              )
              onRecordPdfExport()
              DataExportHelper.shareExportFile(context, pdfFile, "application/pdf", "Share Business PDF Report")
              onDismiss()
            } catch (e: Exception) {
              Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        )

        val handleCsvClick: (() -> Unit) -> Unit = { exportAction ->
          if (!isCsvExportAllowed) {
            onDismiss()
            onShowLimitMessage("CSV export is available exclusively for KhataGo Premium members. Upgrade to KhataGo Premium to export CSV files.")
          } else {
            exportAction()
          }
        }

        // Invoices CSV
        ExportOptionRow(
          title = "Invoices (CSV)",
          subtitle = "All invoices with items, amounts & status (${invoices.size})",
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          iconTint = KhataAmber,
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val file = DataExportHelper.exportInvoicesCsv(context, invoices)
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Invoices CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )

        // Customers CSV
        ExportOptionRow(
          title = "Customers & Balances (CSV)",
          subtitle = "Customer list with phone & khata balance (${customers.size})",
          icon = Icons.Default.People,
          iconTint = KhataGreenPrimary,
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val file = DataExportHelper.exportCustomersCsv(context, customers)
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Customers CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )

        // Khata Transactions CSV
        ExportOptionRow(
          title = "Khata Transactions (CSV)",
          subtitle = "All CREDIT and PAYMENT ledger entries (${transactions.size})",
          icon = Icons.Default.Payments,
          iconTint = Color(0xFF1976D2),
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val file = DataExportHelper.exportTransactionsCsv(context, transactions)
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Transactions CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )

        // Products Catalog CSV
        ExportOptionRow(
          title = "Products Catalog (CSV)",
          subtitle = "SKUs, prices, categories & stock count (${products.size})",
          icon = Icons.Default.Inventory2,
          iconTint = Color(0xFF7B1FA2),
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val file = DataExportHelper.exportProductsCsv(context, products)
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Products CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )

        // Stock Movements CSV
        ExportOptionRow(
          title = "Stock Movements History (CSV)",
          subtitle = "Stock In, Stock Out & Adjustment logs (${stockMovements.size})",
          icon = Icons.Default.SyncAlt,
          iconTint = Color(0xFF00796B),
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val file = DataExportHelper.exportStockMovementsCsv(context, stockMovements, products)
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Stock History CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )

        // Complete Business Summary CSV
        ExportOptionRow(
          title = "Complete Business Summary (CSV)",
          subtitle = "Financial, sales, ledger & inventory summary sheet",
          icon = Icons.Default.TableChart,
          iconTint = Color(0xFFE65100),
          badge = if (!isCsvExportAllowed) "PREMIUM" else null,
          onClick = {
            handleCsvClick {
              try {
                val sales = DashboardCalculator.calculateSalesReport(invoices, DatePeriodFilter.THIS_MONTH)
                val khata = DashboardCalculator.calculateKhataReport(customers, transactions, DatePeriodFilter.ALL_TIME)
                val stock = DashboardCalculator.calculateProductStockReport(products, stockMovements, DatePeriodFilter.ALL_TIME)
                val cust = DashboardCalculator.calculateCustomerReport(customers, invoices)

                val file = DataExportHelper.exportBusinessSummaryCsv(
                  context, businessProfile, "Complete", sales, khata, stock, cust
                )
                DataExportHelper.shareExportFile(context, file, "text/csv", "Share Business Summary CSV")
                onDismiss()
              } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        )
      }
    },
    confirmButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Close")
      }
    }
  )
}

@Composable
private fun ExportOptionRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  badge: String? = null,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (badge != null) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFD97706).copy(alpha = 0.18f)
            ) {
              Text(
                text = badge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB45309),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Download",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
