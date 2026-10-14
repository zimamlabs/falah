package dev.falah.app

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FolderFilesTest {
    private val fs = FakeFileSystem()
    private val private = "/data/Zimam/Falah".toPath()
    private val shared = "/sdcard/Zimam/Falah".toPath()
    private var allowed = false
    private fun files() = FolderFiles(fs, private, shared, sharedAllowed = { allowed })

    @Test fun usesThePrivateFolderUntilAccessIsGiven() {
        val f = files(); f.write("a.txt", "one")
        assertFalse(f.shared); assertEquals("one", f.read("a.txt")); assertTrue(fs.exists(private / "a.txt"))
    }

    @Test fun switchingCopiesAndKeepsTheOldFolderAsBackup() {
        val f = files(); f.write("prayer-log.csv", "2026-10-14,Fajr\n"); allowed = true
        assertTrue(f.refresh()); assertTrue(f.shared)
        assertEquals("2026-10-14,Fajr\n", f.read("prayer-log.csv"))
        assertTrue(fs.exists(private / "prayer-log.csv")) // the old copy is still there
        assertNotNull(f.removableOld())
    }

    @Test fun theOldFolderIsOnlyRemovedWhileEverythingInItIsVerified() {
        val f = files(); f.write("a.txt", "one"); allowed = true; f.refresh()
        fs.write(private / "extra.txt") { writeUtf8("only in the old folder") } // not copied yet
        assertNull(f.removableOld()); assertFalse(f.removeOld()); assertTrue(fs.exists(private / "extra.txt"))
        f.refresh() // copies it too
        assertNotNull(f.removableOld()); assertTrue(f.removeOld()); assertFalse(fs.exists(private))
        assertEquals("one", f.read("a.txt"))
    }

    @Test fun aClashingFileInTheZimamFolderIsNeverOverwritten() {
        fs.createDirectories(shared); fs.write(shared / "settings.txt") { writeUtf8("place=Makkah\n") }
        val f = files(); f.write("settings.txt", "place=Cairo\n"); allowed = true; f.refresh()
        assertEquals("place=Makkah\n", f.read("settings.txt"))
        assertEquals("place=Cairo\n", fs.read(shared / "settings (from Private).txt") { readUtf8() })
    }

    @Test fun writingKeepsThePreviousVersion() {
        val f = files(); f.write("a.txt", "v1"); f.write("a.txt", "v2")
        assertEquals("v2", f.read("a.txt")); assertEquals("v1", fs.read(private / "a.txt.bak") { readUtf8() })
    }

    @Test fun settingsComeBackAfterAReinstall() {
        val f = files(); val first = MirroredPrefs(MapPrefs(), f)
        first.set("place", "Makkah"); first.set("notify.Fajr", "1"); first.set("tour.done", "1")
        val second = MirroredPrefs(MapPrefs(), f) // a fresh install reading the same folder
        assertEquals("Makkah", second.get("place")); assertEquals("1", second.get("tour.done"))
    }
}
