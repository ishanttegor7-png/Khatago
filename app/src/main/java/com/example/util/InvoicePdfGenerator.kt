package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoicePaymentStatus
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object InvoicePdfGenerator {

  // A4 dimensions in PostScript points (72 points per inch)
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842

  private const val MARGIN_LEFT = 36f
  private const val MARGIN_RIGHT = 559f
  private const val MARGIN_TOP = 36f
  private const val MARGIN_BOTTOM = 806f

  private val indianCurrencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
  }

  private fun formatAmount(amount: Double): String {
    return "Rs. " + indianCurrencyFormat.format(amount)
  }

  /**
   * Generates a clean A4 PDF invoice and returns the File in the app's cache dir.
   */
  fun generateInvoicePdf(context: Context, invoice: Invoice): File {
    val pdfDocument = PdfDocument()
    val cacheDir = File(context.cacheDir, "invoices").apply { mkdirs() }
    val sanitizedNum = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    val outputFile = File(cacheDir, "Invoice_${sanitizedNum}.pdf")

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    val items = invoice.items
    val totalItems = items.size
    var currentItemIndex = 0
    var pageNumber = 1

    // Estimate pages or dynamically create pages
    while (currentItemIndex < totalItems || pageNumber == 1) {
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas = page.canvas

      var cursorY = MARGIN_TOP

      // 1. Header (Business details on left, Invoice metadata on right)
      cursorY = drawHeader(canvas, paint, textPaint, invoice, pageNumber, cursorY)

      // 2. Bill To (Customer Details) - only on page 1 or compact on subsequent
      if (pageNumber == 1) {
        cursorY = drawBillTo(canvas, paint, textPaint, invoice, cursorY)
      }

      // 3. Table Header
      cursorY = drawTableHeader(canvas, paint, cursorY)

      // 4. Table Rows
      val startItemIndex = currentItemIndex
      val rowsBottomLimit = if (currentItemIndex + 4 >= totalItems) 620f else 750f

      while (currentItemIndex < totalItems && cursorY < rowsBottomLimit) {
        val item = items[currentItemIndex]
        val rowHeight = calculateRowHeight(item)
        if (cursorY + rowHeight > rowsBottomLimit && currentItemIndex > startItemIndex) {
          // Break to next page
          break
        }
        drawTableRow(canvas, paint, textPaint, item, currentItemIndex + 1, cursorY, rowHeight)
        cursorY += rowHeight
        currentItemIndex++
      }

      // 5. If all items rendered on this page, draw Summary Totals & Notes
      if (currentItemIndex >= totalItems) {
        cursorY = drawSummaryAndTotals(canvas, paint, invoice, cursorY)
      }

      // 6. Footer (Invoice # & Page Number)
      drawFooter(canvas, paint, invoice.invoiceNumber, pageNumber)

      pdfDocument.finishPage(page)

      if (currentItemIndex >= totalItems) {
        break
      }
      pageNumber++
    }

    FileOutputStream(outputFile).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    return outputFile
  }

  private fun drawHeader(
    canvas: Canvas,
    paint: Paint,
    textPaint: TextPaint,
    invoice: Invoice,
    pageNumber: Int,
    startY: Float
  ): Float {
    var y = startY

    // Business Name
    paint.color = Color.rgb(15, 118, 110) // Khata Green Teal
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 20f
    val bName = invoice.businessName.ifBlank { "KhataGo Merchant" }
    canvas.drawText(bName, MARGIN_LEFT, y + 18f, paint)

    // Invoice Title on top right
    paint.color = Color.rgb(30, 41, 59)
    paint.textSize = 16f
    paint.textAlign = Paint.Align.RIGHT
    val title = if (invoice.taxEnabled) "TAX INVOICE" else "INVOICE"
    canvas.drawText(title, MARGIN_RIGHT, y + 16f, paint)

    paint.textAlign = Paint.Align.LEFT
    y += 24f

    // Business Phone & Address
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 9.5f
    paint.color = Color.rgb(71, 85, 105)

    if (invoice.businessPhone.isNotBlank()) {
      canvas.drawText("Phone: ${invoice.businessPhone}", MARGIN_LEFT, y + 10f, paint)
      y += 12f
    }
    if (invoice.businessAddress.isNotBlank()) {
      val addr = if (invoice.businessAddress.length > 45) invoice.businessAddress.take(45) + "..." else invoice.businessAddress
      canvas.drawText(addr, MARGIN_LEFT, y + 10f, paint)
      y += 12f
    }
    if (invoice.businessUpi.isNotBlank()) {
      canvas.drawText("UPI: ${invoice.businessUpi}", MARGIN_LEFT, y + 10f, paint)
      y += 12f
    }

    // Right side: Invoice # and Date
    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = 10f
    paint.color = Color.rgb(30, 41, 59)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Invoice #: ${invoice.invoiceNumber}", MARGIN_RIGHT, startY + 34f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Date: ${invoice.invoiceDate}", MARGIN_RIGHT, startY + 47f, paint)

    if (invoice.dueDate.isNotBlank()) {
      canvas.drawText("Due Date: ${invoice.dueDate}", MARGIN_RIGHT, startY + 60f, paint)
    }

    // Payment Status badge on top right
    val statusColor = when (invoice.paymentStatus) {
      InvoicePaymentStatus.PAID -> Color.rgb(16, 185, 129)
      InvoicePaymentStatus.PARTIALLY_PAID -> Color.rgb(217, 119, 6)
      InvoicePaymentStatus.PENDING -> Color.rgb(220, 38, 38)
      InvoicePaymentStatus.CANCELLED -> Color.rgb(100, 116, 139)
    }
    paint.color = statusColor
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Status: ${invoice.paymentStatus.displayName.uppercase()}", MARGIN_RIGHT, startY + 74f, paint)

    paint.textAlign = Paint.Align.LEFT
    y = maxOf(y + 8f, startY + 80f)

    // Divider line
    paint.color = Color.rgb(226, 232, 240)
    paint.strokeWidth = 1.5f
    canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paint)

    return y + 10f
  }

  private fun drawBillTo(
    canvas: Canvas,
    paint: Paint,
    textPaint: TextPaint,
    invoice: Invoice,
    startY: Float
  ): Float {
    var y = startY

    // Bill To Box
    paint.color = Color.rgb(248, 250, 252)
    val rect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 48f)
    canvas.drawRoundRect(rect, 6f, 6f, paint)

    paint.color = Color.rgb(226, 232, 240)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRoundRect(rect, 6f, 6f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(100, 116, 139)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("BILL TO (CUSTOMER)", MARGIN_LEFT + 10f, y + 14f, paint)

    paint.color = Color.rgb(15, 23, 42)
    paint.textSize = 11f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val custName = invoice.customerName.ifBlank { "Walk-in Customer" }
    canvas.drawText(custName, MARGIN_LEFT + 10f, y + 28f, paint)

    paint.color = Color.rgb(71, 85, 105)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val phonePart = if (invoice.customerPhone.isNotBlank()) "Phone: ${invoice.customerPhone}" else ""
    val addrPart = if (invoice.customerAddress.isNotBlank()) " | ${invoice.customerAddress}" else ""
    val details = "$phonePart$addrPart".trim().removePrefix("|").trim()
    if (details.isNotBlank()) {
      canvas.drawText(details, MARGIN_LEFT + 10f, y + 40f, paint)
    }

    return y + 56f
  }

  private fun drawTableHeader(canvas: Canvas, paint: Paint, startY: Float): Float {
    val headerHeight = 22f

    // Header Background
    paint.color = Color.rgb(241, 245, 249)
    val rect = RectF(MARGIN_LEFT, startY, MARGIN_RIGHT, startY + headerHeight)
    canvas.drawRoundRect(rect, 4f, 4f, paint)

    paint.color = Color.rgb(51, 65, 85)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    // Columns:
    // S.No: 36..65
    // Item: 70..320
    // Qty: 325..375
    // Rate: 380..450
    // Amount: 455..559
    canvas.drawText("#", MARGIN_LEFT + 8f, startY + 15f, paint)
    canvas.drawText("ITEM DESCRIPTION", MARGIN_LEFT + 34f, startY + 15f, paint)

    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("QTY", 355f, startY + 15f, paint)

    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("RATE", 445f, startY + 15f, paint)
    canvas.drawText("AMOUNT", MARGIN_RIGHT - 10f, startY + 15f, paint)
    paint.textAlign = Paint.Align.LEFT

    return startY + headerHeight + 4f
  }

  private fun calculateRowHeight(item: InvoiceItem): Float {
    // If item name is very long, allow 2 lines
    return if (item.productName.length > 40) 32f else 22f
  }

  private fun drawTableRow(
    canvas: Canvas,
    paint: Paint,
    textPaint: TextPaint,
    item: InvoiceItem,
    index: Int,
    startY: Float,
    rowHeight: Float
  ) {
    paint.color = Color.rgb(248, 250, 252)
    if (index % 2 == 0) {
      canvas.drawRect(MARGIN_LEFT, startY, MARGIN_RIGHT, startY + rowHeight, paint)
    }

    // Row bottom border
    paint.color = Color.rgb(241, 245, 249)
    paint.strokeWidth = 0.8f
    canvas.drawLine(MARGIN_LEFT, startY + rowHeight, MARGIN_RIGHT, startY + rowHeight, paint)

    paint.color = Color.rgb(51, 65, 85)
    paint.textSize = 9.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    // Index
    canvas.drawText("$index", MARGIN_LEFT + 8f, startY + 14f, paint)

    // Item Name with wrapping if long
    val maxTextWidth = 250
    if (item.productName.length > 40) {
      textPaint.color = Color.rgb(15, 23, 42)
      textPaint.textSize = 9.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      val staticLayout = StaticLayout.Builder.obtain(
        item.productName,
        0,
        item.productName.length,
        textPaint,
        maxTextWidth
      ).setMaxLines(2).build()

      canvas.save()
      canvas.translate(MARGIN_LEFT + 34f, startY + 4f)
      staticLayout.draw(canvas)
      canvas.restore()
    } else {
      canvas.drawText(item.productName, MARGIN_LEFT + 34f, startY + 14f, paint)
    }

    // Qty
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("${item.quantity}", 355f, startY + 14f, paint)

    // Rate
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(formatAmount(item.unitPrice), 445f, startY + 14f, paint)

    // Item Total
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(formatAmount(item.itemTotal), MARGIN_RIGHT - 10f, startY + 14f, paint)
    paint.textAlign = Paint.Align.LEFT
  }

  private fun drawSummaryAndTotals(
    canvas: Canvas,
    paint: Paint,
    invoice: Invoice,
    startY: Float
  ): Float {
    var y = startY + 12f

    val summaryBoxLeft = 320f
    val summaryBoxRight = MARGIN_RIGHT

    // Notes on Left
    if (invoice.notes.isNotBlank()) {
      paint.color = Color.rgb(100, 116, 139)
      paint.textSize = 8.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("NOTES / REMARKS:", MARGIN_LEFT, y + 10f, paint)

      paint.color = Color.rgb(51, 65, 85)
      paint.textSize = 9f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      val notesText = if (invoice.notes.length > 100) invoice.notes.take(100) + "..." else invoice.notes
      canvas.drawText(notesText, MARGIN_LEFT, y + 24f, paint)
    }

    // Subtotal
    paint.textSize = 9.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.rgb(71, 85, 105)
    canvas.drawText("Subtotal:", summaryBoxLeft, y + 10f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(formatAmount(invoice.subtotal), summaryBoxRight - 10f, y + 10f, paint)
    paint.textAlign = Paint.Align.LEFT
    y += 16f

    // Discount (if > 0)
    if (invoice.discount > 0.0) {
      paint.color = Color.rgb(220, 38, 38)
      canvas.drawText("Discount:", summaryBoxLeft, y + 10f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("- " + formatAmount(invoice.discount), summaryBoxRight - 10f, y + 10f, paint)
      paint.textAlign = Paint.Align.LEFT
      y += 16f
    }

    // Tax / GST (if enabled)
    if (invoice.taxEnabled && invoice.taxAmount > 0.0) {
      paint.color = Color.rgb(71, 85, 105)
      val taxLabel = if (invoice.taxRate > 0.0) "GST (${invoice.taxRate.toInt()}%):" else "Tax:"
      canvas.drawText(taxLabel, summaryBoxLeft, y + 10f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("+ " + formatAmount(invoice.taxAmount), summaryBoxRight - 10f, y + 10f, paint)
      paint.textAlign = Paint.Align.LEFT
      y += 16f
    }

    // Grand Total Highlight Box
    y += 4f
    paint.color = Color.rgb(15, 118, 110)
    val totalBox = RectF(summaryBoxLeft - 6f, y, summaryBoxRight, y + 26f)
    canvas.drawRoundRect(totalBox, 6f, 6f, paint)

    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("GRAND TOTAL:", summaryBoxLeft + 6f, y + 17f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(formatAmount(invoice.grandTotal), summaryBoxRight - 10f, y + 17f, paint)
    paint.textAlign = Paint.Align.LEFT
    y += 32f

    // Paid & Balance Due
    paint.textSize = 9.5f
    paint.color = Color.rgb(16, 185, 129) // Green
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Amount Paid:", summaryBoxLeft, y + 10f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(formatAmount(invoice.paidAmount), summaryBoxRight - 10f, y + 10f, paint)
    paint.textAlign = Paint.Align.LEFT
    y += 16f

    val balanceColor = if (invoice.remainingAmount > 0.0) Color.rgb(220, 38, 38) else Color.rgb(16, 185, 129)
    paint.color = balanceColor
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Balance Due:", summaryBoxLeft, y + 10f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(formatAmount(invoice.remainingAmount), summaryBoxRight - 10f, y + 10f, paint)
    paint.textAlign = Paint.Align.LEFT

    return y + 24f
  }

  private fun drawFooter(canvas: Canvas, paint: Paint, invoiceNumber: String, pageNumber: Int) {
    paint.color = Color.rgb(226, 232, 240)
    paint.strokeWidth = 1f
    canvas.drawLine(MARGIN_LEFT, MARGIN_BOTTOM - 20f, MARGIN_RIGHT, MARGIN_BOTTOM - 20f, paint)

    paint.color = Color.rgb(148, 163, 184)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Generated by KhataGo - Digital Hisaab & Invoicing", MARGIN_LEFT, MARGIN_BOTTOM - 6f, paint)

    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Invoice #$invoiceNumber  |  Page $pageNumber", MARGIN_RIGHT, MARGIN_BOTTOM - 6f, paint)
    paint.textAlign = Paint.Align.LEFT
  }
}
