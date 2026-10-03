package app.pixo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppPrefs.attach(applicationContext)
        AppPrefs.init()
        applyAppLanguage(AppPrefs.language)
        initCrashLogging()
        initImageLoaderOnce()
        setContent {
            App()
        }
    }
}
