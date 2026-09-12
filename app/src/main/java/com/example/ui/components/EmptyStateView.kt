package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  description: String,
  actionButtonText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  testTagPrefix: String = "empty_state"
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp)
      .testTag("${testTagPrefix}_container"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .background(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(38.dp)
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      lineHeight = 20.sp
    )

    if (actionButtonText != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onActionClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .height(48.dp)
          .testTag("${testTagPrefix}_action_button")
      ) {
        Text(
          text = actionButtonText,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
