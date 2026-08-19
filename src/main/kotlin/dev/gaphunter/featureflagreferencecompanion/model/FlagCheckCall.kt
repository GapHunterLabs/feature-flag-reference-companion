package dev.gaphunter.featureflagreferencecompanion.model

/**
 * One recognized feature-flag-check call site found in the project's
 * PSI (`isEnabled("checkout_v2")`, `variation("checkout_v2", false)`,
 * ...): the flag [key] it reads plus the exact source-text offset of
 * the key's string-literal leaf token, so a later PSI pass can re-find
 * the same literal to anchor a [com.intellij.codeInsight.daemon.LineMarkerInfo]
 * on it. Deliberately holds no [com.intellij.psi.PsiElement] reference
 * -- those are only valid on the read thread that produced them, and
 * this value is meant to be cached ([dev.gaphunter.featureflagreferencecompanion.scan.FlagReferenceIndex])
 * across read actions.
 */
data class FlagCheckCall(
    val key: String,
    val fileUrl: String,
    val literalStartOffset: Int,
)
