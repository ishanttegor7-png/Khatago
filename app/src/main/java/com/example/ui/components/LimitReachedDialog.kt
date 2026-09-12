package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val GoldPrimary = Color(0xFFD97706)

@Composable
fun LimitReachedDialog(
  title: String = "Free Plan Limit Reached",
  message: String,
  onDismiss: () -> Unit,
  onUpgradeClick: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("dialog_limit_reached"),
    icon = {
      Icon(
        imageVector = Icons.Default.WorkspacePremium,
        contentDescription = null,
        tint = GoldPrimary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Upgrade to KhataGo Premium to unlock higher limits, advanced analytics, and CSV exports.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDismiss()
          onUpgradeClick()
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        modifier = Modifier.testTag("btn_dialog_upgrade_premium")
      ) {
        Text("Upgrade to Premium", color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("btn_dialog_dismiss_limit")
      ) {
        Text("Maybe Later")
      }
    }
  )
}
