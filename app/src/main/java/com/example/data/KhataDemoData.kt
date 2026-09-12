package com.example.data

import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoicePaymentStatus
import com.example.model.Transaction
import com.example.model.TransactionType

object KhataDemoData {
  val defaultProfile = BusinessProfile(
    businessName = "Sharma Kirana & General Store",
    ownerName = "Rajesh Sharma",
    phone = "9876543210",
    address = "Shop No. 12, Main Market, Jaipur, Rajasthan",
    upiId = "sharmakirana@upi"
  )

  val initialBusinessProfile = defaultProfile

  val initialCustomers = listOf(
    Customer(
      id = "1",
      name = "Ramesh Kumar",
      phone = "9829012345",
      address = "Plot 42, Civil Lines, Jaipur",
      balance = 2500.0,
      lastUpdated = "08 Sep 2026"
    ),
    Customer(
      id = "2",
      name = "Priya Gupta",
      phone = "9828776655",
      address = "Sector 3, Malviya Nagar",
      balance = 1200.0,
      lastUpdated = "07 Sep 2026"
    ),
    Customer(
      id = "3",
      name = "Deepak Sharma",
      phone = "9460012345",
      address = "Station Road, Jaipur",
      balance = -800.0,
      lastUpdated = "05 Sep 2026"
    ),
    Customer(
      id = "4",
      name = "Sunita Verma",
      phone = "9785123456",
      address = "Vaishali Nagar, Jaipur",
      balance = 4500.0,
      lastUpdated = "04 Sep 2026"
    ),
    Customer(
      id = "5",
      name = "Vikram Singh",
      phone = "9928076543",
      address = "Raja Park, Jaipur",
      balance = 3100.0,
      lastUpdated = "02 Sep 2026"
    ),
    Customer(
      id = "6",
      name = "Anil Tailor",
      phone = "9636223344",
      address = "Purani Basti, Jaipur",
      balance = 0.0,
      lastUpdated = "28 Aug 2026"
    ),
    Customer(
      id = "7",
      name = "Meena Devi",
      phone = "9829988776",
      address = "Mansarovar, Jaipur",
      balance = 950.0,
      lastUpdated = "25 Aug 2026"
    )
  )

  val initialTransactions = listOf(
    Transaction(
      id = "tx1",
      customerId = "1",
      customerName = "Ramesh Kumar",
      type = TransactionType.LENA_HAI,
      amount = 2500.0,
      note = "Monthly ration (atta, rice, dal, oil)",
      date = "08 Sep 2026"
    ),
    Transaction(
      id = "tx2",
      customerId = "2",
      customerName = "Priya Gupta",
      type = TransactionType.LENA_HAI,
      amount = 1200.0,
      note = "Spices, ghee, dry fruits",
      date = "07 Sep 2026"
    ),
    Transaction(
      id = "tx3",
      customerId = "3",
      customerName = "Deepak Sharma",
      type = TransactionType.DENA_HAI,
      amount = 800.0,
      note = "Milk supply weekly payment balance",
      date = "05 Sep 2026"
    ),
    Transaction(
      id = "tx4",
      customerId = "4",
      customerName = "Sunita Verma",
      type = TransactionType.LENA_HAI,
      amount = 3000.0,
      note = "Festive grocery items on credit",
      date = "04 Sep 2026"
    ),
    Transaction(
      id = "tx5",
      customerId = "4",
      customerName = "Sunita Verma",
      type = TransactionType.LENA_HAI,
      amount = 1500.0,
      note = "Cleaning supplies & tea packets",
      date = "03 Sep 2026"
    ),
    Transaction(
      id = "tx6",
      customerId = "5",
      customerName = "Vikram Singh",
      type = TransactionType.LENA_HAI,
      amount = 3100.0,
      note = "Stationery and general store items",
      date = "02 Sep 2026"
    ),
    Transaction(
      id = "tx7",
      customerId = "1",
      customerName = "Ramesh Kumar",
      type = TransactionType.DENA_HAI,
      amount = 1500.0,
      note = "Advance received for upcoming order",
      date = "01 Sep 2026"
    )
  )

  val initialInvoices = listOf(
    Invoice(
      id = "inv1",
      invoiceNumber = "INV-1001",
      customerId = "1",
      customerName = "Ramesh Kumar",
      customerPhone = "9829012345",
      customerAddress = "Plot 42, Civil Lines, Jaipur",
      businessName = "Sharma Kirana & General Store",
      businessPhone = "9876543210",
      businessAddress = "Shop No. 12, Main Market, Jaipur, Rajasthan",
      businessUpi = "sharmakirana@upi",
      invoiceDate = "09 Sep 2026",
      dueDate = "16 Sep 2026",
      items = listOf(
        InvoiceItem(
          id = "item1",
          invoiceId = "inv1",
          productName = "Basmati Rice 5kg",
          quantity = 2,
          unitPrice = 450.0,
          discount = 0.0,
          itemTotal = 900.0
        ),
        InvoiceItem(
          id = "item2",
          invoiceId = "inv1",
          productName = "Mustard Oil 2L",
          quantity = 2,
          unitPrice = 220.0,
          discount = 0.0,
          itemTotal = 440.0
        )
      ),
      subtotal = 1340.0,
      discount = 40.0,
      taxEnabled = true,
      taxRate = 5.0,
      taxAmount = 65.0,
      grandTotal = 1365.0,
      paidAmount = 500.0,
      remainingAmount = 865.0,
      paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID,
      notes = "Thank you for your business!",
      createdTimestamp = 1725849600000L,
      updatedTimestamp = 1725849600000L
    ),
    Invoice(
      id = "inv2",
      invoiceNumber = "INV-1002",
      customerId = "2",
      customerName = "Priya Gupta",
      customerPhone = "9828776655",
      customerAddress = "Sector 3, Malviya Nagar",
      businessName = "Sharma Kirana & General Store",
      businessPhone = "9876543210",
      businessAddress = "Shop No. 12, Main Market, Jaipur, Rajasthan",
      businessUpi = "sharmakirana@upi",
      invoiceDate = "08 Sep 2026",
      dueDate = "",
      items = listOf(
        InvoiceItem(
          id = "item3",
          invoiceId = "inv2",
          productName = "Aashirvaad Atta 10kg",
          quantity = 3,
          unitPrice = 420.0,
          discount = 0.0,
          itemTotal = 1260.0
        ),
        InvoiceItem(
          id = "item4",
          invoiceId = "inv2",
          productName = "Organic Turmeric Powder 500g",
          quantity = 2,
          unitPrice = 120.0,
          discount = 0.0,
          itemTotal = 240.0
        )
      ),
      subtotal = 1500.0,
      discount = 0.0,
      taxEnabled = false,
      taxRate = 0.0,
      taxAmount = 0.0,
      grandTotal = 1500.0,
      paidAmount = 1500.0,
      remainingAmount = 0.0,
      paymentStatus = InvoicePaymentStatus.PAID,
      notes = "Paid via UPI",
      createdTimestamp = 1725763200000L,
      updatedTimestamp = 1725763200000L
    )
  )
}
