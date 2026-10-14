package dev.falah.app

import dev.falah.core.FolderMigration
import okio.FileSystem
import okio.Path

/** Where Falah keeps the files you can see: its settings copy and the prayer log. */
interface AppFiles {
    fun read(name: String): String?
    fun write(name: String, text: String)
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
    val message: String?
    /** The old private folder, when everything in it is verified in the Zimam folder and it can be removed; otherwise null. */
    fun removableOld(): String?
    fun removeOld(): Boolean
}

/** Files in memory only; used before a platform supplies real ones. */
class MemoryFiles : AppFiles {
    private val data = mutableMapOf<String, String>()
    override fun read(name: String) = data[name]
    override fun write(name: String, text: String) { data[name] = text }
    override val location = "Memory"
    override val shared = false
    override val canAskAccess = false
    override fun askAccess() {}
    override fun refresh() = false
    override val message: String? = null
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
    override var message: String? = null
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
                    message = "Falah could not verify the copy of $privateRoot into $sharedRoot, so it keeps using the old folder and will try again later. Nothing was deleted."
                    target = privateRoot
                }
            }
        }
        val changed = target != root
        root = target
        return changed
    }

    override fun read(name: String): String? = runCatching { if (fs.exists(root / name)) fs.read(root / name) { readUtf8() } else null }.getOrNull()

    /** Writes through a temporary file and keeps the previous version as `name.bak`. */
    override fun write(name: String, text: String) {
        runCatching {
            fs.createDirectories(root)
            val file = root / name
            val part = root / "$name.part"
            fs.write(part) { writeUtf8(text) }
            if (fs.exists(file)) fs.copy(file, root / "$name.bak")
            fs.atomicMove(part, file)
        }
    }

    override fun removableOld(): String? =
        if (shared && fs.exists(privateRoot) && privateRoot != sharedRoot && migration.safeToRemove(privateRoot, sharedRoot, "Private")) privateRoot.toString() else null

    override fun removeOld(): Boolean {
        if (removableOld() == null) return false
        return runCatching { fs.deleteRecursively(privateRoot); true }.getOrDefault(false)
    }
}
