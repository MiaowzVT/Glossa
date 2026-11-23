package de.miaowz.glossa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import de.miaowz.glossa.ui.navigation.GlossaNavHost
import de.miaowz.glossa.ui.theme.GlossaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as GlossaApplication).officialCourseRepository

        lifecycleScope.launch {
            try {
                repository.refresh()
                Log.d("Sync", "Stored ${repository.courses.first().size} courses")
            } catch (e: Exception) {
                Log.e("Sync", "Refresh failed", e)
            }
        }
        setContent {
            GlossaTheme {
                GlossaNavHost()
            }
        }
    }
}
