package com.pressione.iperteso

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.ui.navigation.NavGraph
import com.pressione.iperteso.ui.theme.IperTesoTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Extract token from deep link: iperteso://share/{token}
        val sharedToken = intent?.data
            ?.takeIf { it.scheme == "iperteso" && it.host == "share" }
            ?.lastPathSegment
        setContent {
            IperTesoTheme {
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
