package app.cleanslate.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class Area(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val areaId: String,
    val beforePath: String,
    val afterPath: String? = null,
    val overlayPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val changedPercent: Float? = null,
    val verdict: String? = null,
    val clutterSummary: String? = null,
    val sample: Boolean = false,
)

@Serializable
private data class Db(
    val areas: List<Area> = emptyList(),
    val sessions: List<Session> = emptyList(),
)

class Store(private val file: File) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var db: Db = load()

    private fun load(): Db =
        if (file.exists()) runCatching { json.decodeFromString<Db>(file.readText()) }.getOrDefault(Db())
        else Db()

    @Synchronized
    private fun save() {
        file.writeText(json.encodeToString(db))
    }

    @Synchronized
    fun areas(): List<Area> = db.areas

    @Synchronized
    fun sessionsFor(areaId: String): List<Session> =
        db.sessions.filter { it.areaId == areaId }.sortedByDescending { it.createdAt }

    @Synchronized
    fun session(id: String): Session? = db.sessions.firstOrNull { it.id == id }

    @Synchronized
    fun openSessionFor(areaId: String): Session? =
        db.sessions.filter { it.areaId == areaId && it.afterPath == null }.maxByOrNull { it.createdAt }

    @Synchronized
    fun addArea(name: String): Area {
        val area = Area(name = name.trim())
        db = db.copy(areas = db.areas + area)
        save()
        return area
    }

    @Synchronized
    fun createSession(areaId: String, beforePath: String, sample: Boolean = false): Session {
        val session = Session(areaId = areaId, beforePath = beforePath, sample = sample)
        db = db.copy(sessions = db.sessions + session)
        save()
        return session
    }

    @Synchronized
    fun completeSession(id: String, afterPath: String, overlayPath: String?, changedPercent: Float, verdict: String?, clutterSummary: String? = null) {
        db = db.copy(
            sessions = db.sessions.map {
                if (it.id == id) it.copy(
                    afterPath = afterPath,
                    overlayPath = overlayPath,
                    completedAt = System.currentTimeMillis(),
                    changedPercent = changedPercent,
                    verdict = verdict,
                    clutterSummary = clutterSummary,
                ) else it
            }
        )
        save()
    }

    /** Consecutive days (ending today or yesterday) with at least one completed session. */
    @Synchronized
    fun streakDays(now: Long = System.currentTimeMillis()): Int {
        val dayMillis = 86_400_000L
        fun dayOf(ts: Long): Long = ts / dayMillis
        val days = db.sessions.filter { !it.sample }.mapNotNull { it.completedAt }.map(::dayOf).toSet()
        if (days.isEmpty()) return 0
        var cursor = dayOf(now)
        if (cursor !in days) cursor -= 1
        if (cursor !in days) return 0
        var streak = 0
        while (cursor in days) {
            streak++
            cursor -= 1
        }
        return streak
    }
}
