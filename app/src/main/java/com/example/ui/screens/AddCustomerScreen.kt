package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary

@Composable
fun AddCustomerScreen(
  onSaveCustomer: (name: String, phone: String, address: String, onError: (String) -> Unit) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }

  var nameError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var generalError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("add_customer_screen"),
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onCancel,
          modifier = Modifier.testTag("btn_back_add_customer")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "Add New Customer",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
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
      // Info card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(KhataGreenContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              tint = KhataGreenPrimary,
              modifier = Modifier.size(28.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Customer Details",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Save contact to track Udhaar & send payment reminders",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // General backend error if any
      if (generalError != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth().testTag("add_customer_general_error")
        ) {
          Text(
            text = generalError!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Fields Container
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Customer Name (Required)
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              if (it.isNotBlank()) {
                nameError = null
                generalError = null
              }
            },
            label = { Text("Customer Name *") },
            placeholder = { Text("e.g. Ramesh Kumar, Sharma Ji") },
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = null, tint = KhataGreenPrimary)
            },
            isError = nameError != null,
            supportingText = {
              if (nameError != null) {
                Text(text = nameError!!, color = MaterialTheme.colorScheme.error)
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_customer_name")
          )

          // Mobile Number (Optional)
          OutlinedTextField(
            value = phone,
            onValueChange = {
              if (it.all { ch -> ch.isDigit() } && it.length <= 10) {
                phone = it
                phoneError = null
                generalError = null
              }
            },
            label = { Text("Mobile Number (Optional)") },
            placeholder = { Text("10-digit mobile number") },
            leadingIcon = {
              Icon(Icons.Default.Phone, contentDescription = null, tint = KhataGreenPrimary)
            },
            prefix = { if (phone.isNotEmpty()) Text("+91 ", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError != null,
            supportingText = {
              if (phoneError != null) {
                Text(text = phoneError!!, color = MaterialTheme.colorScheme.error)
              } else if (phone.isNotEmpty()) {
                Text(text = "${phone.length}/10 digits", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_customer_phone")
          )

          // Optional Address
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address / Shop Location (Optional)") },
            placeholder = { Text("e.g. Shop No. 4, Main Bazar") },
            leadingIcon = {
              Icon(Icons.Default.Home, contentDescription = null, tint = KhataGreenPrimary)
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_customer_address")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Action Buttons
      Button(
        onClick = {
          var hasError = false
          if (name.trim().isBlank()) {
            nameError = "Customer name is required"
            hasError = true
          }
          if (phone.isNotBlank() && phone.trim().length < 10) {
            phoneError = "Please enter a valid 10-digit mobile number or leave empty"
            hasError = true
          }
          if (!hasError) {
            onSaveCustomer(name.trim(), phone.trim(), address.trim()) { errMsg ->
              generalError = errMsg
            }
          }
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = KhataGreenPrimary,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("btn_save_customer")
      ) {
        Text("Save Customer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onCancel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_cancel_add_customer")
      ) {
        Text("Cancel", fontSize = 15.sp)
      }
    }
  }
}
