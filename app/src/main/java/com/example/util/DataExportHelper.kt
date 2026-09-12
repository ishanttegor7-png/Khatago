package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.CustomerReportData
import com.example.model.Invoice
import com.example.model.KhataReportData
import com.example.model.Product
import com.example.model.ProductStockReportData
import com.example.model.SalesReportData
import com.example.model.StockMovement
import com.example.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportHelper {

  private val indianCurrencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
  private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
  private val displayDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

  private fun getExportDir(context: Context): File {
    val dir = File(context.cacheDir, "exports")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun escapeCsv(text: String): String {
    val clean = text.replace("\"", "\"\"")
    return if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
      "\"$clean\""
    } else {
      clean
    }
  }

  fun exportCustomersCsv(context: Context, customers: List<Customer>): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_Customers_$timestamp.csv")
    FileWriter(file).use { writer ->
      writer.append("Customer ID,Name,Phone,Address,Outstanding Balance (Rs),Total Credit (Rs),Total Payment (Rs),Transactions Count,Last Updated\n")
      for (c in customers) {
        writer.append(escapeCsv(c.id)).append(",")
        writer.append(escapeCsv(c.name)).append(",")
        writer.append(escapeCsv(c.phone)).append(",")
        writer.append(escapeCsv(c.address)).append(",")
        writer.append(c.balance.toString()).append(",")
        writer.append(c.totalCredit.toString()).append(",")
        writer.append(c.totalPayment.toString()).append(",")
        writer.append(c.transactionCount.toString()).append(",")
        writer.append(escapeCsv(c.lastUpdated)).append("\n")
      }
    }
    return file
  }

  fun exportTransactionsCsv(context: Context, transactions: List<Transaction>): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_Transactions_$timestamp.csv")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    FileWriter(file).use { writer ->
      writer.append("Transaction ID,Customer ID,Customer Name,Type,Amount (Rs),Note,Date,Timestamp\n")
      for (t in transactions) {
        writer.append(escapeCsv(t.id)).append(",")
        writer.append(escapeCsv(t.customerId)).append(",")
        writer.append(escapeCsv(t.customerName)).append(",")
        writer.append(t.type.name).append(",")
        writer.append(t.amount.toString()).append(",")
        writer.append(escapeCsv(t.note)).append(",")
        writer.append(escapeCsv(dateFormat.format(Date(t.timestamp)))).append(",")
        writer.append(t.timestamp.toString()).append("\n")
      }
    }
    return file
  }

  fun exportInvoicesCsv(context: Context, invoices: List<Invoice>): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_Invoices_$timestamp.csv")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    FileWriter(file).use { writer ->
      writer.append("Invoice Number,Customer Name,Phone,Address,Subtotal (Rs),Discount (Rs),Tax Rate (%),Tax Amount (Rs),Grand Total (Rs),Paid Amount (Rs),Remaining Due (Rs),Status,Items Count,Items Summary,Created Date\n")
      for (inv in invoices) {
        val itemsSummary = inv.items.joinToString("; ") { "${it.productName} (x${it.quantity})" }
        writer.append(escapeCsv(inv.invoiceNumber)).append(",")
        writer.append(escapeCsv(inv.customerName)).append(",")
        writer.append(escapeCsv(inv.customerPhone)).append(",")
        writer.append(escapeCsv(inv.customerAddress)).append(",")
        writer.append(inv.subtotal.toString()).append(",")
        writer.append(inv.discount.toString()).append(",")
        writer.append(if (inv.taxEnabled) inv.taxRate.toString() else "0").append(",")
        writer.append(inv.taxAmount.toString()).append(",")
        writer.append(inv.grandTotal.toString()).append(",")
        writer.append(inv.paidAmount.toString()).append(",")
        writer.append(inv.remainingAmount.toString()).append(",")
        writer.append(inv.paymentStatus.displayName).append(",")
        writer.append(inv.items.size.toString()).append(",")
        writer.append(escapeCsv(itemsSummary)).append(",")
        writer.append(escapeCsv(dateFormat.format(Date(inv.createdTimestamp)))).append("\n")
      }
    }
    return file
  }

  fun exportProductsCsv(context: Context, products: List<Product>): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_Products_$timestamp.csv")
    FileWriter(file).use { writer ->
      writer.append("Product ID,Name,SKU,Category,Purchase Price (Rs),Selling Price (Rs),Current Stock,Unit,Low Stock Threshold,Stock Status,Active\n")
      for (p in products) {
        val stockStatus = when {
          !p.isActive -> "Inactive"
          p.currentStock <= 0.0 -> "Out of Stock"
          p.currentStock <= p.lowStockThreshold -> "Low Stock"
          else -> "In Stock"
        }
        writer.append(escapeCsv(p.id)).append(",")
        writer.append(escapeCsv(p.name)).append(",")
        writer.append(escapeCsv(p.sku)).append(",")
        writer.append(escapeCsv(p.category)).append(",")
        writer.append(p.purchasePrice.toString()).append(",")
        writer.append(p.sellingPrice.toString()).append(",")
        writer.append(p.currentStock.toString()).append(",")
        writer.append(escapeCsv(p.unit)).append(",")
        writer.append(p.lowStockThreshold.toString()).append(",")
        writer.append(stockStatus).append(",")
        writer.append(if (p.isActive) "Yes" else "No").append("\n")
      }
    }
    return file
  }

  fun exportStockMovementsCsv(context: Context, movements: List<StockMovement>, products: List<Product>): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_StockMovements_$timestamp.csv")
    val productMap = products.associateBy { it.id }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    FileWriter(file).use { writer ->
      writer.append("Movement ID,Product ID,Product Name,Type,Quantity Changed,Reason/Note,Date\n")
      for (m in movements) {
        val prodName = productMap[m.productId]?.name ?: m.productId
        writer.append(escapeCsv(m.id)).append(",")
        writer.append(escapeCsv(m.productId)).append(",")
        writer.append(escapeCsv(prodName)).append(",")
        writer.append(m.movementType.name).append(",")
        writer.append(m.quantity.toString()).append(",")
        writer.append(escapeCsv(m.reason)).append(",")
        writer.append(escapeCsv(dateFormat.format(Date(m.timestamp)))).append("\n")
      }
    }
    return file
  }

  fun exportBusinessSummaryCsv(
    context: Context,
    businessProfile: BusinessProfile,
    periodLabel: String,
    salesReport: SalesReportData,
    khataReport: KhataReportData,
    stockReport: ProductStockReportData,
    customerReport: CustomerReportData
  ): File {
    val timestamp = fileDateFormat.format(Date())
    val file = File(getExportDir(context), "KhataGo_Business_Summary_$timestamp.csv")
    FileWriter(file).use { writer ->
      writer.append("=== KHATAGO BUSINESS SUMMARY REPORT ===\n")
      writer.append("Business Name,${escapeCsv(businessProfile.businessName)}\n")
      writer.append("Owner,${escapeCsv(businessProfile.ownerName)}\n")
      writer.append("Phone,${escapeCsv(businessProfile.phone)}\n")
      writer.append("Period,${escapeCsv(periodLabel)}\n")
      writer.append("Report Generated,${escapeCsv(displayDateFormat.format(Date()))}\n\n")

      writer.append("=== 1. SALES SUMMARY ===\n")
      writer.append("Metric,Value\n")
      writer.append("Total Sales (Rs),${salesReport.totalSales}\n")
      writer.append("Total Invoices Count,${salesReport.invoiceCount}\n")
      writer.append("Total Collected Paid (Rs),${salesReport.paidAmount}\n")
      writer.append("Total Pending Amount (Rs),${salesReport.pendingAmount}\n")
      writer.append("Fully Paid Invoices,${salesReport.fullyPaidCount}\n")
      writer.append("Partially Paid Invoices,${salesReport.partiallyPaidCount}\n")
      writer.append("Unpaid Invoices,${salesReport.unpaidCount}\n\n")

      writer.append("=== 2. KHATA LEDGER SUMMARY ===\n")
      writer.append("Metric,Value\n")
      writer.append("Total Credit Given (Rs),${khataReport.totalCredit}\n")
      writer.append("Total Payment Received (Rs),${khataReport.totalPayments}\n")
      writer.append("Total Outstanding Balance (Lena Hai Rs),${khataReport.totalOutstanding}\n")
      writer.append("Customers With Dues,${khataReport.customerOutstandingList.size}\n\n")

      writer.append("=== 3. INVENTORY & STOCK SUMMARY ===\n")
      writer.append("Metric,Value\n")
      writer.append("Total Products Cataloged,${stockReport.totalProducts}\n")
      writer.append("Total Physical Stock Units,${stockReport.totalStockUnits}\n")
      writer.append("Total Stock Valuation Cost (Rs),${stockReport.totalStockValuation}\n")
      writer.append("Total Stock Valuation Retail (Rs),${stockReport.totalRetailValuation}\n")
      writer.append("Low Stock Items Count,${stockReport.lowStockProducts.size}\n")
      writer.append("Out of Stock Items Count,${stockReport.outOfStockProducts.size}\n\n")

      writer.append("=== 4. CUSTOMER REPORT SUMMARY ===\n")
      writer.append("Metric,Value\n")
      writer.append("Total Customers,${customerReport.totalCustomers}\n")
      writer.append("Customers With Pending Dues,${customerReport.customersWithBalanceCount}\n\n")

      writer.append("Top Customers by Invoice Amount:\n")
      writer.append("Customer Name,Phone,Invoices Count,Total Purchases (Rs),Paid (Rs),Outstanding (Rs)\n")
      for (top in customerReport.topCustomersByInvoice.take(10)) {
        writer.append(escapeCsv(top.customerName)).append(",")
        writer.append(escapeCsv(top.customerPhone)).append(",")
        writer.append(top.invoiceCount.toString()).append(",")
        writer.append(top.totalInvoiceAmount.toString()).append(",")
        writer.append(top.paidAmount.toString()).append(",")
        writer.append(top.outstandingAmount.toString()).append("\n")
      }
    }
    return file
  }

  fun exportBusinessReportPdf(
    context: Context,
    businessProfile: BusinessProfile,
    periodLabel: String,
    salesReport: SalesReportData,
    khataReport: KhataReportData,
    stockReport: ProductStockReportData,
    customerReport: CustomerReportData
  ): File {
    val pdfDocument = PdfDocument()
    val timestamp = fileDateFormat.format(Date())
    val outputFile = File(getExportDir(context), "KhataGo_Business_Report_$timestamp.pdf")

    val pageWidth = 595
    val pageHeight = 842
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    // Header Background
    paint.color = Color.parseColor("#1B5E20") // Khata Green
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

    // Business Name & Title
    textPaint.color = Color.WHITE
    textPaint.textSize = 20f
    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(businessProfile.businessName, 36f, 38f, textPaint)

    textPaint.textSize = 12f
    textPaint.typeface = Typeface.DEFAULT
    canvas.drawText("Business & Financial Performance Report • Period: $periodLabel", 36f, 58f, textPaint)
    canvas.drawText("Owner: ${businessProfile.ownerName} | Tel: ${businessProfile.phone}", 36f, 76f, textPaint)

    var currentY = 120f

    fun drawSectionHeader(title: String) {
      paint.color = Color.parseColor("#E8F5E9")
      canvas.drawRoundRect(RectF(36f, currentY - 14f, 559f, currentY + 12f), 6f, 6f, paint)
      textPaint.color = Color.parseColor("#1B5E20")
      textPaint.textSize = 13f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText(title, 46f, currentY + 4f, textPaint)
      currentY += 30f
    }

    fun drawMetricRow(label1: String, val1: String, label2: String, val2: String) {
      textPaint.textSize = 10.5f
      textPaint.color = Color.parseColor("#555555")
      textPaint.typeface = Typeface.DEFAULT
      canvas.drawText(label1, 46f, currentY, textPaint)
      canvas.drawText(label2, 300f, currentY, textPaint)

      textPaint.color = Color.parseColor("#111111")
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText(val1, 190f, currentY, textPaint)
      canvas.drawText(val2, 450f, currentY, textPaint)
      currentY += 18f
    }

    // 1. Sales & Revenue
    drawSectionHeader("1. SALES & REVENUE")
    drawMetricRow(
      "Total Sales:", "Rs. ${indianCurrencyFormat.format(salesReport.totalSales.toInt())}",
      "Total Invoices:", "${salesReport.invoiceCount}"
    )
    drawMetricRow(
      "Paid / Collected:", "Rs. ${indianCurrencyFormat.format(salesReport.paidAmount.toInt())}",
      "Pending Balance:", "Rs. ${indianCurrencyFormat.format(salesReport.pendingAmount.toInt())}"
    )
    drawMetricRow(
      "Fully Paid Invoices:", "${salesReport.fullyPaidCount}",
      "Unpaid Invoices:", "${salesReport.unpaidCount}"
    )
    currentY += 10f

    // 2. Khata Ledger & Dues
    drawSectionHeader("2. KHATA (UDHAAR & JAMA) SUMMARY")
    drawMetricRow(
      "Total Credit Given:", "Rs. ${indianCurrencyFormat.format(khataReport.totalCredit.toInt())}",
      "Total Payments Received:", "Rs. ${indianCurrencyFormat.format(khataReport.totalPayments.toInt())}"
    )
    drawMetricRow(
      "Total Outstanding (Lena):", "Rs. ${indianCurrencyFormat.format(khataReport.totalOutstanding.toInt())}",
      "Customers with Dues:", "${khataReport.customerOutstandingList.size}"
    )
    currentY += 10f

    // 3. Products & Inventory
    drawSectionHeader("3. PRODUCTS & INVENTORY HEALTH")
    drawMetricRow(
      "Cataloged Products:", "${stockReport.totalProducts}",
      "Total Stock Units:", "${stockReport.totalStockUnits.toInt()}"
    )
    drawMetricRow(
      "Stock Valuation (Cost):", "Rs. ${indianCurrencyFormat.format(stockReport.totalStockValuation.toInt())}",
      "Retail Valuation:", "Rs. ${indianCurrencyFormat.format(stockReport.totalRetailValuation.toInt())}"
    )
    drawMetricRow(
      "Low Stock Alert Items:", "${stockReport.lowStockProducts.size}",
      "Out of Stock Items:", "${stockReport.outOfStockProducts.size}"
    )
    currentY += 10f

    // 4. Customer Performance
    drawSectionHeader("4. TOP CUSTOMERS SUMMARY")
    drawMetricRow(
      "Total Customers:", "${customerReport.totalCustomers}",
      "Pending Khata Count:", "${customerReport.customersWithBalanceCount}"
    )

    // Table of top customers
    val topList = customerReport.topCustomersByInvoice.take(5)
    if (topList.isNotEmpty()) {
      currentY += 6f
      paint.color = Color.parseColor("#F5F5F5")
      canvas.drawRect(36f, currentY - 10f, 559f, currentY + 12f, paint)

      textPaint.color = Color.parseColor("#333333")
      textPaint.textSize = 9.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("CUSTOMER", 46f, currentY + 4f, textPaint)
      canvas.drawText("PHONE", 210f, currentY + 4f, textPaint)
      canvas.drawText("INVOICES", 330f, currentY + 4f, textPaint)
      canvas.drawText("PURCHASE (RS)", 440f, currentY + 4f, textPaint)
      currentY += 20f

      textPaint.typeface = Typeface.DEFAULT
      for (cust in topList) {
        textPaint.color = Color.parseColor("#222222")
        canvas.drawText(cust.customerName.take(20), 46f, currentY, textPaint)
        canvas.drawText(cust.customerPhone, 210f, currentY, textPaint)
        canvas.drawText("${cust.invoiceCount}", 340f, currentY, textPaint)
        canvas.drawText("Rs. ${indianCurrencyFormat.format(cust.totalInvoiceAmount.toInt())}", 440f, currentY, textPaint)
        currentY += 16f
      }
    }

    // Footer
    paint.color = Color.LTGRAY
    canvas.drawLine(36f, pageHeight - 40f, 559f, pageHeight - 40f, paint)

    textPaint.color = Color.GRAY
    textPaint.textSize = 9f
    textPaint.typeface = Typeface.DEFAULT
    canvas.drawText("Generated by KhataGo on ${displayDateFormat.format(Date())} • Confidential Business Report", 36f, pageHeight - 25f, textPaint)

    pdfDocument.finishPage(page)

    FileOutputStream(outputFile).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    return outputFile
  }

  fun shareExportFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        putExtra(Intent.EXTRA_TEXT, "Exported file: ${file.name} from KhataGo")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, chooserTitle)
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
