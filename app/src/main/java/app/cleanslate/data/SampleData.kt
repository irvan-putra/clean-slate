package app.cleanslate.data

import android.content.Context
import java.io.File

/**
 * Seeds a clearly-labeled SAMPLE check using a generic, illustrative sink image
 * bundled with the app. The sample is an open check awaiting your own AFTER photo
 * - no fabricated result - and is distinguishable from real captured sessions by
 * its name.
 */
object SampleData {
    private const val SAMPLE_AREA_NAME = "Kitchen sink (sample)"
    private const val SAMPLE_ASSET = "sample-sink-before.jpg"
    private const val SAMPLE_PHOTO = "sample-sink-before.jpg"

    fun seed(context: Context, store: Store, photosDir: File) {
        if (store.areas().isNotEmpty()) return
        val dest = File(photosDir, SAMPLE_PHOTO)
        if (!dest.exists()) {
            context.assets.open(SAMPLE_ASSET).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val area = store.addArea(SAMPLE_AREA_NAME)
        store.createSession(area.id, dest.absolutePath, sample = true)
    }
}
