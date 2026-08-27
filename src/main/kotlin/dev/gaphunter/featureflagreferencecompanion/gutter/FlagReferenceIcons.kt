package dev.gaphunter.featureflagreferencecompanion.gutter

import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * The three fixed gutter icons this plugin ever shows. Reuses the
 * platform's own bundled icons rather than a custom `Icon`
 * implementation -- same precedent as `unused-npm-script-companion`'s
 * `UnusedNpmScriptIcons` (icon members confirmed to exist on the pinned
 * platform version via `javap` against `AllIcons$General`, not guessed).
 */
object FlagReferenceIcons {
    /** totalReferenceCount >= 2: at least one other call site shares this key. */
    val IN_USE: Icon = AllIcons.General.InspectionsOKEmpty

    /** totalReferenceCount == 1: no other call site anywhere in the project uses this key -- orphan candidate. */
    val ORPHAN_CANDIDATE: Icon = AllIcons.General.InspectionsWarningEmpty

    /**
     * The index has never been refreshed this session -- shown instead
     * of a verdict icon so a stale/empty count is never presented as a
     * real answer. Reuses [AllIcons.Actions.Refresh] (confirmed already
     * in real use in this catalog by `circular-dependency-companion`'s
     * own refresh button) rather than an unconfirmed icon member --
     * also doubles as a visual hint of the fix (run the refresh action).
     */
    val NOT_YET_SCANNED: Icon = AllIcons.Actions.Refresh
}
