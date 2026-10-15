package dev.falah.app

import dev.falah.core.Prayer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageErrorsTest {
    private val root = "/data/Zimam/Falah".toPath()

    /** A file system whose reads or writes of chosen paths fail, like a bad card or a full disk. */
    private class Flaky(delegate: FileSystem) : ForwardingFileSystem(delegate) {
        var failWrites = false
        var unreadable = emptySet<String>()
        // The first read of a listed file fails (a glitch), later reads work, so the copy the app keeps can be made.
        override fun source(file: Path): Source { if (file.name in unreadable) { unreadable = unreadable - file.name; throw IOException("read error") }; return super.source(file) }
        override fun sink(file: Path, mustCreate: Boolean): Sink { if (failWrites) throw IOException("disk full"); return super.sink(file, mustCreate) }
    }

    private val flaky = Flaky(FakeFileSystem())
    private fun files() = FolderFiles(flaky, root, "/sdcard/Zimam/Falah".toPath(), sharedAllowed = { false })

    @Test fun aMissingFileIsNotAnError() = assertEquals(FileLoad.Missing, files().load("prayer-log.csv"))

    @Test fun aFailedWriteReturnsFalse() {
        val f = files(); flaky.failWrites = true
        assertFalse(f.write("a.txt", "x"))
    }

    @Test fun aFailedPrayerSaveIsShownAndKeptInMemory() {
        val s = FalahState(MapPrefs(), files(), NoPlatform); flaky.failWrites = true
        s.mark(Prayer.Fajr, true)
        assertTrue(s.saveFailed); assertTrue(s.prayed(Prayer.Fajr))
        flaky.failWrites = false; s.mark(Prayer.Dhuhr, true)
        assertFalse(s.saveFailed)
    }

    @Test fun anUnreadableLogIsNeverOverwritten() {
        val f = files(); f.write("prayer-log.csv", "2026-10-01,Fajr\n2026-10-01,Dhuhr\n")
        flaky.unreadable = setOf("prayer-log.csv")
        val s = FalahState(MapPrefs(), f, NoPlatform)
        assertEquals(setOf("prayer-log.csv"), s.unreadable)
        s.mark(Prayer.Fajr, true) // tries to save a one-entry log over the real one
        assertTrue(s.saveFailed)
        assertEquals("2026-10-01,Fajr\n2026-10-01,Dhuhr\n", f.read("prayer-log.csv"))
        assertTrue(flaky.exists(root / "prayer-log.csv.corrupt-1"), "a copy is kept for the user")
    }

    @Test fun anUnreadableTasbihFileIsNeverOverwrittenEither() {
        val f = files(); f.write("tasbih.csv", "2026-10-01,subhanallah,33\n")
        flaky.unreadable = setOf("tasbih.csv")
        val s = FalahState(MapPrefs(), f, NoPlatform); s.tap("subhanallah")
        assertEquals("2026-10-01,subhanallah,33\n", f.read("tasbih.csv"))
    }
}
