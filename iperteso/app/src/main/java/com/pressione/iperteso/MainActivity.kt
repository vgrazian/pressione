package com.pressione.iperteso

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.services.ThemeManager
import com.pressione.iperteso.ui.navigation.NavGraph
import com.pressione.iperteso.ui.theme.IperTesoTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)
        // Extract token from deep link: iperteso://share/{token}
        val sharedToken = intent?.data
            ?.takeIf { it.scheme == "iperteso" && it.host == "share" }
            ?.lastPathSegment
        setContent {
            val themeMode by ThemeManager.mode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeManager.Mode.SYSTEM -> systemDark
                ThemeManager.Mode.LIGHT -> false
                ThemeManager.Mode.DARK -> true
            }
            IperTesoTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(sharedToken = sharedToken)
                }
            }
        }
    }
}
