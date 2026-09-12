package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.Invoice
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object InvoiceShareHelper {

  fun shareInvoicePdf(context: Context, invoice: Invoice) {
    try {
      val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Invoice #${invoice.invoiceNumber} - ${invoice.businessName}")
        val summaryText = "Invoice #${invoice.invoiceNumber}\n" +
          "Customer: ${invoice.customerName}\n" +
          "Grand Total: ₹${invoice.grandTotal}\n" +
          "Balance Due: ₹${invoice.remainingAmount}\n" +
          "Status: ${invoice.paymentStatus.displayName}"
        putExtra(Intent.EXTRA_TEXT, summaryText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to share invoice: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun printInvoicePdf(context: Context, invoice: Invoice) {
    try {
      val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      if (printManager == null) {
        Toast.makeText(context, "Printing is not supported on this device", Toast.LENGTH_SHORT).show()
        return
      }

      val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info = PrintDocumentInfo.Builder("Invoice_${invoice.invoiceNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
          callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?
        ) {
          if (destination == null) {
            callback?.onWriteFailed("Missing destination")
            return
          }
          try {
            FileInputStream(pdfFile).use { input ->
              FileOutputStream(destination.fileDescriptor).use { output ->
                input.copyTo(output)
              }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          }
        }
      }

      printManager.print("Invoice_${invoice.invoiceNumber}", adapter, PrintAttributes.Builder().build())
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to print invoice: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun saveInvoicePdfLocally(context: Context, invoice: Invoice): String? {
    return try {
      val generated = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
      val docsDir = context.getExternalFilesDir("Invoices") ?: context.filesDir
      val target = File(docsDir, "Invoice_${invoice.invoiceNumber}.pdf")
      generated.copyTo(target, overwrite = true)
      target.absolutePath
    } catch (e: Exception) {
      null
    }
  }
}
