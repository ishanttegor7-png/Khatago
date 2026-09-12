package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.Customer
import com.example.ui.theme.KhataGreenPrimary
import com.example.util.DashboardCalculator
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SendReminderDialog(
  customer: Customer,
  businessName: String,
  onDismiss: () -> Unit,
  onSendConfirmation: (method: String) -> Unit
) {
  val context = LocalContext.current
  val reminderMessage = DashboardCalculator.formatPaymentReminderMessage(
    customerName = customer.name,
    outstandingAmount = customer.balance,
    businessName = businessName
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    modifier = Modifier.testTag("send_reminder_dialog"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Payment Reminder",
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
          text = "Ready to share with ${customer.name} (${customer.phone}):",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
        ) {
          Text(
            text = reminderMessage,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = "Tap 'Share Reminder' to choose WhatsApp, SMS, or any installed messaging app.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder - $businessName")
            putExtra(Intent.EXTRA_TEXT, reminderMessage)
          }
          val chooser = Intent.createChooser(shareIntent, "Share Payment Reminder")
          chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(chooser)
          onSendConfirmation("Share")
          onDismiss()
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KhataGreenPrimary),
        modifier = Modifier.height(48.dp).testTag("send_whatsapp_reminder_button")
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = null,
          modifier = Modifier.padding(end = 6.dp)
        )
        Text("Share Reminder", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp).testTag("cancel_reminder_button")
      ) {
        Text("Cancel")
      }
    }
  )
}
