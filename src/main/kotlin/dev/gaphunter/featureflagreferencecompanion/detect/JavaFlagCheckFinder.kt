package dev.gaphunter.featureflagreferencecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import dev.gaphunter.featureflagreferencecompanion.model.FlagCheckCall
import dev.gaphunter.featureflagreferencecompanion.scan.FlagMethodNames

/**
 * Finds Java flag-check call sites: a string-literal argument of a call
 * whose simple method name matches [FlagMethodNames] --
 * `isEnabled("checkout_v2")`, `client.boolVariation("checkout_v2", user,
 * false)`, etc. Same name-based (not resolved-symbol-based) contract as
 * `http-status-inline-companion`'s `JavaHttpStatusFinder`: matching by
 * simple method name works the same whether LaunchDarkly, Unleash,
 * ConfigCat, or an in-house wrapper is actually on the classpath, and
 * avoids the cost of resolving every call on a pass that (for the
 * project-wide reference count) already has to visit every file.
 *
 * Deliberately only looks at the call's **first** string-literal
 * argument -- every SDK method covered by [FlagMethodNames] takes the
 * flag key as its first parameter, so this avoids matching an unrelated
 * later string argument (a default value, a user segment id) as if it
 * were the key.
 */
object JavaFlagCheckFinder {

    fun findAll(file: PsiFile): List<FlagCheckCall> {
        val hits = mutableListOf<FlagCheckCall>()
        val fileUrl = file.virtualFile?.url ?: return emptyList()

        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is PsiMethodCallExpression) {
                    hitFor(element, fileUrl)?.let { hits += it }
                }
            }
        })
        return hits
    }

    private fun hitFor(call: PsiMethodCallExpression, fileUrl: String): FlagCheckCall? {
        val methodName = call.methodExpression.referenceName ?: return null
        if (!FlagMethodNames.matches(methodName)) return null

        val firstArg = call.argumentList.expressions.firstOrNull() ?: return null
        val key = (firstArg as? PsiLiteralExpression)?.value as? String ?: return null

        return FlagCheckCall(key = key, fileUrl = fileUrl, literalStartOffset = firstArg.textRange.startOffset)
    }
}
