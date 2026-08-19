package dev.gaphunter.featureflagreferencecompanion.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import dev.gaphunter.featureflagreferencecompanion.detect.JavaFlagCheckFinder
import dev.gaphunter.featureflagreferencecompanion.detect.KotlinFlagCheckFinder
import dev.gaphunter.featureflagreferencecompanion.model.FlagCheckCall
import dev.gaphunter.featureflagreferencecompanion.model.FlagReferenceResult

/**
 * Project-level, **manually refreshed** cache of every flag-check call
 * site found across the whole open project, plus the derived
 * [FlagReferenceResult] per key. Same "heavy computation stays off the
 * hot path, refreshed on an explicit action, never recomputed on every
 * keystroke" principle already used by `circular-dependency-companion`'s
 * `ProjectGraphAnalyzer` -- a project-wide scan is real work (every Java
 * and Kotlin file in the project), so it must never run inside the
 * daemon's per-keystroke highlighting pass. [dev.gaphunter.featureflagreferencecompanion.gutter.FlagReferenceLineMarkerProvider]
 * only ever *reads* [resultFor]; only [dev.gaphunter.featureflagreferencecompanion.action.RefreshFlagReferencesAction]
 * (or the initial empty state) ever calls [refresh].
 *
 * Deliberately holds derived, serializable data ([FlagCheckCall]/
 * [FlagReferenceResult]), never a raw `PsiElement` -- those aren't safe
 * to keep across read actions (see [FlagCheckCall]'s own doc comment).
 *
 * **Walks the project's own file tree by extension** (`.java`/`.kt`)
 * rather than a platform file-type index -- this catalog has no prior
 * confirmed use of `FileTypeIndex` against a Kotlin-plugin file type,
 * and a plain `ProjectFileIndex.iterateContent` walk needs no such
 * dependency: it only needs `VirtualFile.extension`, already a stable,
 * always-available API.
 */
@Service(Service.Level.PROJECT)
class FlagReferenceIndex(private val project: Project) {

    @Volatile
    private var callsByKey: Map<String, List<FlagCheckCall>> = emptyMap()

    @Volatile
    private var hasRunAtLeastOnce: Boolean = false

    /** True once [refresh] has completed at least once this session -- lets the gutter provider distinguish "never scanned yet" from "scanned, zero flags found". */
    fun hasRunAtLeastOnce(): Boolean = hasRunAtLeastOnce

    /**
     * Re-scans every `.java`/`.kt` file under project content roots and
     * replaces the cached index. Must be called from a read action (the
     * caller -- [dev.gaphunter.featureflagreferencecompanion.action.RefreshFlagReferencesAction] --
     * runs this inside `runReadAction` off the EDT, per `CONSTITUTION.md` §6).
     */
    fun refresh() {
        val calls = mutableListOf<FlagCheckCall>()
        val psiManager = PsiManager.getInstance(project)
        val fileIndex = ProjectFileIndex.getInstance(project)

        fileIndex.iterateContent { virtualFile: VirtualFile ->
            if (!virtualFile.isDirectory) {
                val psiFile = psiManager.findFile(virtualFile)
                when (virtualFile.extension) {
                    "java" -> psiFile?.let { calls += JavaFlagCheckFinder.findAll(it) }
                    "kt" -> psiFile?.let { calls += KotlinFlagCheckFinder.findAll(it) }
                }
            }
            true
        }

        callsByKey = calls.groupBy { it.key }
        hasRunAtLeastOnce = true
    }

    /** Result for the key found at [call], or null if [refresh] has never run or found no call at that exact site. */
    fun resultFor(call: FlagCheckCall): FlagReferenceResult? {
        val callsForKey = callsByKey[call.key] ?: return null
        return FlagReferenceResult(key = call.key, totalReferenceCount = callsForKey.size)
    }

    companion object {
        fun getInstance(project: Project): FlagReferenceIndex = project.getService(FlagReferenceIndex::class.java)
    }
}
