package app.cleanslate

import app.cleanslate.analysis.YoloDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloDecoderTest {

    private fun outputWith(boxes: List<Triple<Int, Float, FloatArray>>): FloatArray {
        // boxes: (classId, score, [cx, cy, w, h]) written into (84, 8400) layout
        val out = FloatArray(84 * 8400)
        boxes.forEachIndexed { i, (classId, score, box) ->
            out[0 * 8400 + i] = box[0]
            out[1 * 8400 + i] = box[1]
            out[2 * 8400 + i] = box[2]
            out[3 * 8400 + i] = box[3]
            out[(4 + classId) * 8400 + i] = score
        }
        return out
    }

    @Test
    fun decodesSingleDetection() {
        val out = outputWith(listOf(Triple(39, 0.9f, floatArrayOf(320f, 320f, 100f, 80f))))
        val dets = YoloDecoder.decode(out)
        assertEquals(1, dets.size)
        assertEquals(39, dets[0].classId) // bottle
        assertEquals("bottle", YoloDecoder.COCO_CLASSES[dets[0].classId])
    }

    @Test
    fun belowThresholdDropped() {
        val out = outputWith(listOf(Triple(39, 0.2f, floatArrayOf(320f, 320f, 100f, 80f))))
        assertTrue(YoloDecoder.decode(out).isEmpty())
    }

    @Test
    fun overlappingDuplicatesSuppressed() {
        val out = outputWith(
            listOf(
                Triple(39, 0.9f, floatArrayOf(320f, 320f, 100f, 80f)),
                Triple(39, 0.8f, floatArrayOf(322f, 318f, 102f, 82f)),
                Triple(41, 0.7f, floatArrayOf(50f, 50f, 40f, 40f)),
            )
        )
        val dets = YoloDecoder.decode(out)
        assertEquals(2, dets.size)
        assertEquals(setOf(39, 41), dets.map { it.classId }.toSet())
    }

    @Test
    fun cocoClassCountIs80() {
        assertEquals(80, YoloDecoder.COCO_CLASSES.size)
    }
}
