package app.cleanslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.cleanslate.data.SampleData
import app.cleanslate.data.Store
import app.cleanslate.ui.App
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(File(filesDir, "data.json"))
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        SampleData.seed(this, store, photosDir)
        setContent {
            MaterialTheme {
                Surface {
                    App(store = store, photosDir = photosDir)
                }
            }
        }
    }
}
