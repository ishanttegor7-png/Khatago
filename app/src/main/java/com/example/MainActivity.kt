package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KhataApp
import com.example.ui.KhataViewModel
import com.example.ui.theme.KhataGoTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: KhataViewModel = viewModel()
      val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

      val isDark = when (selectedTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
      }

      KhataGoTheme(darkTheme = isDark) {
        KhataApp(viewModel = viewModel)
      }
    }
  }
}
