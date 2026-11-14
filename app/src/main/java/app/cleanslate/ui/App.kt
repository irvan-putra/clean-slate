package app.cleanslate.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cleanslate.analysis.ClutterCounter
import app.cleanslate.analysis.DiffEngine
import app.cleanslate.analysis.VerdictMaker
import app.cleanslate.data.Area
import app.cleanslate.data.Session
import app.cleanslate.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Home : Screen
    data class AreaDetail(val areaId: String) : Screen
    data class CaptureBefore(val areaId: String) : Screen
    data class CaptureAfter(val areaId: String, val sessionId: String) : Screen
    data class Verdict(val sessionId: String) : Screen
}

@Composable
fun App(store: Store, photosDir: File) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    // Bumped whenever the store changes so lists re-read.
    var version by remember { mutableIntStateOf(0) }

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    when (val screen = stack.last()) {
        Screen.Home -> HomeScreen(
            store = store,
            version = version,
            onOpenArea = { stack.add(Screen.AreaDetail(it)) },
            onAreaAdded = { version++ },
        )
        is Screen.AreaDetail -> AreaScreen(
            store = store,
            areaId = screen.areaId,
            version = version,
            onTakeBefore = { stack.add(Screen.CaptureBefore(screen.areaId)) },
            onTakeAfter = { sessionId -> stack.add(Screen.CaptureAfter(screen.areaId, sessionId)) },
            onOpenSession = { stack.add(Screen.Verdict(it)) },
        )
        is Screen.CaptureBefore -> CaptureScreen(
            title = "Take the BEFORE photo",
            ghostPath = null,
            photoFile = File(photosDir, "before-${System.currentTimeMillis()}.jpg"),
            onCaptured = { path ->
                store.createSession(screen.areaId, path)
                version++
                stack.removeAt(stack.lastIndex)
            },
            onCancel = { stack.removeAt(stack.lastIndex) },
        )
        is Screen.CaptureAfter -> {
            val session = store.session(screen.sessionId)
            if (session == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) { stack.removeAt(stack.lastIndex) }
            } else {
                CaptureScreen(
                    title = "Take the AFTER photo from the same spot",
                    ghostPath = session.beforePath,
                    photoFile = File(photosDir, "after-${session.id}.jpg"),
                    onCaptured = { path ->
                        version++
                        stack.removeAt(stack.lastIndex)
                        stack.add(Screen.Verdict(session.id))
                    },
                    onCancel = { stack.removeAt(stack.lastIndex) },
                )
            }
        }
        is Screen.Verdict -> VerdictScreen(
            store = store,
            photosDir = photosDir,
            sessionId = screen.sessionId,
            afterPathPending = File(photosDir, "after-${screen.sessionId}.jpg").takeIf { it.exists() }?.absolutePath,
            onDone = {
                version++
                stack.removeAt(stack.lastIndex)
            },
        )
    }
}

@Composable
fun HomeScreen(store: Store, version: Int, onOpenArea: (String) -> Unit, onAreaAdded: () -> Unit) {
    var newArea by remember { mutableStateOf("") }
    val areas = remember(version) { store.areas() }
    val streak = remember(version) { store.streakDays() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Clean Slate", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (streak > 0) "Streak: $streak day${if (streak == 1) "" else "s"}" else "No streak yet - finish a before/after check.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(areas, key = { it.id }) { area ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenArea(area.id) }) {
                    Text(area.name, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newArea,
                onValueChange = { newArea = it },
                label = { Text("Area name (e.g. desk)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (newArea.isNotBlank()) {
                        store.addArea(newArea)
                        newArea = ""
                        onAreaAdded()
                    }
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically),
            ) { Text("Add") }
        }
    }
}

@Composable
fun AreaScreen(
    store: Store,
    areaId: String,
    version: Int,
    onTakeBefore: () -> Unit,
    onTakeAfter: (String) -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val area = remember(areaId, version) { store.areas().firstOrNull { it.id == areaId } }
    val sessions = remember(areaId, version) { store.sessionsFor(areaId) }
    val open = remember(areaId, version) { store.openSessionFor(areaId) }
    val fmt = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(area?.name ?: "Area", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (open == null) {
            Button(onClick = onTakeBefore, modifier = Modifier.fillMaxWidth()) {
                Text("Start a check - take BEFORE photo")
            }
        } else {
            var showSampleConfirm by remember { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (open.sample) {
                        Text("Sample check", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "This check uses your volunteered sink photo as its BEFORE image. " +
                                "It never counts toward your streak.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text("Check started ${fmt.format(Date(open.createdAt))}.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Finished cleaning? Take the AFTER photo from the same spot.")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { if (open.sample) showSampleConfirm = true else onTakeAfter(open.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Take AFTER photo")
                    }
                }
            }
            if (showSampleConfirm) {
                SampleConfirmDialog(
                    onConfirm = {
                        showSampleConfirm = false
                        onTakeAfter(open.id)
                    },
                    onDismiss = { showSampleConfirm = false },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("History", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions.filter { it.completedAt != null }, key = { it.id }) { s ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenSession(s.id) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            fmt.format(Date(s.completedAt!!)) + if (s.sample) " (sample)" else "",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        s.changedPercent?.let {
                            Text("Visible change: %.1f%% of the frame".format(it), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerdictScreen(
    store: Store,
    photosDir: File,
    sessionId: String,
    afterPathPending: String?,
    onDone: () -> Unit,
) {
    var session by remember(sessionId) { mutableStateOf(store.session(sessionId)) }
    var working by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
    var clutter by remember { mutableStateOf<String?>(null) }
    var verdict by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val appContext = LocalContext.current.applicationContext

    androidx.compose.runtime.LaunchedEffect(afterPathPending) {
        val afterPath = afterPathPending ?: return@LaunchedEffect
        val current = store.session(sessionId) ?: return@LaunchedEffect
        if (current.completedAt != null || working) return@LaunchedEffect
        working = true
        withContext(Dispatchers.Default) {
            runCatching {
                val dim = 512
                val before = Bitmaps.decode(current.beforePath, dim)
                val afterRaw = Bitmaps.decode(afterPath, dim)
                val after = Bitmaps.cropTo(afterRaw, before.width, before.height)
                val mask = DiffEngine.changeMask(Bitmaps.pixels(before), Bitmaps.pixels(after))
                val percent = DiffEngine.changedPercent(mask)
                val overlayBitmap = Bitmaps.fromPixels(
                    DiffEngine.overlay(Bitmaps.pixels(after), mask),
                    after.width,
                    after.height,
                )
                val overlayFile = File(photosDir, "overlay-$sessionId.jpg")
                overlayFile.outputStream().use { overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                var verdictText: String? = null
                val summary = runCatching {
                    val counter = ClutterCounter(appContext)
                    try {
                        val beforeCounts = counter.countRelevant(before)
                        val afterCounts = counter.countRelevant(after)
                        verdictText = VerdictMaker.make(percent, beforeCounts, afterCounts)
                        formatClutter(beforeCounts, afterCounts)
                    } finally {
                        counter.close()
                    }
                }.getOrNull()
                store.completeSession(sessionId, afterPath, overlayFile.absolutePath, percent, verdictText, summary)
                verdict = verdictText
                clutter = summary
                overlay = overlayBitmap
            }.onFailure { error = it.message }
        }
        session = store.session(sessionId)
        working = false
    }

    val overlayFromDisk = rememberBitmap(session?.overlayPath, 768)
    val shownOverlay = overlay ?: overlayFromDisk

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Result", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        when {
            working -> Text("Comparing photos on this phone...")
            error != null -> Text("Comparison failed: $error", color = MaterialTheme.colorScheme.error)
            shownOverlay != null -> {
                Image(
                    bitmap = shownOverlay.asImageBitmap(),
                    contentDescription = "Changed regions highlighted",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Spacer(Modifier.height(8.dp))
                session?.changedPercent?.let {
                    Text("Visible change: %.1f%% of the frame".format(it), style = MaterialTheme.typography.titleMedium)
                }
                val verdictText = verdict ?: session?.verdict
                verdictText?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.titleSmall)
                }
                val clutterText = clutter ?: session?.clutterSummary
                clutterText?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Counts recognizable objects only - it does not see dust, wiping, or everything else.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Red marks what visibly changed between your photos. That is a visual aid, not a cleanliness judgment.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> Text("No comparison available yet.")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}


/** Human-readable before/after tally of goal-relevant detected objects. */
fun formatClutter(before: Map<String, Int>, after: Map<String, Int>): String {
    val keys = (before.keys + after.keys).toSortedSet()
    if (keys.isEmpty()) return "Items detected: none of the tracked object types in either photo."
    val beforeTotal = before.values.sum()
    val afterTotal = after.values.sum()
    val lines = keys.map { k -> "$k: ${before[k] ?: 0} -> ${after[k] ?: 0}" }
    return "Items detected: $beforeTotal before -> $afterTotal after (" + lines.joinToString(", ") + ")"
}


@Composable
fun SampleConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use the sample BEFORE photo?") },
        text = {
            Text(
                "Your AFTER photo will be compared against the sample sink photo you sent. " +
                    "Start a real check instead if you want your own BEFORE photo."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Use sample") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
