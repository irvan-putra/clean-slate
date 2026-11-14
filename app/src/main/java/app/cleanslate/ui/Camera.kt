package app.cleanslate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.Executors

@Composable
fun rememberBitmap(path: String?, maxDim: Int = 1024): Bitmap? {
    if (path == null) return null
    return remember(path) { runCatching { Bitmaps.decode(path, maxDim) }.getOrNull() }
}

@Composable
fun CaptureScreen(
    title: String,
    ghostPath: String?,
    photoFile: File,
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                provider.unbindAll()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            }.onFailure { error = it.message }
        }, ContextCompat.getMainExecutor(context))
    }

    val ghost = rememberBitmap(ghostPath)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (hasPermission) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                ghost?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Before photo overlay",
                        alpha = 0.35f,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera permission is needed to take photos.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant permission")
                    }
                }
            }
        }
        error?.let {
            Text("Camera error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("Cancel")
            }
            Button(
                enabled = hasPermission,
                onClick = {
                    val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        options,
                        Executors.newSingleThreadExecutor(),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                onCaptured(photoFile.absolutePath)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                error = exception.message
                            }
                        },
                    )
                },
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text("Take photo")
            }
        }
    }
}
