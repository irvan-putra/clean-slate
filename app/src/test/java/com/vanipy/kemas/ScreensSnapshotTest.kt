package com.vanipy.kemas

import com.vanipy.kemas.data.Store
import com.vanipy.kemas.ui.AreaScreen
import com.vanipy.kemas.ui.SampleConfirmDialog
import com.vanipy.kemas.ui.VerdictScreen
import com.vanipy.kemas.ui.HomeScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import com.vanipy.kemas.ui.AppSafeArea
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import java.io.File

class ScreensSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun seededStore(): Pair<Store, File> {
        val file = File.createTempFile("kemas-db", ".json")
        val store = Store(file)
        store.addArea("Desk")
        store.addArea("Kitchen counter")
        return store to file
    }

    // Snapshot dates must not drift with the CI clock. This instant matches
    // the original deterministic visual fixture: Sat 24 Oct, 08:17 UTC.
    private fun freezeSessionTimes(file: File) {
        val root = org.json.JSONObject(file.readText())
        val sessions = root.getJSONArray("sessions")
        for (i in 0 until sessions.length()) {
            val session = sessions.getJSONObject(i)
            session.put("createdAt", 1792829820000L)
            if (session.has("completedAt") && !session.isNull("completedAt")) {
                session.put("completedAt", 1792829820000L)
            }
        }
        file.writeText(root.toString())
    }

    @Test
    fun homeScreen() {
        val (store, _) = seededStore()
        paparazzi.snapshot {
            HomeScreen(store = store, version = 0, onOpenArea = {}, onAreaAdded = {})
        }
    }

    // Fixed inset fixtures exercise the actual shared root on narrow phone layouts.
    // Insets are in dp, matching system bars and the keyboard at any display density.
    @Test
    fun homeWithThreeButtonNavigation() {
        val (store, _) = seededStore()
        paparazzi.snapshot {
            AppSafeArea(WindowInsets(top = 24.dp, bottom = 48.dp)) {
                HomeScreen(store, 0, {}, {})
            }
        }
    }

    @Test
    fun homeWithGestureNavigation() {
        val (store, _) = seededStore()
        paparazzi.snapshot {
            AppSafeArea(WindowInsets(top = 24.dp, bottom = 24.dp)) {
                HomeScreen(store, 0, {}, {})
            }
        }
    }

    @Test
    fun homeWithKeyboardVisible() {
        val (store, _) = seededStore()
        paparazzi.snapshot {
            AppSafeArea(WindowInsets(top = 24.dp, bottom = 300.dp)) {
                HomeScreen(store, 0, {}, {})
            }
        }
    }

    @Test
    fun areaScreenWithOpenSession() {
        val (initialStore, file) = seededStore()
        val area = initialStore.areas().first()
        initialStore.createSession(area.id, "/nonexistent/before.jpg")
        freezeSessionTimes(file)
        val store = Store(file)
        val stableArea = store.areas().first()
        paparazzi.snapshot {
            AreaScreen(
                store = store,
                areaId = stableArea.id,
                version = 0,
                onTakeBefore = {},
                onTakeAfter = {},
                onOpenSession = {},
            )
        }
    }

    @Test
    fun verdictScreenCompleted() {
        val (initialStore, file) = seededStore()
        val area = initialStore.areas().first()
        val session = initialStore.createSession(area.id, "/nonexistent/before.jpg")
        val dir = File.createTempFile("verdict-photos", "").let { it.delete(); it.mkdirs(); it }
        val overlayFile = File(dir, "overlay.jpg")
        javaClass.getResourceAsStream("/testdata/overlay.jpg")!!.use { input ->
            overlayFile.outputStream().use { input.copyTo(it) }
        }
        initialStore.completeSession(
            session.id,
            "/nonexistent/after.jpg",
            overlayFile.absolutePath,
            23.4f,
            "Looks cleared: 4 recognized items before, 1 after (cup 3->1). This only counts objects the detector knows - it can't see dirt, grease or whether you actually washed them.",
            "Items detected: 4 before -> 1 after (bottle: 1 -> 0, cup: 3 -> 1)",
        )
        freezeSessionTimes(file)
        val store = Store(file)
        paparazzi.snapshot {
            VerdictScreen(
                store = store,
                photosDir = dir,
                sessionId = session.id,
                afterPathPending = null,
                onDone = {},
            )
        }
    }

    @Test
    fun sampleConfirmDialog() {
        paparazzi.snapshot {
            SampleConfirmDialog(onConfirm = {}, onDismiss = {})
        }
    }

    @Test
    fun areaScreenWithSampleSession() {
        val (store, _) = seededStore()
        val area = store.addArea("Kitchen sink (sample)")
        store.createSession(area.id, "/nonexistent/sample-before.jpg", sample = true)
        paparazzi.snapshot {
            AreaScreen(
                store = store,
                areaId = area.id,
                version = 0,
                onTakeBefore = {},
                onTakeAfter = {},
                onOpenSession = {},
            )
        }
    }

    @Test
    fun areaScreenWithHistory() {
        val (initialStore, file) = seededStore()
        val area = initialStore.areas().first()
        val session = initialStore.createSession(area.id, "/nonexistent/before.jpg")
        initialStore.completeSession(session.id, "/nonexistent/after.jpg", null, 23.4f, null)
        freezeSessionTimes(file)
        val store = Store(file)
        val stableArea = store.areas().first()
        paparazzi.snapshot {
            AreaScreen(
                store = store,
                areaId = stableArea.id,
                version = 0,
                onTakeBefore = {},
                onTakeAfter = {},
                onOpenSession = {},
            )
        }
    }
}
