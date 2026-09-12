package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataGreenDark
import com.example.ui.theme.KhataGreenPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var startAnimation by remember { mutableStateOf(false) }

  val scale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.8f,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "scale"
  )

  val alpha by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "alpha"
  )

  LaunchedEffect(Unit) {
    startAnimation = true
    delay(1200)
    onSplashComplete()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            KhataGreenPrimary,
            KhataGreenDark,
            Color(0xFF042F2E)
          )
        )
      )
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .scale(scale)
        .alpha(alpha)
        .padding(32.dp)
    ) {
      // Emblem with Book & Rupee
      Surface(
        modifier = Modifier
          .size(104.dp)
          .testTag("splash_logo_icon"),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 12.dp
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = "KhataGo Logo",
              tint = KhataGreenPrimary,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
              modifier = Modifier
                .size(width = 24.dp, height = 4.dp)
                .background(KhataAmber, CircleShape)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // App Title
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Khata",
          fontSize = 38.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White
        )
        Text(
          text = "Go",
          fontSize = 38.sp,
          fontWeight = FontWeight.ExtraBold,
          color = KhataAmber
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Tagline
      Text(
        text = stringResource(R.string.app_tagline),
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.9f),
        letterSpacing = 0.5.sp
      )

      Spacer(modifier = Modifier.height(40.dp))

      // Micro badge
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
      ) {
        Text(
          text = "Digital Bahi Khata for India",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
      }
    }
  }
}
