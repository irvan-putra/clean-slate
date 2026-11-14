package app.cleanslate

import app.cleanslate.data.Store
import app.cleanslate.ui.AreaScreen
import app.cleanslate.ui.SampleConfirmDialog
import app.cleanslate.ui.VerdictScreen
import app.cleanslate.ui.HomeScreen
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

    private fun seededStore(): Store {
        val store = Store(File.createTempFile("cleanslate-db", ".json"))
        store.addArea("Desk")
        store.addArea("Kitchen counter")
        return store
    }

    @Test
    fun homeScreen() {
        val store = seededStore()
        paparazzi.snapshot {
            HomeScreen(store = store, version = 0, onOpenArea = {}, onAreaAdded = {})
        }
    }

    @Test
    fun areaScreenWithOpenSession() {
        val store = seededStore()
        val area = store.areas().first()
        store.createSession(area.id, "/nonexistent/before.jpg")
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
    fun verdictScreenCompleted() {
        val store = seededStore()
        val area = store.areas().first()
        val session = store.createSession(area.id, "/nonexistent/before.jpg")
        val dir = File.createTempFile("verdict-photos", "").let { it.delete(); it.mkdirs(); it }
        val overlayFile = File(dir, "overlay.jpg")
        javaClass.getResourceAsStream("/testdata/overlay.jpg")!!.use { input ->
            overlayFile.outputStream().use { input.copyTo(it) }
        }
        store.completeSession(
            session.id,
            "/nonexistent/after.jpg",
            overlayFile.absolutePath,
            23.4f,
            "Looks cleared: 4 recognized items before, 1 after (cup 3->1). This only counts objects the detector knows - it can't see dirt, grease or whether you actually washed them.",
            "Items detected: 4 before -> 1 after (bottle: 1 -> 0, cup: 3 -> 1)",
        )
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
        val store = seededStore()
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
        val store = seededStore()
        val area = store.areas().first()
        val s = store.createSession(area.id, "/nonexistent/before.jpg")
        store.completeSession(s.id, "/nonexistent/after.jpg", null, 23.4f, null)
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
}
