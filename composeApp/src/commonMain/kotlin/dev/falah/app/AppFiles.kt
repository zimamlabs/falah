package dev.falah.app

import dev.falah.core.FolderMigration
import okio.FileSystem
import okio.Path

/** Where Falah keeps the files you can see: its settings copy and the prayer log. */
interface AppFiles {
    /** The file's text, or null when it is missing or unreadable; use [load] when the difference matters. */
    fun read(name: String): String? = (load(name) as? FileLoad.Loaded)?.text
    /** Reads a file and tells a missing file from one that exists but cannot be read. */
    fun load(name: String): FileLoad
    /** Writes the file; false when it could not be saved (disk full, folder not writable). */
    fun write(name: String, text: String): Boolean
    /** The folder in use, as text. */
    val location: String
    /** True when that is the visible Zimam folder, false for the app's private folder. */
    val shared: Boolean
    /** True when the system would let the user switch to the Zimam folder but has not been asked yet. */
    val canAskAccess: Boolean
    fun askAccess()
    /** Picks the folder again (after the permission changed); true when it changed, so callers reload. */
    fun refresh(): Boolean
    /** Why the last switch was held back, if it was. */
    val message: Pair<String, String>?
    /** The old private folder, when everything in it is verified in the Zimam folder and it can be removed; otherwise null. */
    fun removableOld(): String?
    fun removeOld(): Boolean
}

/** The result of reading a data file. */
sealed interface FileLoad {
    data object Missing : FileLoad
    data class Loaded(val text: String) : FileLoad
    /** The file exists but could not be read; the app must not write over it. */
    data object Unreadable : FileLoad
}

/** Files in memory only; used before a platform supplies real ones. */
class MemoryFiles : AppFiles {
    private val data = mutableMapOf<String, String>()
    override fun load(name: String) = data[name]?.let { FileLoad.Loaded(it) } ?: FileLoad.Missing
    override fun write(name: String, text: String): Boolean { data[name] = text; return true }
    override val location = "Memory"
    override val shared = false
    override val canAskAccess = false
    override fun askAccess() {}
    override fun refresh() = false
    override val message: Pair<String, String>? = null
    override fun removableOld(): String? = null
    override fun removeOld() = false
}

/**
 * The same logic on every platform: the Zimam folder when the system allows it, otherwise [privateRoot]. Switching
 * to the Zimam folder copies the private one across, checks every file, and keeps the private folder as a backup;
 * if any check fails the private folder stays in use. Nothing is deleted except by [removeOld], after a fresh check.
 */
class FolderFiles(
    private val fs: FileSystem,
    private val privateRoot: Path,
    private val sharedRoot: Path,
    private val sharedAllowed: () -> Boolean,
    private val canAsk: () -> Boolean = { false },
    private val ask: () -> Unit = {},
) : AppFiles {
    private val migration = FolderMigration(fs)
    private var root: Path = privateRoot
    override var message: Pair<String, String>? = null
        private set

    init { refresh() }

    override val location: String get() = root.toString()
    override val shared: Boolean get() = root == sharedRoot
    override val canAskAccess: Boolean get() = canAsk()
    override fun askAccess() = ask()

    override fun refresh(): Boolean {
        message = null
        var target = privateRoot
        if (sharedAllowed() && runCatching { fs.createDirectories(sharedRoot) }.isSuccess) {
            target = sharedRoot
            if (fs.exists(privateRoot) && privateRoot != sharedRoot) {
                val r = migration.copyAndVerify(privateRoot, sharedRoot, "Private")
                if (!r.ok) {
                    message = privateRoot.toString() to sharedRoot.toString()
                    target = privateRoot
                }
            }
        }
        val changed = target != root
        root = target
        return changed
    }

    override fun load(name: String): FileLoad {
        val file = root / name
        return try {
            if (fs.exists(file)) FileLoad.Loaded(fs.read(file) { readUtf8() }) else FileLoad.Missing
        } catch (e: Exception) {
            keepCorruptCopy(name)
            FileLoad.Unreadable
        }
    }

    /** Keeps the first unreadable version beside the original as `name.corrupt-1`, if it can be copied at all. */
    private fun keepCorruptCopy(name: String) {
        runCatching {
            val taken = fs.list(root).any { it.name.startsWith("$name.corrupt-") }
            if (!taken) fs.copy(root / name, root / "$name.corrupt-1")
        }
    }

    /** Writes through a temporary file and keeps the previous version as `name.bak`. */
    override fun write(name: String, text: String): Boolean = try {
        fs.createDirectories(root)
        val file = root / name
        val part = root / "$name.part"
        fs.write(part) { writeUtf8(text) }
        if (fs.exists(file)) fs.copy(file, root / "$name.bak")
        fs.atomicMove(part, file)
        true
    } catch (e: Exception) {
        false
    }

    override fun removableOld(): String? =
        if (shared && fs.exists(privateRoot) && privateRoot != sharedRoot && migration.safeToRemove(privateRoot, sharedRoot, "Private")) privateRoot.toString() else null

    override fun removeOld(): Boolean {
        if (removableOld() == null) return false
        return runCatching { fs.deleteRecursively(privateRoot); true }.getOrDefault(false)
    }
}
