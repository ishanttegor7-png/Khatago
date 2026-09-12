package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.InvoiceItemInput
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.ui.components.ProductPickerDialog
import com.example.ui.theme.DenaHaiBgLight
import com.example.ui.theme.DenaHaiRed
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import com.example.util.InvoiceCalculator
import java.text.NumberFormat
import java.util.Locale

data class ItemFormState(
  val id: String = java.util.UUID.randomUUID().toString(),
  var productId: String? = null,
  var productName: String = "",
  var quantity: Int = 1,
  var unitPriceText: String = "",
  var discountText: String = "0",
  var availableStock: Double? = null,
  var productUnit: String = "piece"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
  customerList: List<Customer>,
  productList: List<Product> = emptyList(),
  existingInvoice: Invoice? = null,
  onSaveInvoice: (
    customerId: String,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    items: List<InvoiceItemInput>,
    overallDiscount: Double,
    taxEnabled: Boolean,
    taxRate: Double,
    paidAmount: Double,
    notes: String,
    dueDate: String
  ) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val indianFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

  // Customer State
  var selectedCustomerId by remember {
    mutableStateOf(existingInvoice?.customerId ?: customerList.firstOrNull()?.id ?: "")
  }
  var customerName by remember {
    mutableStateOf(existingInvoice?.customerName ?: customerList.firstOrNull()?.name ?: "")
  }
  var customerPhone by remember {
    mutableStateOf(existingInvoice?.customerPhone ?: customerList.firstOrNull()?.phone ?: "")
  }
  var customerAddress by remember {
    mutableStateOf(existingInvoice?.customerAddress ?: customerList.firstOrNull()?.address ?: "")
  }
  var customerDropdownExpanded by remember { mutableStateOf(false) }

  // Multi-item Form List
  val items = remember {
    mutableStateListOf<ItemFormState>().apply {
      if (existingInvoice != null && existingInvoice.items.isNotEmpty()) {
        addAll(
          existingInvoice.items.map { item ->
            val matchingProduct = productList.find { it.id == item.productId }
            ItemFormState(
              id = item.id,
              productId = item.productId,
              productName = item.productName,
              quantity = item.quantity,
              unitPriceText = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString(),
              discountText = if (item.discount % 1.0 == 0.0) item.discount.toInt().toString() else item.discount.toString(),
              availableStock = matchingProduct?.currentStock,
              productUnit = matchingProduct?.unit ?: "piece"
            )
          }
        )
      } else {
        add(ItemFormState(productName = "", quantity = 1, unitPriceText = "", discountText = "0"))
      }
    }
  }

  var activePickerItemIndex by remember { mutableStateOf<Int?>(null) }

  // Overall Discount & Tax State
  var overallDiscountText by remember {
    mutableStateOf(
      if (existingInvoice != null && existingInvoice.discount > 0) {
        if (existingInvoice.discount % 1.0 == 0.0) existingInvoice.discount.toInt().toString() else existingInvoice.discount.toString()
      } else "0"
    )
  }

  var taxEnabled by remember { mutableStateOf(existingInvoice?.taxEnabled ?: false) }
  var taxRateText by remember {
    mutableStateOf(
      if (existingInvoice != null && existingInvoice.taxRate > 0) {
        if (existingInvoice.taxRate % 1.0 == 0.0) existingInvoice.taxRate.toInt().toString() else existingInvoice.taxRate.toString()
      } else "18"
    )
  }

  // Payment State
  var paidAmountText by remember {
    mutableStateOf(
      if (existingInvoice != null && existingInvoice.paidAmount > 0) {
        if (existingInvoice.paidAmount % 1.0 == 0.0) existingInvoice.paidAmount.toInt().toString() else existingInvoice.paidAmount.toString()
      } else "0"
    )
  }

  var dueDateText by remember { mutableStateOf(existingInvoice?.dueDate ?: "") }
  var notesText by remember { mutableStateOf(existingInvoice?.notes ?: "") }

  var validationError by remember { mutableStateOf<String?>(null) }

  // Live Calculations using decimal-safe InvoiceCalculator
  val overallDiscount = overallDiscountText.toDoubleOrNull() ?: 0.0
  val taxRate = taxRateText.toDoubleOrNull() ?: 0.0
  val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0

  val domainItemsPreview by remember(items.toList(), items.map { it.productName to it.unitPriceText to it.quantity }) {
    derivedStateOf {
      items.mapIndexed { idx, itm ->
        val price = itm.unitPriceText.toDoubleOrNull() ?: 0.0
        val disc = itm.discountText.toDoubleOrNull() ?: 0.0
        val itemTotal = InvoiceCalculator.calculateItemTotal(itm.quantity, price, disc)
        InvoiceItem(
          id = itm.id,
          invoiceId = "",
          productName = itm.productName,
          quantity = itm.quantity,
          unitPrice = price,
          discount = disc,
          itemTotal = itemTotal
        )
      }
    }
  }

  val calcResult by remember(domainItemsPreview, overallDiscount, taxEnabled, taxRate, paidAmount) {
    derivedStateOf {
      InvoiceCalculator.calculateInvoice(
        items = domainItemsPreview,
        overallDiscount = overallDiscount,
        taxEnabled = taxEnabled,
        taxRatePercent = taxRate,
        paidAmount = paidAmount
      )
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("create_invoice_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingInvoice != null) "Edit Invoice ${existingInvoice.invoiceNumber}" else "Create Invoice",
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onCancel, modifier = Modifier.testTag("create_invoice_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          if (validationError != null) {
            Text(
              text = validationError!!,
              color = MaterialTheme.colorScheme.error,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(bottom = 8.dp)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(
              onClick = onCancel,
              modifier = Modifier.weight(1f).height(50.dp).testTag("cancel_invoice_btn"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancel")
            }

            Button(
              onClick = {
                // Validation
                if (customerName.isBlank()) {
                  validationError = "Please enter or select a customer"
                  return@Button
                }
                if (items.isEmpty()) {
                  validationError = "Please add at least one item"
                  return@Button
                }
                for ((idx, itm) in items.withIndex()) {
                  if (itm.productName.isBlank()) {
                    validationError = "Item #${idx + 1} name cannot be empty"
                    return@Button
                  }
                  val p = itm.unitPriceText.toDoubleOrNull()
                  if (p == null || p < 0.0) {
                    validationError = "Enter valid price for item #${idx + 1}"
                    return@Button
                  }
                  if (itm.quantity <= 0) {
                    validationError = "Quantity for item #${idx + 1} must be at least 1"
                    return@Button
                  }
                  // Stock limit check for newly linked catalog products
                  if (existingInvoice == null && itm.productId != null && itm.availableStock != null) {
                    if (itm.quantity > itm.availableStock!!) {
                      validationError = "${itm.productName}: Quantity (${itm.quantity}) exceeds available stock (${itm.availableStock?.toInt()} ${itm.productUnit})"
                      return@Button
                    }
                  }
                }

                val itemInputs = items.map { itm ->
                  InvoiceItemInput(
                    productName = itm.productName.trim(),
                    quantity = itm.quantity,
                    unitPrice = itm.unitPriceText.toDoubleOrNull() ?: 0.0,
                    discount = itm.discountText.toDoubleOrNull() ?: 0.0,
                    taxRate = if (taxEnabled) taxRate else 0.0,
                    productId = itm.productId
                  )
                }

                val custId = if (selectedCustomerId.isNotBlank()) selectedCustomerId else "c_custom"
                validationError = null
                onSaveInvoice(
                  custId,
                  customerName.trim(),
                  customerPhone.trim(),
                  customerAddress.trim(),
                  itemInputs,
                  overallDiscount,
                  taxEnabled,
                  taxRate,
                  paidAmount,
                  notesText.trim(),
                  dueDateText.trim()
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
              modifier = Modifier.weight(1.3f).height(50.dp).testTag("save_invoice_btn"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = if (existingInvoice != null) "Update Invoice" else "Save & Create Invoice",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Section 1: Customer Selection
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier.size(32.dp).clip(CircleShape).background(KhataGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Person, contentDescription = null, tint = KhataGreenPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Customer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Select from existing customers dropdown
          if (customerList.isNotEmpty()) {
            ExposedDropdownMenuBox(
              expanded = customerDropdownExpanded,
              onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = customerName,
                onValueChange = {
                  customerName = it
                  selectedCustomerId = ""
                },
                label = { Text("Customer Name *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = KhataGreenPrimary,
                  focusedLabelColor = KhataGreenPrimary
                ),
                modifier = Modifier.fillMaxWidth().menuAnchor().testTag("invoice_customer_name_input")
              )

              ExposedDropdownMenu(
                expanded = customerDropdownExpanded,
                onDismissRequest = { customerDropdownExpanded = false }
              ) {
                customerList.forEach { cust ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(cust.name, fontWeight = FontWeight.Bold)
                        if (cust.phone.isNotBlank()) {
                          Text(cust.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      }
                    },
                    onClick = {
                      selectedCustomerId = cust.id
                      customerName = cust.name
                      customerPhone = cust.phone
                      customerAddress = cust.address
                      customerDropdownExpanded = false
                    }
                  )
                }
              }
            }
          } else {
            OutlinedTextField(
              value = customerName,
              onValueChange = { customerName = it },
              label = { Text("Customer Name *") },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("invoice_customer_name_input")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customerPhone,
              onValueChange = { customerPhone = it },
              label = { Text("Mobile Number (Optional)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("invoice_customer_phone_input")
            )

            OutlinedTextField(
              value = customerAddress,
              onValueChange = { customerAddress = it },
              label = { Text("Address (Optional)") },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("invoice_customer_address_input")
            )
          }
        }
      }

      // Section 2: Items & Services
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(KhataAmberBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = KhataAmber, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text("Invoice Items (${items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            TextButton(
              onClick = {
                items.add(ItemFormState(productName = "", quantity = 1, unitPriceText = "", discountText = "0"))
              },
              modifier = Modifier.testTag("add_item_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = KhataGreenPrimary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Item", color = KhataGreenPrimary, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          items.forEachIndexed { index, itemState ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Item #${index + 1}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (productList.isNotEmpty()) {
                      Spacer(modifier = Modifier.width(8.dp))
                      TextButton(
                        onClick = { activePickerItemIndex = index },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp).testTag("btn_select_product_${index + 1}")
                      ) {
                        Text("📋 Select Product", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KhataGreenPrimary)
                      }
                    }
                  }

                  if (items.size > 1) {
                    IconButton(
                      onClick = { items.removeAt(index) },
                      modifier = Modifier.size(28.dp).testTag("delete_item_${index + 1}")
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                  value = itemState.productName,
                  onValueChange = {
                    items[index] = itemState.copy(productName = it, productId = null, availableStock = null)
                  },
                  label = { Text("Product / Service Name *") },
                  placeholder = { Text("e.g. Atta 10kg / Mobile Service") },
                  shape = RoundedCornerShape(8.dp),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("item_name_input_${index + 1}")
                )

                if (itemState.productId != null && itemState.availableStock != null) {
                  val stock = itemState.availableStock!!
                  val isExceeded = itemState.quantity > stock
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = if (isExceeded) "⚠️ Alert: Only $stock ${itemState.productUnit} in stock!" else "📦 Available Stock: $stock ${itemState.productUnit}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isExceeded) DenaHaiRed else LenaHaiGreen
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Quantity with +/- buttons
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.1f)
                  ) {
                    IconButton(
                      onClick = {
                        if (itemState.quantity > 1) {
                          items[index] = itemState.copy(quantity = itemState.quantity - 1)
                        }
                      },
                      modifier = Modifier.size(32.dp).testTag("qty_decrease_${index + 1}")
                    ) {
                      Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(
                      text = "${itemState.quantity}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      modifier = Modifier.padding(horizontal = 4.dp).testTag("qty_text_${index + 1}")
                    )
                    IconButton(
                      onClick = {
                        items[index] = itemState.copy(quantity = itemState.quantity + 1)
                      },
                      modifier = Modifier.size(32.dp).testTag("qty_increase_${index + 1}")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                  }

                  // Price
                  OutlinedTextField(
                    value = itemState.unitPriceText,
                    onValueChange = {
                      items[index] = itemState.copy(unitPriceText = it)
                    },
                    label = { Text("Price (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("item_price_input_${index + 1}")
                  )

                  // Item Discount
                  OutlinedTextField(
                    value = itemState.discountText,
                    onValueChange = {
                      items[index] = itemState.copy(discountText = it)
                    },
                    label = { Text("Disc (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f).testTag("item_disc_input_${index + 1}")
                  )
                }

                // Live row total
                val q = itemState.quantity
                val p = itemState.unitPriceText.toDoubleOrNull() ?: 0.0
                val d = itemState.discountText.toDoubleOrNull() ?: 0.0
                val rowTotal = InvoiceCalculator.calculateItemTotal(q, p, d)

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                  Text(
                    text = "Total: ₹${indianFormat.format(rowTotal.toInt())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KhataGreenPrimary
                  )
                }
              }
            }
          }
        }
      }

      // Section 3: Discounts & Optional GST / Tax
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text("Discounts & Tax (GST)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          // Overall Discount
          OutlinedTextField(
            value = overallDiscountText,
            onValueChange = { overallDiscountText = it },
            label = { Text("Special Invoice Discount (₹)") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("overall_discount_input")
          )

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          Spacer(modifier = Modifier.height(10.dp))

          // Tax Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Apply GST / Tax", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("GST is optional for small businesses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = taxEnabled,
              onCheckedChange = { taxEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = KhataGreenPrimary, checkedTrackColor = KhataGreenContainer),
              modifier = Modifier.testTag("tax_switch")
            )
          }

          AnimatedVisibility(visible = taxEnabled) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
              Text("Select GST Rate:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("0", "5", "12", "18", "28").forEach { rate ->
                  val isSelected = taxRateText == rate
                  FilterChip(
                    selected = isSelected,
                    onClick = { taxRateText = rate },
                    label = { Text("$rate%", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = KhataGreenContainer,
                      selectedLabelColor = KhataGreenPrimary
                    ),
                    modifier = Modifier.testTag("tax_chip_$rate")
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              OutlinedTextField(
                value = taxRateText,
                onValueChange = { taxRateText = it },
                label = { Text("Custom Tax Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("custom_tax_input")
              )
            }
          }
        }
      }

      // Section 4: Payment Details
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text("Payment & Due Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = paidAmountText,
            onValueChange = { paidAmountText = it },
            label = { Text("Amount Paid Upfront (₹)") },
            placeholder = { Text("0 for Unpaid / Pending") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("invoice_paid_amount_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { paidAmountText = "0" },
              modifier = Modifier.weight(1f).testTag("quick_unpaid_btn"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Mark Unpaid (₹0)", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = {
                paidAmountText = if (calcResult.grandTotal % 1.0 == 0.0) {
                  calcResult.grandTotal.toInt().toString()
                } else {
                  "%.2f".format(calcResult.grandTotal)
                }
              },
              modifier = Modifier.weight(1f).testTag("quick_paid_full_btn"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Mark Paid Full", fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = dueDateText,
            onValueChange = { dueDateText = it },
            label = { Text("Due Date (Optional)") },
            placeholder = { Text("e.g. 15 Oct 2026") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("invoice_due_date_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("Notes / Terms (Optional)") },
            placeholder = { Text("e.g. Goods once sold will not be returned") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("invoice_notes_input")
          )
        }
      }

      // Section 5: Real-time Calculation Breakdown Summary
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("invoice_calc_summary")
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text("Bill Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${indianFormat.format(calcResult.subtotal.toInt())}", fontWeight = FontWeight.Medium)
          }

          if (calcResult.discount > 0.0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Discount", color = DenaHaiRed)
              Text("- ₹${indianFormat.format(calcResult.discount.toInt())}", color = DenaHaiRed)
            }
          }

          if (taxEnabled && calcResult.taxAmount > 0.0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("GST (${calcResult.taxRate.toInt()}%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("+ ₹${indianFormat.format(calcResult.taxAmount.toInt())}")
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = KhataGreenContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Grand Total", fontWeight = FontWeight.Bold, color = KhataGreenPrimary)
              Text(
                "₹${indianFormat.format(calcResult.grandTotal.toInt())}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KhataGreenPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Paid Amount", color = LenaHaiGreen, fontWeight = FontWeight.SemiBold)
            Text("₹${indianFormat.format(calcResult.paidAmount.toInt())}", color = LenaHaiGreen, fontWeight = FontWeight.Bold)
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Balance Due", color = if (calcResult.remainingAmount > 0) DenaHaiRed else LenaHaiGreen, fontWeight = FontWeight.SemiBold)
            Text(
              "₹${indianFormat.format(calcResult.remainingAmount.toInt())}",
              fontWeight = FontWeight.Bold,
              color = if (calcResult.remainingAmount > 0) DenaHaiRed else LenaHaiGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (activePickerItemIndex != null) {
    ProductPickerDialog(
      products = productList,
      onDismiss = { activePickerItemIndex = null },
      onProductSelected = { prod ->
        val idx = activePickerItemIndex
        if (idx != null && idx in items.indices) {
          items[idx] = items[idx].copy(
            productId = prod.id,
            productName = prod.name,
            unitPriceText = if (prod.sellingPrice % 1.0 == 0.0) prod.sellingPrice.toInt().toString() else prod.sellingPrice.toString(),
            availableStock = prod.currentStock,
            productUnit = prod.unit
          )
        }
        activePickerItemIndex = null
      }
    )
  }
}
