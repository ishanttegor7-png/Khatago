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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.model.BusinessProfile
import com.example.model.PlanType
import com.example.model.PlanUsageMetrics
import com.example.sync.SyncStatus
import com.example.ui.components.SignInDialog
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiBgLight
import com.example.ui.theme.LenaHaiGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
  businessProfile: BusinessProfile,
  selectedLanguage: String,
  selectedTheme: String,
  notificationsEnabled: Boolean,
  currentUser: UserAccount?,
  syncStatus: SyncStatus,
  lastSyncTimestamp: Long,
  lastSyncMessage: String,
  isFirebaseConfigured: Boolean,
  firebaseNotice: String,
  planMetrics: PlanUsageMetrics = PlanUsageMetrics(),
  onOpenBusinessProfile: () -> Unit,
  onOpenPremium: () -> Unit = {},
  onLanguageChange: (String) -> Unit,
  onThemeChange: (String) -> Unit,
  onToggleNotifications: () -> Unit,
  onSignInGoogle: (onResult: (Boolean, String?) -> Unit) -> Unit,
  onSignInDemo: () -> Unit = {},
  onSendPhoneOtp: (phoneNumber: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) -> Unit,
  onVerifyPhoneOtp: (verificationId: String, otp: String, onResult: (Boolean, String?) -> Unit) -> Unit,
  onResendPhoneOtp: ((phoneNumber: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
  onSignOut: () -> Unit,
  onSyncNow: () -> Unit,
  onRestoreData: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showLanguageDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showSignInDialog by remember { mutableStateOf(false) }
  var showRestoreConfirmDialog by remember { mutableStateOf(false) }
  var showSignOutConfirmDialog by remember { mutableStateOf(false) }

  if (showLanguageDialog) {
    LanguageSelectionDialog(
      currentLanguage = selectedLanguage,
      onDismiss = { showLanguageDialog = false },
      onSelect = {
        onLanguageChange(it)
        showLanguageDialog = false
      }
    )
  }

  if (showThemeDialog) {
    ThemeSelectionDialog(
      currentTheme = selectedTheme,
      onDismiss = { showThemeDialog = false },
      onSelect = {
        onThemeChange(it)
        showThemeDialog = false
      }
    )
  }

  if (showSignInDialog) {
    SignInDialog(
      onDismiss = { showSignInDialog = false },
      onSignInGoogle = onSignInGoogle,
      onSignInDemo = onSignInDemo,
      onSendPhoneOtp = onSendPhoneOtp,
      onVerifyPhoneOtp = onVerifyPhoneOtp,
      onResendPhoneOtp = onResendPhoneOtp
    )
  }

  if (showRestoreConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showRestoreConfirmDialog = false },
      modifier = Modifier.testTag("dialog_confirm_restore"),
      title = { Text("Restore Ledger from Cloud?") },
      text = {
        Text("This will download all your saved customers, transactions, and invoices from your cloud account onto this phone. Existing local records will be merged safely.")
      },
      confirmButton = {
        Button(
          onClick = {
            showRestoreConfirmDialog = false
            onRestoreData()
          },
          colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary)
        ) {
          Text("Restore Now")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRestoreConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showSignOutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirmDialog = false },
      modifier = Modifier.testTag("dialog_confirm_signout"),
      title = { Text("Sign Out of KhataGo?") },
      text = {
        Text("Your business ledger remains safely stored on this phone. You can sign back in anytime to sync to the cloud.")
      },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirmDialog = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .testTag("settings_screen")
  ) {
    // Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Settings & Cloud Sync",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Manage your shop profile, cloud account, and ledger backup",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Account / Profile Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setting_account_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          if (currentUser != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = (currentUser.displayName ?: currentUser.phoneNumber ?: "U").take(1).uppercase(),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = currentUser.displayName ?: (currentUser.phoneNumber ?: "KhataGo User"),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = currentUser.email ?: (currentUser.phoneNumber ?: "Logged In"),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Account UID: ${currentUser.uid.take(12)}...",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.outline
                )
              }
              OutlinedButton(
                onClick = { showSignOutConfirmDialog = true },
                modifier = Modifier.testTag("btn_sign_out"),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Out", fontSize = 11.sp)
              }
            }
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(LenaHaiBgLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = LenaHaiGreen,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "KhataGo Account",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Sign in to enable cloud backup",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Button(
                onClick = { showSignInDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_sign_in")
              ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign In", fontSize = 12.sp)
              }
            }
          }
        }
      }

      // KhataGo Plan & Premium Card
      val goldPrimary = Color(0xFFD97706)
      val goldDark = Color(0xFFB45309)
      val goldLightBg = Color(0xFFFFFBEB)
      val goldBorder = Color(0xFFFDE68A)
      val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (planMetrics.isPremiumActive) goldLightBg else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setting_plan_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (planMetrics.isPremiumActive) goldPrimary else MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = if (planMetrics.isPremiumActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Current Plan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = if (planMetrics.isPremiumActive) planMetrics.planType.displayName.uppercase() else "FREE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (planMetrics.isPremiumActive) goldDark else MaterialTheme.colorScheme.onSurface
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (planMetrics.isPremiumActive) LenaHaiGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = if (planMetrics.isPremiumActive) "ACTIVE" else "FREE TIER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (planMetrics.isPremiumActive) LenaHaiGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          if (planMetrics.isPremiumActive) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Plan Type: ${planMetrics.planType.displayName}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (planMetrics.expiryMillis != null && planMetrics.expiryMillis > 0) {
                Text(
                  text = "Expiry Date: ${dateFmt.format(Date(planMetrics.expiryMillis))}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = "✓ No Ads  •  ✓ Advanced Reports  •  ✓ CSV Export  •  ✓ High Limits",
                style = MaterialTheme.typography.labelSmall,
                color = goldDark
              )
            }

            Button(
              onClick = onOpenPremium,
              colors = ButtonDefaults.buttonColors(containerColor = goldPrimary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_view_premium_details")
            ) {
              Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("View Premium Details", color = Color.White, fontWeight = FontWeight.Bold)
            }
          } else {
            // Free Tier Usage Overview
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Monthly & Total Usage",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Customers: ${planMetrics.customerCount} / ${planMetrics.maxCustomers}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Products: ${planMetrics.productCount} / ${planMetrics.maxProducts}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Invoices this month: ${planMetrics.monthlyInvoiceCount} / ${planMetrics.maxMonthlyInvoices}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Khata entries: ${planMetrics.monthlyTransactionCount} / ${planMetrics.maxMonthlyTransactions}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = "PDF exports this month: ${planMetrics.monthlyPdfExportCount} / ${planMetrics.maxMonthlyPdfExports}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = onOpenPremium,
              colors = ButtonDefaults.buttonColors(containerColor = goldPrimary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_upgrade_from_settings")
            ) {
              Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Upgrade to KhataGo Premium", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 2. Business Profile Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenBusinessProfile() }
          .testTag("setting_business_profile")
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(KhataGreenContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Store,
              contentDescription = null,
              tint = KhataGreenPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Business Profile",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${businessProfile.businessName} • ${businessProfile.ownerName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open profile",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 3. Cloud Backup & Synchronization Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setting_backup")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LenaHaiBgLight),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                tint = LenaHaiGreen,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Cloud Backup & Sync",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Last synced: ${formatLastSyncTime(lastSyncTimestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Sync Status Badge
            SyncStatusBadge(syncStatus = syncStatus)
          }

          if (lastSyncMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = lastSyncMessage,
              style = MaterialTheme.typography.labelSmall,
              color = if (syncStatus == SyncStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (currentUser == null) {
                  showSignInDialog = true
                } else {
                  onSyncNow()
                }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("btn_backup_now"),
              colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
              enabled = syncStatus != SyncStatus.SYNCING
            ) {
              if (syncStatus == SyncStatus.SYNCING) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Syncing...", fontSize = 12.sp)
              } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Now", fontSize = 12.sp)
              }
            }

            OutlinedButton(
              onClick = {
                if (currentUser == null) {
                  showSignInDialog = true
                } else {
                  showRestoreConfirmDialog = true
                }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("btn_restore_cloud"),
              enabled = syncStatus != SyncStatus.SYNCING
            ) {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Restore", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Data Isolation note
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Account Isolation: Ledger is strictly secured by UID.",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // App Preferences Section Header
      Text(
        text = "Preferences",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
      )

      // 4. Language Setting
      SettingItemRow(
        title = "App Language",
        subtitle = selectedLanguage,
        icon = Icons.Default.Language,
        iconBg = KhataAmberBg,
        iconTint = KhataAmber,
        onClick = { showLanguageDialog = true },
        testTag = "setting_language"
      )

      // 5. Theme Setting
      SettingItemRow(
        title = "Theme Display",
        subtitle = "$selectedTheme Mode",
        icon = Icons.Default.DarkMode,
        iconBg = MaterialTheme.colorScheme.surfaceVariant,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = { showThemeDialog = true },
        testTag = "setting_theme"
      )

      // 6. Notifications Toggle Row
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setting_notifications")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = null,
              tint = KhataGreenPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Payment Alerts & Reminders",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (notificationsEnabled) "Daily hisaab summary enabled" else "Alerts turned off",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = notificationsEnabled,
            onCheckedChange = { onToggleNotifications() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = KhataGreenPrimary
            ),
            modifier = Modifier.testTag("switch_notifications")
          )
        }
      }

      // 7. About KhataGo Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_about_app")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = KhataGreenPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "KhataGo v1.0.0 (Phase 4)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "“Har hisaab, ek jagah.”",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Offline-first with secure cloud sync for Indian Kirana & Small Businesses",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun SyncStatusBadge(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
  val (label, bg, fg, icon) = when (syncStatus) {
    SyncStatus.SYNCED -> SyncBadgeData("Synced", LenaHaiBgLight, LenaHaiGreen, Icons.Default.CheckCircle)
    SyncStatus.SYNCING -> SyncBadgeData("Syncing", KhataGreenContainer, KhataGreenPrimary, Icons.Default.Sync)
    SyncStatus.PENDING -> SyncBadgeData("Pending", KhataAmberBg, KhataAmber, Icons.Default.CloudUpload)
    SyncStatus.OFFLINE -> SyncBadgeData("Offline", Color(0xFFF1F5F9), Color(0xFF64748B), Icons.Default.CloudOff)
    SyncStatus.FAILED -> SyncBadgeData("Failed", Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.ErrorOutline)
    SyncStatus.NOT_CONFIGURED -> SyncBadgeData("Ready", Color(0xFFF1F5F9), Color(0xFF64748B), Icons.Default.CloudQueue)
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("badge_sync_status")
  ) {
    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
  }
}

private data class SyncBadgeData(val label: String, val bg: Color, val fg: Color, val icon: ImageVector)

private fun formatLastSyncTime(millis: Long): String {
  if (millis <= 0L) return "Never"
  val diff = System.currentTimeMillis() - millis
  if (diff < 60_000L) return "Just now"
  if (diff < 3600_000L) return "${diff / 60_000L}m ago"
  val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
  return sdf.format(Date(millis))
}

@Composable
private fun SettingItemRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconBg: Color,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun LanguageSelectionDialog(
  currentLanguage: String,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit
) {
  val languages = listOf("English", "Hindi (हिंदी)", "Hinglish")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Select App Language", fontWeight = FontWeight.Bold)
    },
    text = {
      Column {
        languages.forEach { lang ->
          val isSelected = currentLanguage == lang ||
            (lang.startsWith("Hindi") && currentLanguage == "Hindi")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(lang.substringBefore(" (")) }
              .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = isSelected,
              onClick = { onSelect(lang.substringBefore(" (")) },
              colors = RadioButtonDefaults.colors(selectedColor = KhataGreenPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = lang, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}

@Composable
private fun ThemeSelectionDialog(
  currentTheme: String,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit
) {
  val themes = listOf("System", "Light", "Dark")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Select Theme", fontWeight = FontWeight.Bold)
    },
    text = {
      Column {
        themes.forEach { theme ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(theme) }
              .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentTheme == theme,
              onClick = { onSelect(theme) },
              colors = RadioButtonDefaults.colors(selectedColor = KhataGreenPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$theme Mode", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
