package app.cleanslate.analysis

import kotlin.math.max
import kotlin.math.min

/** One detected object. Box is center-x, center-y, width, height in pixels of the model input. */
data class Detection(val classId: Int, val score: Float, val cx: Float, val cy: Float, val w: Float, val h: Float)

/**
 * Pure-Kotlin decoder for YOLO11-family TFLite output shaped (1, 84, 8400) float32:
 * 4 box values + 80 COCO class scores per candidate. Unit-tested on the JVM.
 */
object YoloDecoder {

    fun decode(
        output: FloatArray,
        candidates: Int = 8400,
        channels: Int = 84,
        scoreThreshold: Float = 0.45f,
        iouThreshold: Float = 0.5f,
    ): List<Detection> {
        require(output.size == channels * candidates) { "unexpected output size ${output.size}" }
        val dets = ArrayList<Detection>()
        for (i in 0 until candidates) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 4 until channels) {
                val s = output[c * candidates + i]
                if (s > bestScore) {
                    bestScore = s
                    bestClass = c - 4
                }
            }
            if (bestScore >= scoreThreshold) {
                dets.add(
                    Detection(
                        classId = bestClass,
                        score = bestScore,
                        cx = output[0 * candidates + i],
                        cy = output[1 * candidates + i],
                        w = output[2 * candidates + i],
                        h = output[3 * candidates + i],
                    )
                )
            }
        }
        dets.sortByDescending { it.score }
        val kept = ArrayList<Detection>()
        for (d in dets) {
            if (kept.none { iou(it, d) >= iouThreshold }) kept.add(d)
        }
        return kept
    }

    private fun iou(a: Detection, b: Detection): Float {
        val ax1 = a.cx - a.w / 2; val ay1 = a.cy - a.h / 2
        val ax2 = a.cx + a.w / 2; val ay2 = a.cy + a.h / 2
        val bx1 = b.cx - b.w / 2; val by1 = b.cy - b.h / 2
        val bx2 = b.cx + b.w / 2; val by2 = b.cy + b.h / 2
        val iw = max(0f, min(ax2, bx2) - max(ax1, bx1))
        val ih = max(0f, min(ay2, by2) - max(ay1, by1))
        val inter = iw * ih
        return inter / (a.w * a.h + b.w * b.h - inter + 1e-9f)
    }

    val COCO_CLASSES: List<String> = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
        "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
        "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
        "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
        "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
        "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
        "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
        "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush",
    )
}
