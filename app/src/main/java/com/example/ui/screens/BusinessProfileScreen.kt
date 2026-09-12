package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.model.BusinessProfile
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary

@Composable
fun BusinessProfileScreen(
  profile: BusinessProfile,
  onSaveProfile: (businessName: String, ownerName: String, phone: String, address: String, upiId: String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var businessName by remember { mutableStateOf(profile.businessName) }
  var ownerName by remember { mutableStateOf(profile.ownerName) }
  var phone by remember { mutableStateOf(profile.phone) }
  var address by remember { mutableStateOf(profile.address) }
  var upiId by remember { mutableStateOf(profile.upiId) }

  var businessNameError by remember { mutableStateOf<String?>(null) }
  var ownerNameError by remember { mutableStateOf<String?>(null) }
  var saveSuccessMessage by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("business_profile_screen"),
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_back_business_profile")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "Business Profile",
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
      // Business Logo Placeholder Section
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(KhataGreenContainer)
              .clickable { /* Logo change simulation */ }
              .testTag("business_logo_placeholder"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Store,
              contentDescription = null,
              tint = KhataGreenPrimary,
              modifier = Modifier.size(40.dp)
            )
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(KhataGreenPrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Change photo",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = businessName.ifBlank { "Your Shop / Business" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Tap icon to upload or change shop logo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Fields
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Shop & Owner Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )

          // Business Name
          OutlinedTextField(
            value = businessName,
            onValueChange = {
              businessName = it
              if (it.isNotBlank()) businessNameError = null
            },
            label = { Text("Business / Shop Name *") },
            placeholder = { Text("e.g. Sharma Kirana Store") },
            leadingIcon = {
              Icon(Icons.Default.Store, contentDescription = null, tint = KhataGreenPrimary)
            },
            isError = businessNameError != null,
            supportingText = {
              if (businessNameError != null) {
                Text(text = businessNameError!!, color = MaterialTheme.colorScheme.error)
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_business_name")
          )

          // Owner Name
          OutlinedTextField(
            value = ownerName,
            onValueChange = {
              ownerName = it
              if (it.isNotBlank()) ownerNameError = null
            },
            label = { Text("Owner / Manager Name *") },
            placeholder = { Text("e.g. Rajesh Sharma") },
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = null, tint = KhataGreenPrimary)
            },
            isError = ownerNameError != null,
            supportingText = {
              if (ownerNameError != null) {
                Text(text = ownerNameError!!, color = MaterialTheme.colorScheme.error)
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_owner_name")
          )

          // Mobile Number
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Business Mobile Number") },
            leadingIcon = {
              Icon(Icons.Default.Phone, contentDescription = null, tint = KhataGreenPrimary)
            },
            prefix = { Text("+91 ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_business_phone")
          )

          // Address
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Shop Address") },
            placeholder = { Text("e.g. Shop No. 12, Main Market") },
            leadingIcon = {
              Icon(Icons.Default.Home, contentDescription = null, tint = KhataGreenPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_business_address")
          )

          // UPI ID
          OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            label = { Text("UPI ID for Payments (Optional)") },
            placeholder = { Text("e.g. shopname@upi") },
            leadingIcon = {
              Icon(Icons.Default.QrCode, contentDescription = null, tint = KhataGreenPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_business_upi")
          )
        }
      }

      if (saveSuccessMessage) {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "✓ Business details saved successfully!",
            color = KhataGreenPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Save Button
      Button(
        onClick = {
          var hasError = false
          if (businessName.trim().isBlank()) {
            businessNameError = "Please enter business name"
            hasError = true
          }
          if (ownerName.trim().isBlank()) {
            ownerNameError = "Please enter owner name"
            hasError = true
          }
          if (!hasError) {
            onSaveProfile(businessName.trim(), ownerName.trim(), phone.trim(), address.trim(), upiId.trim())
            saveSuccessMessage = true
          }
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("btn_save_business_profile")
      ) {
        Text("Save Business Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
