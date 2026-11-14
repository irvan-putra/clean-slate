package app.cleanslate.analysis

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Counts recognizable objects (COCO classes) in a photo, fully on-device via a
 * bundled YOLO11n INT8 LiteRT model (~4.6MB inside the APK, no download, no network).
 * Counts are an object tally for goal-relevant items - not a cleanliness judgment.
 */
class ClutterCounter(context: Context) {

    /** Object classes worth tallying for typical desk/counter/floor goals. */
    val relevantClasses: Set<String> = setOf(
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "book", "scissors",
        "laptop", "mouse", "remote", "keyboard", "cell phone", "backpack", "handbag", "suitcase",
        "sports ball", "teddy bear", "vase", "potted plant", "clock", "banana", "apple", "orange",
        "sandwich", "pizza", "donut", "cake", "toothbrush", "umbrella",
    )

    private val inputSize = 640
    private val interpreter: Interpreter by lazy {
        val fd = context.assets.openFd(MODEL_ASSET)
        FileInputStream(fd.fileDescriptor).channel.use { ch ->
            val mapped = ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            Interpreter(mapped, Interpreter.Options().apply { numThreads = 4 })
        }
    }

    fun close() = interpreter.close()

    /** Returns class name -> count for goal-relevant detections only. */
    fun countRelevant(bitmap: Bitmap): Map<String, Int> {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(1 * 3 * inputSize * inputSize)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        // NCHW int8, symmetric quantization matching the model (scale ~1/255, zero -128)
        for (c in 0 until 3) {
            for (p in pixels) {
                val v = when (c) {
                    0 -> (p shr 16) and 0xFF
                    1 -> (p shr 8) and 0xFF
                    else -> p and 0xFF
                }
                input.put((v - 128).toByte())
            }
        }
        input.rewind()
        val output = Array(1) { Array(84) { FloatArray(8400) } }
        interpreter.run(input, output)
        val detections = YoloDecoder.decode(output[0].flatMap { it.asIterable() }.toFloatArray())
        return detections
            .mapNotNull { d -> YoloDecoder.COCO_CLASSES.getOrNull(d.classId) }
            .filter { it in relevantClasses }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }

    companion object {
        const val MODEL_ASSET = "yolo11n-int8.tflite"
    }
}
