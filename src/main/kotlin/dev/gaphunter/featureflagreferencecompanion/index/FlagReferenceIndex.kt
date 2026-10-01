package dev.gaphunter.featureflagreferencecompanion.index

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
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
     * runs this inside `runReadAction` off the EDT).
     */
    /**
     * Rescans every Java/Kotlin file of the project. One short read action per file, with cancellation between
     * files: before 0.2.2 the whole scan ran inside ONE read action, so on a large project every write action --
     * typing included -- waited for the full scan to finish (a freeze), and the task couldn't be cancelled
     * (found 2026-10-01). [indicator] reports progress and cancels; null uses the current one, if any.
     */
    fun refresh(indicator: ProgressIndicator? = null) {
        val files = ReadAction.compute<List<VirtualFile>, RuntimeException> {
            val found = mutableListOf<VirtualFile>()
            ProjectFileIndex.getInstance(project).iterateContent { virtualFile: VirtualFile ->
                if (!virtualFile.isDirectory && virtualFile.extension in SCANNED_EXTENSIONS) found += virtualFile
                true
            }
            found
        }
        indicator?.isIndeterminate = false
        val calls = mutableListOf<FlagCheckCall>()
        for ((i, virtualFile) in files.withIndex()) {
            if (indicator != null) indicator.checkCanceled() else ProgressManager.checkCanceled()
            indicator?.fraction = i.toDouble() / files.size
            calls += ReadAction.compute<List<FlagCheckCall>, RuntimeException> { callsIn(virtualFile) }
        }
        callsByKey = calls.groupBy { it.key }
        hasRunAtLeastOnce = true
    }

    private fun callsIn(virtualFile: VirtualFile): List<FlagCheckCall> {
        if (!virtualFile.isValid || project.isDisposed) return emptyList()
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()
        return when (virtualFile.extension) {
            "java" -> JavaFlagCheckFinder.findAll(psiFile)
            "kt" -> KotlinFlagCheckFinder.findAll(psiFile)
            else -> emptyList()
        }
    }

    fun resultFor(call: FlagCheckCall): FlagReferenceResult? {
        val callsForKey = callsByKey[call.key] ?: return null
        return FlagReferenceResult(key = call.key, totalReferenceCount = callsForKey.size)
    }

    companion object {
        private val SCANNED_EXTENSIONS = setOf("java", "kt")

        fun getInstance(project: Project): FlagReferenceIndex = project.getService(FlagReferenceIndex::class.java)
    }
}
