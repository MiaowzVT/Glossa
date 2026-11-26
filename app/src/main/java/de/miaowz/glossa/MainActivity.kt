package de.miaowz.glossa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.miaowz.glossa.ui.navigation.GlossaNavHost
import de.miaowz.glossa.ui.theme.GlossaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlossaTheme {
                GlossaNavHost()
            }
        }
    }
}
