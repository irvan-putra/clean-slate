package app.cleanslate.analysis

/**
 * Builds a best-effort verdict from what the app actually measured, with the
 * limits stated next to the claim. Rules:
 * - A verdict only covers recognized-object changes; it is never a cleanliness
 *   or hygiene judgment.
 * - When the detector saw nothing relevant in either photo, the verdict is
 *   "unable to assess" - never an inferred clean/dirty call.
 */
object VerdictMaker {

    fun make(changedPercent: Float, before: Map<String, Int>, after: Map<String, Int>): String {
        val beforeTotal = before.values.sum()
        val afterTotal = after.values.sum()
        return when {
            beforeTotal == 0 && afterTotal == 0 ->
                "Can't assess this one: no recognizable items in either photo, so there is " +
                    "nothing to count. The visible-change view above is the only evidence here, " +
                    "and changed pixels don't say whether things are clean."
            afterTotal < beforeTotal -> {
                val removed = describeDelta(before, after)
                "Looks cleared: $beforeTotal recognized items before, $afterTotal after" +
                    (if (removed.isNotEmpty()) " ($removed)." else ".") +
                    " This only counts objects the detector knows - it can't see dirt, grease or whether you actually washed them."
            }
            afterTotal > beforeTotal ->
                "More recognized items after ($beforeTotal -> $afterTotal). Could be new items in " +
                    "frame, a different angle, or detector noise - not a judgment."
            else ->
                "Same number of recognized items before and after ($beforeTotal). " +
                    "That says nothing about whether they were washed or wiped."
        }
    }

    private fun describeDelta(before: Map<String, Int>, after: Map<String, Int>): String {
        val parts = mutableListOf<String>()
        for ((k, b) in before) {
            val a = after[k] ?: 0
            if (a < b) parts.add("$k ${b}->$a")
        }
        return parts.joinToString(", ")
    }
}
