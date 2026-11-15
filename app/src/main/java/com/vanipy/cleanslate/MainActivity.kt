package com.vanipy.cleanslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.vanipy.cleanslate.data.SampleData
import com.vanipy.cleanslate.data.Store
import com.vanipy.cleanslate.ui.App
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
