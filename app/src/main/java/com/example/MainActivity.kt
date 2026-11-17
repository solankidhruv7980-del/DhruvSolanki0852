package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.OmniTubeAppNavigation
import com.example.ui.theme.OmniTubeStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch Intent BEFORE any UI or ViewModels
        configureAppCheck(this, intent)

        // Step 2: Edge-to-Edge Dark Broadcast status & navigation bar styling
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Step 3: Render Compose UI gated behind Google Sign-In
        setContent {
            OmniTubeStudioTheme {
                OmniTubeAppNavigation()
            }
        }
    }
}
