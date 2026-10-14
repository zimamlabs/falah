package dev.falah.core

import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.blackholeSink
import okio.buffer

/**
 * Copies an old data folder into the new one without ever losing anything: every file goes through a `.part` file and
 * is renamed into place, a file already in the destination is never overwritten (when it differs, the old copy is kept
 * beside it as `name (from Old).ext`), and the result is checked by file count, size and SHA-256. The old folder is
 * never touched here; it stays as a backup until the user asks to remove it, and removal re-checks first.
 */
class FolderMigration(private val fs: FileSystem) {
    data class Result(val ok: Boolean, val copied: Int, val kept: Int, val problems: List<String>)

    fun sha256(file: Path): String = HashingSource.sha256(fs.source(file)).use { h ->
        h.buffer().readAll(blackholeSink())
        h.hash.hex()
    }

    private fun size(file: Path): Long = fs.metadata(file).size ?: -1L

    private fun files(root: Path): List<Path> =
        if (!fs.exists(root)) emptyList() else fs.listRecursively(root).filter { fs.metadata(it).isRegularFile && !it.name.endsWith(".part") }.toList()

    private fun stem(name: String) = name.lastIndexOf('.').let { if (it > 0) name.substring(0, it) else name }
    private fun ext(name: String) = name.lastIndexOf('.').let { if (it > 0) name.substring(it) else "" }

    /** Where a clashing old file is kept: `settings.properties` becomes `settings (from Old).properties`, then `(from Old 2)` if that is taken too. */
    fun clashName(dest: Path, label: String): Path {
        var n = 1
        while (true) {
            val candidate = dest.parent!! / "${stem(dest.name)} (from $label${if (n > 1) " $n" else ""})${ext(dest.name)}"
            if (!fs.exists(candidate)) return candidate
            n++
        }
    }

    /** Finds where [file] of [source] ended up under [target]: the same path, or a kept clash copy next to it. */
    private fun landing(source: Path, file: Path, target: Path, label: String): Path? {
        val dest = target / file.relativeTo(source).toString()
        val hash = sha256(file)
        if (fs.exists(dest) && size(dest) == size(file) && sha256(dest) == hash) return dest
        val folder = dest.parent ?: return null
        if (!fs.exists(folder)) return null
        return fs.list(folder).firstOrNull {
            fs.metadata(it).isRegularFile && it.name.startsWith("${stem(dest.name)} (from $label") && it.name.endsWith(ext(dest.name)) && size(it) == size(file) && sha256(it) == hash
        }
    }

    /** What is wrong, if anything: a file with no verified counterpart in [target]. Empty means the copy is complete. */
    fun verify(source: Path, target: Path, label: String): List<String> =
        files(source).mapNotNull { f -> if (landing(source, f, target, label) == null) "missing or different: ${f.relativeTo(source)}" else null }

    fun copyAndVerify(source: Path, target: Path, label: String): Result {
        if (!fs.exists(source) || source == target) return Result(true, 0, 0, emptyList())
        var copied = 0
        var kept = 0
        val problems = mutableListOf<String>()
        try {
            fs.createDirectories(target)
            for (f in files(source)) {
                if (landing(source, f, target, label) != null) continue
                var dest = target / f.relativeTo(source).toString()
                dest.parent?.let { fs.createDirectories(it) }
                if (fs.exists(dest)) { dest = clashName(dest, label); kept++ }
                val part = dest.parent!! / (dest.name + ".part")
                fs.copy(f, part)
                fs.atomicMove(part, dest)
                copied++
            }
        } catch (e: Exception) {
            problems += "copy stopped: ${e.message ?: e::class.simpleName}"
        }
        problems += verify(source, target, label)
        return Result(problems.isEmpty(), copied, kept, problems)
    }

    /** True only when every file of [old] is verified in [target] right now. */
    fun safeToRemove(old: Path, target: Path, label: String): Boolean =
        fs.exists(old) && fs.exists(target) && verify(old, target, label).isEmpty()
}
