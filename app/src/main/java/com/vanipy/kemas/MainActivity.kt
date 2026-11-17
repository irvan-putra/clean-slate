package com.vanipy.kemas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.vanipy.kemas.data.SampleData
import com.vanipy.kemas.data.Store
import com.vanipy.kemas.ui.AppSafeArea
import com.vanipy.kemas.ui.App
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(File(filesDir, "data.json"))
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        SampleData.seed(this, store, photosDir)
        setContent {
            MaterialTheme {
                Surface {
                    AppSafeArea {
                        App(store = store, photosDir = photosDir)
                    }
                }
            }
        }
    }
}
