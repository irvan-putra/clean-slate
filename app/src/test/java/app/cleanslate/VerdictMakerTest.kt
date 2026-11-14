package app.cleanslate

import app.cleanslate.analysis.VerdictMaker
import org.junit.Assert.assertTrue
import org.junit.Test

class VerdictMakerTest {

    @Test
    fun emptyDetectionsAreUnableToAssessNotClean() {
        val v = VerdictMaker.make(42.0f, emptyMap(), emptyMap())
        assertTrue(v.startsWith("Can't assess"))
        assertTrue(!v.contains("cleaned", ignoreCase = false))
    }

    @Test
    fun fewerItemsReportsCountsWithLimits() {
        val v = VerdictMaker.make(30f, mapOf("cup" to 3, "bottle" to 1), mapOf("cup" to 1))
        assertTrue(v.contains("4 recognized items before, 1 after"))
        assertTrue(v.contains("cup 3->1"))
        assertTrue(v.contains("can't see dirt"))
    }

    @Test
    fun moreItemsIsNotAJudgment() {
        val v = VerdictMaker.make(20f, mapOf("cup" to 1), mapOf("cup" to 3))
        assertTrue(v.contains("not a judgment"))
    }

    @Test
    fun equalCountsSayNothingAboutWashing() {
        val v = VerdictMaker.make(15f, mapOf("cup" to 2), mapOf("cup" to 2))
        assertTrue(v.contains("nothing about whether they were washed"))
    }
}
