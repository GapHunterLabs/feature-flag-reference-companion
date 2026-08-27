package dev.gaphunter.featureflagreferencecompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import dev.gaphunter.featureflagreferencecompanion.detect.JavaFlagCheckFinder
import dev.gaphunter.featureflagreferencecompanion.detect.KotlinFlagCheckFinder
import dev.gaphunter.featureflagreferencecompanion.index.FlagReferenceIndex
import dev.gaphunter.featureflagreferencecompanion.model.FlagCheckCall
import dev.gaphunter.featureflagreferencecompanion.model.FlagReferenceResult
import dev.gaphunter.featureflagreferencecompanion.model.FlagReferenceVerdict
import dev.gaphunter.featureflagreferencecompanion.review.ReviewPrompt
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Gutter icon on every recognized flag-check call site's key literal,
 * showing whether [FlagReferenceIndex] currently counts it as in use
 * (2+ call sites project-wide) or an orphan candidate (exactly 1: only
 * this call site).
 *
 * **Deliberately reads the cache only -- never runs [FlagReferenceIndex.refresh]
 * itself.** A project-wide scan is real, non-trivial work; running it
 * inside `collectSlowLineMarkers` (which the daemon re-invokes on every
 * edit of a visited file) would silently reintroduce the exact
 * per-keystroke cost this plugin's manual-refresh design exists to
 * avoid. Until the user runs [dev.gaphunter.featureflagreferencecompanion.action.RefreshFlagReferencesAction]
 * at least once, every call site shows [FlagReferenceIcons.NOT_YET_SCANNED]
 * instead of guessing at a verdict.
 *
 * **Leaf-anchored from the start**: the element
 * the daemon actually visits is a string-literal leaf/composite
 * depending on language (`PsiLiteralExpression` in Java is already a
 * leaf-ish token; `KtStringTemplateExpression` in Kotlin is a composite
 * with its own child structure), so [buildMarker] always anchors the
 * [LineMarkerInfo] on the innermost real leaf child, never the
 * composite node itself, exactly like `unused-npm-script-companion`'s
 * `UnusedNpmScriptLineMarkerProvider` already proved for `JsonStringLiteral`.
 */
class FlagReferenceLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "Feature flag references"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val project = file.project
        val index = FlagReferenceIndex.getInstance(project)

        val calls = callsInFile(file)
        if (calls.isEmpty()) return

        val callsByOffset = calls.associateBy { it.literalStartOffset }

        for (element in elements) {
            val literalKeyStart = literalKeyStartOffsetOf(element) ?: continue
            val call = callsByOffset[literalKeyStart] ?: continue
            val leaf = leafAnchorOf(element) ?: continue
            val verdictResult = if (index.hasRunAtLeastOnce()) index.resultFor(call) else null
            result.add(buildMarker(leaf, element.textRange, call, verdictResult))

            // Only an orphan-candidate verdict is a real, actionable
            // finding -- IN_USE/NOT_YET_SCANNED just show informational
            // status for every flag check site, which would inflate the
            // CTA counter on ordinary, healthy code.
            if (verdictResult?.verdict == FlagReferenceVerdict.ORPHAN_CANDIDATE) {
                val path = file.virtualFile?.path ?: continue
                val lineNumber = file.viewProvider.document?.getLineNumber(leaf.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(project, "$path:$lineNumber:${call.key}")
            }
        }
    }

    /** Re-detects call sites in just this one already-open file (cheap: one file, not the whole project) so the marker pass knows which literals are real flag-check key literals, independent of whether the project-wide index has been refreshed yet. */
    private fun callsInFile(file: PsiFile): List<FlagCheckCall> {
        return when (file.language.id) {
            "JAVA" -> JavaFlagCheckFinder.findAll(file)
            "kotlin" -> KotlinFlagCheckFinder.findAll(file)
            else -> emptyList()
        }
    }

    /** The offset [FlagCheckCall.literalStartOffset] would have recorded for this element, if it's the same kind of key-literal node the finders look for -- used only to match elements the daemon visits against the calls already found in [callsInFile]. */
    private fun literalKeyStartOffsetOf(element: PsiElement): Int? {
        return when (element) {
            is PsiLiteralExpression -> element.textRange.startOffset
            is KtStringTemplateExpression -> element.textRange.startOffset
            else -> null
        }
    }

    /** The real leaf PSI token to anchor the [LineMarkerInfo] on -- see class doc above. */
    private fun leafAnchorOf(element: PsiElement): PsiElement? {
        var leaf = element
        while (leaf.firstChild != null) {
            leaf = leaf.firstChild
        }
        return leaf
    }

    private fun buildMarker(
        leaf: PsiElement,
        visualRange: com.intellij.openapi.util.TextRange,
        call: FlagCheckCall,
        verdictResult: FlagReferenceResult?,
    ): LineMarkerInfo<PsiElement> {
        val icon = when {
            verdictResult == null -> FlagReferenceIcons.NOT_YET_SCANNED
            verdictResult.verdict == FlagReferenceVerdict.ORPHAN_CANDIDATE -> FlagReferenceIcons.ORPHAN_CANDIDATE
            else -> FlagReferenceIcons.IN_USE
        }
        val tooltip = tooltipFor(call, verdictResult)
        return LineMarkerInfo(
            leaf,
            visualRange,
            icon,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }

    private fun tooltipFor(call: FlagCheckCall, verdictResult: FlagReferenceResult?): String {
        if (verdictResult == null) {
            return "Feature flag \"${call.key}\": run \"Refresh Feature Flag References\" to count project-wide references"
        }
        val count = verdictResult.totalReferenceCount
        return when (verdictResult.verdict) {
            FlagReferenceVerdict.ORPHAN_CANDIDATE ->
                "Feature flag \"${call.key}\": orphan candidate -- only this call site references it anywhere in the project"
            FlagReferenceVerdict.IN_USE ->
                "Feature flag \"${call.key}\": referenced $count times across the project"
        }
    }
}
