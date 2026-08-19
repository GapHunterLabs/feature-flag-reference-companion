package dev.gaphunter.featureflagreferencecompanion.model

/**
 * The gutter verdict for one flag-check call site, derived purely from
 * [totalReferenceCount] (how many flag-check call sites across the
 * whole project use the exact same key -- always includes the call
 * site being annotated itself, so the minimum possible value is 1).
 */
enum class FlagReferenceVerdict {
    /** [totalReferenceCount] == 1: no other call site anywhere in the project uses this key. */
    ORPHAN_CANDIDATE,

    /** [totalReferenceCount] >= 2: at least one other call site shares this key. */
    IN_USE,
}

data class FlagReferenceResult(
    val key: String,
    val totalReferenceCount: Int,
) {
    val verdict: FlagReferenceVerdict
        get() = if (totalReferenceCount <= 1) FlagReferenceVerdict.ORPHAN_CANDIDATE else FlagReferenceVerdict.IN_USE
}
