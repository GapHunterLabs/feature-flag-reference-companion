package dev.gaphunter.featureflagreferencecompanion.scan

/**
 * The fixed set of feature-flag-check method names this plugin
 * recognizes, matched case-insensitively by **simple name only** --
 * never a resolved symbol/type -- so detection works the same whether
 * LaunchDarkly, Unleash, ConfigCat, or an in-house `FeatureFlags`
 * helper is actually on the classpath. Same "match by simple method
 * name, not resolved symbol" principle already proven by
 * `HttpSignalNames`/`SqlSignalNames` elsewhere in this catalog.
 *
 * **Stated honestly, not exhaustively (README repeats this):** this
 * covers the common method names across LaunchDarkly
 * (`boolVariation`, `variation`), Unleash (`isEnabled`), ConfigCat
 * (`getValue` is deliberately NOT included -- too generic a name,
 * see below), Flagsmith (`hasFeatureFlag`, `getFeatureFlag` --
 * specific enough to include, unlike `getValue`: the name itself
 * names the concept, not a generic accessor), and generic/home-grown
 * conventions (`FeatureFlags.isOn(...)`, `isFeatureEnabled`,
 * `isFlagEnabled`, `getFlag`, `evaluateFlag`, `getBooleanFlag`). A
 * project using a differently-named wrapper around one of these SDKs
 * won't be detected -- that's a real, documented limitation, not a
 * bug.
 *
 * **Deliberately excluded:** `getValue`, `get`, `check`, `test`,
 * `is` alone -- names generic enough that matching them by simple
 * name across an entire project would produce far more false
 * positives (unrelated calls with an unrelated first string argument)
 * than real feature-flag detections.
 */
object FlagMethodNames {
    private val NAMES: Set<String> = setOf(
        "isenabled",
        "isfeatureenabled",
        "isflagenabled",
        "getflag",
        "getbooleanflag",
        "variation",
        "boolvariation",
        "ison",
        "evaluateflag",
        "hasfeatureflag",
        "getfeatureflag",
    )

    /** Case-insensitive match against a call's simple method name (no receiver/qualifier). */
    fun matches(simpleMethodName: String): Boolean = simpleMethodName.lowercase() in NAMES
}
