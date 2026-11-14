package app.cleanslate.analysis

/**
 * Semantic judgment of visible cleaning progress, designed to run on-device.
 * No implementation is bundled yet: the Gemini Nano route (ML Kit GenAI) does not
 * support Samsung Galaxy A24, and the open-model route (Gemma 3n via LiteRT-LM,
 * ~3GB licensed download) is pending the owner's decision. Until then the app
 * shows capture comparison only and never substitutes pixel diff for judgment.
 */
interface Judge {
    val available: Boolean
    val status: String
    suspend fun judge(beforePath: String, afterPath: String): String
}

class PendingJudge : Judge {
    override val available: Boolean = false
    override val status: String =
        "On-device judge not included in this build. Capture and comparison work; " +
            "the private on-device AI judge is a separate decision."
    override suspend fun judge(beforePath: String, afterPath: String): String = status
}
