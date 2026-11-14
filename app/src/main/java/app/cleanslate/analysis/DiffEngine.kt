package app.cleanslate.analysis

/**
 * Pure pixel comparison between two equally-sized ARGB pixel arrays.
 * Produces a changed-region mask; this is a visual aid, not a cleanliness verdict.
 */
object DiffEngine {

    fun changeMask(before: IntArray, after: IntArray, threshold: Int = 48): BooleanArray {
        require(before.size == after.size) { "pixel arrays must be the same size" }
        val limit = threshold * threshold
        val mask = BooleanArray(before.size)
        for (i in before.indices) {
            val b = before[i]
            val a = after[i]
            val dr = ((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)
            val dg = ((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)
            val db = (b and 0xFF) - (a and 0xFF)
            mask[i] = (dr * dr + dg * dg + db * db) > limit
        }
        return mask
    }

    fun changedPercent(mask: BooleanArray): Float =
        if (mask.isEmpty()) 0f else mask.count { it } * 100f / mask.size

    /** Tints changed pixels red over a dimmed copy of the base image. */
    fun overlay(base: IntArray, mask: BooleanArray): IntArray {
        require(base.size == mask.size)
        val out = IntArray(base.size)
        for (i in out.indices) {
            val p = base[i]
            out[i] = if (mask[i]) {
                (0xFF shl 24) or (0xFF shl 16) or (((p shr 8) and 0xFF) / 3 shl 8) or ((p and 0xFF) / 3)
            } else {
                val r = ((p shr 16) and 0xFF) / 2
                val g = ((p shr 8) and 0xFF) / 2
                val b = (p and 0xFF) / 2
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return out
    }
}
