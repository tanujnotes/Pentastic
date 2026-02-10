package app.pentastic.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun SystemBarEffect(isDarkTheme: Boolean) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    LaunchedEffect(isDarkTheme) {
        activity?.let {
            if (isDarkTheme) {
                it.window.setBackgroundDrawableResource(android.R.color.black)
                it.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                )
            } else {
                it.window.setBackgroundDrawableResource(android.R.color.white)
                it.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
            }
        }
    }
}
