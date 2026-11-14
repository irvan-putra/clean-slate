package app.cleanslate

import app.cleanslate.analysis.DiffEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffEngineTest {

    private fun px(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun identicalImagesHaveNoChange() {
        val img = intArrayOf(px(10, 20, 30), px(200, 100, 50))
        val mask = DiffEngine.changeMask(img, img)
        assertArrayEquals(booleanArrayOf(false, false), mask)
        assertEquals(0f, DiffEngine.changedPercent(mask), 0.001f)
    }

    @Test
    fun largeChangeIsDetected() {
        val before = intArrayOf(px(255, 255, 255), px(100, 100, 100))
        val after = intArrayOf(px(0, 0, 0), px(102, 98, 101))
        val mask = DiffEngine.changeMask(before, after)
        assertArrayEquals(booleanArrayOf(true, false), mask)
        assertEquals(50f, DiffEngine.changedPercent(mask), 0.001f)
    }

    @Test
    fun overlayTintsOnlyChangedPixels() {
        val base = intArrayOf(px(90, 90, 90), px(90, 90, 90))
        val mask = booleanArrayOf(true, false)
        val out = DiffEngine.overlay(base, mask)
        assertEquals(0xFF, (out[0] shr 16) and 0xFF)
        assertEquals((out[1] shr 16) and 0xFF, 45)
    }

    @Test
    fun emptyMaskIsZero() {
        assertEquals(0f, DiffEngine.changedPercent(BooleanArray(0)), 0.001f)
    }
}
