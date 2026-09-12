package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlanLimits
import com.example.model.PlanType
import com.example.model.PlanUsageMetrics
import com.example.ui.theme.KhataAmber
import com.example.ui.theme.KhataAmberBg
import com.example.ui.theme.KhataGreenContainer
import com.example.ui.theme.KhataGreenPrimary
import com.example.ui.theme.LenaHaiGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoldPrimary = Color(0xFFD97706)
private val GoldDark = Color(0xFFB45309)
private val GoldLightBg = Color(0xFFFFFBEB)
private val GoldBorder = Color(0xFFFDE68A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
  planMetrics: PlanUsageMetrics,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showSoonDialog by remember { mutableStateOf(false) }
  var selectedPlanName by remember { mutableStateOf("") }

  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  if (showSoonDialog) {
    AlertDialog(
      onDismissRequest = { showSoonDialog = false },
      modifier = Modifier.testTag("dialog_premium_coming_soon"),
      icon = {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = GoldPrimary,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "KhataGo Premium",
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Premium purchase will be available soon.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Payment gateway integration is currently in progress. You will soon be able to subscribe to the $selectedPlanName plan securely.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showSoonDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
          modifier = Modifier.testTag("btn_close_coming_soon")
        ) {
          Text("Got It", color = Color.White)
        }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.WorkspacePremium,
              contentDescription = null,
              tint = GoldPrimary
            )
            Text(
              text = "KhataGo Premium",
              fontWeight = FontWeight.Bold
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("btn_premium_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.testTag("premium_screen")
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Hero Banner
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GoldLightBg),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
          .testTag("premium_hero_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(GoldPrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Diamond,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(30.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "KHATAGO PREMIUM",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = GoldDark,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "“Har hisaab, aur bhi powerful.”",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Unlock higher limits, advanced reports, and CSV exports for your growing business.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          if (planMetrics.isPremiumActive) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LenaHaiGreen.copy(alpha = 0.15f),
              border = androidx.compose.foundation.BorderStroke(1.dp, LenaHaiGreen)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = LenaHaiGreen,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Active Plan: ${planMetrics.planType.displayName}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = LenaHaiGreen
                )
              }
            }

            if (planMetrics.expiryMillis != null && planMetrics.expiryMillis > 0) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Valid till ${dateFormat.format(Date(planMetrics.expiryMillis))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Plans Selection Cards
      Text(
        text = "Select Your Plan",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Monthly Plan Card
        PlanCard(
          planName = "PREMIUM MONTHLY",
          price = "₹99",
          period = "/month",
          subtitle = "Flexible monthly plan",
          isPopular = false,
          isSelected = planMetrics.isPremiumActive && planMetrics.planType == PlanType.PREMIUM_MONTHLY,
          buttonText = "Choose Monthly",
          onChoose = {
            selectedPlanName = "Premium Monthly (₹99/month)"
            showSoonDialog = true
          },
          modifier = Modifier.weight(1f)
        )

        // Yearly Plan Card
        PlanCard(
          planName = "PREMIUM YEARLY",
          price = "₹799",
          period = "/year",
          subtitle = "Save over 32% annually",
          isPopular = true,
          isSelected = planMetrics.isPremiumActive && planMetrics.planType == PlanType.PREMIUM_YEARLY,
          buttonText = "Choose Yearly",
          onChoose = {
            selectedPlanName = "Premium Yearly (₹799/year)"
            showSoonDialog = true
          },
          modifier = Modifier.weight(1f)
        )
      }

      // Key Benefits Section
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_premium_benefits")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = GoldPrimary
            )
            Text(
              text = "Premium Benefits",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          BenefitItem("No Ads", "Distraction-free experience for seamless billing")
          BenefitItem("Advanced Reports", "Sales trends, top customers, inventory valuation")
          BenefitItem("CSV Export", "Export invoices, ledger, customers & inventory to Excel/CSV")
          BenefitItem("More Customers", "Manage up to 500 customers (vs 50 on Free)")
          BenefitItem("More Products", "Add up to 1000 inventory items (vs 100 on Free)")
          BenefitItem("More Invoices", "Generate up to 500 invoices/month (vs 50 on Free)")
          BenefitItem("Higher Khata Limit", "Up to 2000 entries/month (vs 200 on Free)")
          BenefitItem("Unlimited PDF Exports", "Share unlimited invoice & report PDFs")
          BenefitItem("Higher Cloud/Sync Limits", "Seamless multi-device cloud synchronization")
        }
      }

      // Free vs Premium Comparison Table
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_plan_comparison")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Free vs Premium Comparison",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )

          ComparisonRow(feature = "Customer Limit", freeVal = "50", premiumVal = "500")
          ComparisonRow(feature = "Product Inventory", freeVal = "100", premiumVal = "1000")
          ComparisonRow(feature = "Invoices / Month", freeVal = "50", premiumVal = "500")
          ComparisonRow(feature = "Khata Entries / Month", freeVal = "200", premiumVal = "2000")
          ComparisonRow(feature = "PDF Exports / Month", freeVal = "10", premiumVal = "Unlimited")
          ComparisonRow(feature = "CSV / Excel Export", freeVal = "No", premiumVal = "Yes")
          ComparisonRow(feature = "Advanced Analytics", freeVal = "No", premiumVal = "Yes")
          ComparisonRow(feature = "Ad-free Experience", freeVal = "Standard", premiumVal = "100% Ad-Free")
          ComparisonRow(feature = "Cloud Backup & Sync", freeVal = "Basic", premiumVal = "Priority")
        }
      }

      // Bottom Note
      Text(
        text = "Your business data is always safe on KhataGo. Limits reset at the start of each calendar month.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      )
    }
  }
}

@Composable
private fun PlanCard(
  planName: String,
  price: String,
  period: String,
  subtitle: String,
  isPopular: Boolean,
  isSelected: Boolean,
  buttonText: String,
  onChoose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = when {
    isSelected -> LenaHaiGreen
    isPopular -> GoldPrimary
    else -> MaterialTheme.colorScheme.outlineVariant
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isPopular) GoldLightBg else MaterialTheme.colorScheme.surface
    ),
    modifier = modifier
      .border(if (isPopular || isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
      .testTag("plan_card_${planName.lowercase().replace(" ", "_")}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (isPopular) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GoldPrimary
        ) {
          Text(
            text = "BEST VALUE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.height(18.dp))
      }

      Text(
        text = planName,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (isPopular) GoldDark else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = price,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.ExtraBold,
          color = if (isPopular) GoldDark else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = period,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 3.dp)
        )
      }

      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onChoose,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isPopular) GoldPrimary else KhataGreenPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_choose_${planName.lowercase().replace(" ", "_")}")
      ) {
        Text(
          text = if (isSelected) "Current Plan" else buttonText,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

@Composable
private fun BenefitItem(title: String, description: String) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .padding(top = 2.dp)
        .size(20.dp)
        .clip(CircleShape)
        .background(LenaHaiGreen.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = LenaHaiGreen,
        modifier = Modifier.size(14.dp)
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun ComparisonRow(feature: String, freeVal: String, premiumVal: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = feature,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1.3f)
      )

      Text(
        text = freeVal,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(0.8f)
      )

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = GoldLightBg,
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = premiumVal,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = GoldDark,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 2.dp)
        )
      }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  }
}
