package dev.gaphunter.featureflagreferencecompanion.detect

import com.intellij.psi.PsiFile
import dev.gaphunter.featureflagreferencecompanion.model.FlagCheckCall
import dev.gaphunter.featureflagreferencecompanion.scan.FlagMethodNames
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Kotlin counterpart of [JavaFlagCheckFinder]. Same "match by simple
 * call name only" contract -- see that class's doc comment for why.
 *
 * Handles both `isEnabled("checkout_v2")` (plain call) and
 * `client.isEnabled("checkout_v2")` (qualified/dot-call) the same way a
 * receiver never gates the match, only the callee's own simple name.
 * The flag key is read from the first value argument's string-template
 * expression -- only accepted when it's a plain literal (no
 * `${...}` interpolation), matching this plugin's stated "exact text,
 * never a computed value" scope (see [FlagMethodNames]/README).
 */
object KotlinFlagCheckFinder {

    fun findAll(file: PsiFile): List<FlagCheckCall> {
        val hits = mutableListOf<FlagCheckCall>()
        val fileUrl = file.virtualFile?.url ?: return emptyList()

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                hitFor(expression, fileUrl)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(call: KtCallExpression, fileUrl: String): FlagCheckCall? {
        val calleeName = calleeSimpleName(call) ?: return null
        if (!FlagMethodNames.matches(calleeName)) return null

        val firstArg: KtValueArgument = call.valueArguments.firstOrNull() ?: return null
        val literal = firstArg.getArgumentExpression() as? KtStringTemplateExpression ?: return null
        val key = plainLiteralTextOf(literal) ?: return null

        return FlagCheckCall(key = key, fileUrl = fileUrl, literalStartOffset = literal.textRange.startOffset)
    }

    /** `isEnabled(...)` -> "isEnabled"; `client.isEnabled(...)` -> "isEnabled" (receiver ignored, see class doc). */
    private fun calleeSimpleName(call: KtCallExpression): String? {
        return when (val callee = call.calleeExpression) {
            is KtSimpleNameExpression -> callee.getReferencedName()
            else -> null
        }
    }

    /**
     * Returns the literal text only when the whole template is a single
     * plain entry with no interpolation (`"checkout_v2"`, never
     * `"checkout_${env}"`) -- an interpolated key isn't a fixed,
     * exact-text-matchable flag name, so this plugin honestly declines
     * to guess at one. Reuses [KtStringTemplateExpression.tryEvaluateConstant]-
     * equivalent logic by hand: a template with exactly one
     * `KtLiteralStringTemplateEntry` child is a plain literal.
     */
    private fun plainLiteralTextOf(template: KtStringTemplateExpression): String? {
        val entries = template.entries
        if (entries.size != 1) return null
        val entry = entries[0]
        if (entry.javaClass.simpleName != "KtLiteralStringTemplateEntry") return null
        return entry.text
    }
}
