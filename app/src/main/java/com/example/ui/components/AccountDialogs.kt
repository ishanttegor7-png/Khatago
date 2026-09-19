package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FirstLoginChoice
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen

@Composable
fun SignInDialog(
  onDismiss: () -> Unit,
  onSignInGoogle: (onResult: (Boolean, String?) -> Unit) -> Unit,
  onSendPhoneOtp: (phoneNumber: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) -> Unit,
  onVerifyPhoneOtp: (verificationId: String, otp: String, onResult: (Boolean, String?) -> Unit) -> Unit,
  onResendPhoneOtp: ((phoneNumber: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
  onSignInDemo: (() -> Unit)? = null
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var phoneNumber by remember { mutableStateOf("") }
  var verificationId by remember { mutableStateOf<String?>(null) }
  var otpCode by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var resendCountdown by remember { mutableIntStateOf(60) }

  LaunchedEffect(verificationId) {
    if (verificationId != null) {
      resendCountdown = 60
      while (resendCountdown > 0) {
        delay(1000)
        resendCountdown--
      }
    }
  }

  AlertDialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    modifier = Modifier.testTag("dialog_sign_in"),
    title = {
      Column {
        Text(
          text = "KhataGo Account",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Secure your ledger with cloud backup & multi-device sync",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              errorMessage = null
            },
            text = { Text("Phone OTP", fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              errorMessage = null
            },
            text = { Text("Google", fontWeight = FontWeight.SemiBold) }
          )
        }

        if (errorMessage != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMessage!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (selectedTab == 0) {
          // Phone OTP Tab
          if (verificationId == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Enter your 10-digit mobile number to receive a secure OTP code via SMS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 10) phoneNumber = it.filter { char -> char.isDigit() } },
                label = { Text("Mobile Number") },
                prefix = { Text("+91 ") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_phone_number")
              )
              Button(
                onClick = {
                  if (phoneNumber.length < 10) {
                    errorMessage = "Please enter a valid 10-digit mobile number."
                    return@Button
                  }
                  isLoading = true
                  errorMessage = null
                  onSendPhoneOtp(phoneNumber, { vId ->
                    isLoading = false
                    verificationId = vId
                  }, { err ->
                    isLoading = false
                    errorMessage = err
                  })
                },
                enabled = !isLoading && phoneNumber.length >= 10,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_send_otp"),
                colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Send OTP (Get Code)")
              }
            }
          } else {
            // OTP verification step
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "OTP sent to +91 $phoneNumber. Enter code below:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (verificationId?.startsWith("local_sandbox_") == true) {
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Test Code: 123456",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { otpCode = "123456" }) {
                      Text("Auto-fill", fontSize = 11.sp)
                    }
                  }
                }
              }
              OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it.filter { char -> char.isDigit() } },
                label = { Text("6-Digit OTP") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_otp_code")
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(
                  onClick = {
                    verificationId = null
                    otpCode = ""
                    errorMessage = null
                  }
                ) {
                  Text("Change Number")
                }
                TextButton(
                  onClick = {
                    if (resendCountdown == 0) {
                      isLoading = true
                      errorMessage = null
                      val resendAction = onResendPhoneOtp ?: onSendPhoneOtp
                      resendAction(phoneNumber, { vId ->
                        isLoading = false
                        verificationId = vId
                        resendCountdown = 60
                      }, { err ->
                        isLoading = false
                        errorMessage = err
                      })
                    }
                  },
                  enabled = !isLoading && resendCountdown == 0
                ) {
                  Text(if (resendCountdown > 0) "Resend in ${resendCountdown}s" else "Resend OTP")
                }
              }

              Button(
                onClick = {
                  if (otpCode.isBlank()) {
                    errorMessage = "Please enter the OTP code."
                    return@Button
                  }
                  isLoading = true
                  errorMessage = null
                  onVerifyPhoneOtp(verificationId!!, otpCode) { success, err ->
                    isLoading = false
                    if (success) {
                      onDismiss()
                    } else {
                      errorMessage = err ?: "Incorrect OTP. Please retry."
                    }
                  }
                },
                enabled = !isLoading && otpCode.length >= 4,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_verify_otp"),
                colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Verify & Sign In")
              }
            }
          }
        } else {
          // Google Sign-In Tab
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "Sign in quickly using your verified Google Account to backup your records to Google Cloud.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = {
                isLoading = true
                errorMessage = null
                onSignInGoogle { success, err ->
                  isLoading = false
                  if (success) {
                    onDismiss()
                  } else {
                    errorMessage = err
                  }
                }
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_sign_in_google"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Connecting to Google...",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.SemiBold
                )
              } else {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Continue with Google",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            if (onSignInDemo != null) {
              OutlinedButton(
                onClick = {
                  onSignInDemo()
                  onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("btn_sign_in_demo")
              ) {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Continue with Shop Profile",
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isLoading
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun FirstLoginSyncChoiceDialog(
  onDismiss: () -> Unit,
  onChoice: (FirstLoginChoice) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("dialog_first_login_choice"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(LenaHaiBgLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            tint = LenaHaiGreen,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Link Business Ledger",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Existing customer and transaction records were found on this device. Choose how you would like to sync them with your account:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Option 1: Use this phone's existing data (Recommended)
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onChoice(FirstLoginChoice.USE_LOCAL_DATA) }
            .testTag("choice_use_local")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = KhataGreenPrimary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Use this phone's existing data",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Keep all current records and back them up securely to your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Option 2: Merge Both
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onChoice(FirstLoginChoice.MERGE_BOTH) }
            .testTag("choice_merge_both")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.MergeType,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Merge cloud and phone data",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Safely combine existing phone records with cloud backup without duplicates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Option 3: Start Empty / Restore Cloud Only
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onChoice(FirstLoginChoice.START_EMPTY_FROM_CLOUD) }
            .testTag("choice_start_empty")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CloudDownload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Start with an empty business",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Clear unassigned local ledger and download only data from cloud account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Decide Later")
      }
    }
  )
}
