package dev.falah.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderMigrationTest {
    private val fs = FakeFileSystem()
    private val m = FolderMigration(fs)
    private val old = "/old".toPath()
    private val new = "/Zimam/Falah".toPath()

    private fun put(path: String, text: String) { val p = path.toPath(); fs.createDirectories(p.parent!!); fs.write(p) { writeUtf8(text) } }
    private fun read(path: String) = fs.read(path.toPath()) { readUtf8() }

    @Test fun copiesEverythingAndKeepsTheOldFolder() {
        put("/old/settings.properties", "place=Cairo\n"); put("/old/logs/a.csv", "1,2")
        val r = m.copyAndVerify(old, new, "Falah")
        assertTrue(r.ok); assertEquals(2, r.copied)
        assertEquals("place=Cairo\n", read("/Zimam/Falah/settings.properties")); assertEquals("1,2", read("/Zimam/Falah/logs/a.csv"))
        assertTrue(fs.exists(old / "settings.properties"))
        assertTrue(m.safeToRemove(old, new, "Falah"))
    }

    @Test fun neverOverwritesAClashingFileAndKeepsBoth() {
        put("/old/settings.properties", "place=Cairo\n"); put("/Zimam/Falah/settings.properties", "place=Makkah\n")
        val r = m.copyAndVerify(old, new, "Falah")
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("place=Makkah\n", read("/Zimam/Falah/settings.properties"))
        assertEquals("place=Cairo\n", read("/Zimam/Falah/settings (from Falah).properties"))
        assertTrue(m.safeToRemove(old, new, "Falah"))
    }

    @Test fun runningTwiceChangesNothing() {
        put("/old/a.txt", "x"); m.copyAndVerify(old, new, "Falah")
        val again = m.copyAndVerify(old, new, "Falah")
        assertTrue(again.ok); assertEquals(0, again.copied); assertEquals(0, again.kept)
    }

    @Test fun anInterruptedCopyIsFinishedNextTime() {
        put("/old/a.txt", "one"); put("/old/b.txt", "two")
        put("/Zimam/Falah/a.txt", "one"); put("/Zimam/Falah/b.txt.part", "tw") // stopped half way through b
        val r = m.copyAndVerify(old, new, "Falah")
        assertTrue(r.ok); assertEquals("two", read("/Zimam/Falah/b.txt"))
    }

    @Test fun aMissingOrChangedFileIsNotSafeToRemove() {
        put("/old/a.txt", "one"); m.copyAndVerify(old, new, "Falah")
        put("/Zimam/Falah/a.txt", "changed")
        assertFalse(m.safeToRemove(old, new, "Falah"))
        fs.delete(new / "a.txt")
        assertFalse(m.safeToRemove(old, new, "Falah"))
        assertTrue(m.verify(old, new, "Falah").isNotEmpty())
    }

    @Test fun aSecondClashGetsANumberedName() {
        put("/old/s.txt", "A"); put("/Zimam/Falah/s.txt", "B"); put("/Zimam/Falah/s (from Falah).txt", "C")
        assertTrue(m.copyAndVerify(old, new, "Falah").ok)
        assertEquals("A", read("/Zimam/Falah/s (from Falah 2).txt"))
    }

    @Test fun noOldFolderIsNothingToDo() {
        val r = m.copyAndVerify("/nothing".toPath(), new, "Falah")
        assertTrue(r.ok); assertEquals(0, r.copied)
        assertFalse(m.safeToRemove("/nothing".toPath(), new, "Falah"))
    }
}
