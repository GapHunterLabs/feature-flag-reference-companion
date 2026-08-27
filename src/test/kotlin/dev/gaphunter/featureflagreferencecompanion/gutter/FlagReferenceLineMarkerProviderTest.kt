package dev.gaphunter.featureflagreferencecompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.featureflagreferencecompanion.index.FlagReferenceIndex

/**
 * End-to-end: real PSI + real daemon slow-line-marker pass (same
 * `DaemonCodeAnalyzerImpl.getLineMarkers` contract already proven by
 * `unused-npm-script-companion`'s equivalent test), confirming the
 * provider is actually *wired up* for Java and Kotlin files, and that
 * it correctly reflects [FlagReferenceIndex]'s manually-refreshed state
 * rather than recomputing anything itself.
 *
 * Markers are matched by [LineMarkerInfo.getLineMarkerTooltip] (which
 * always contains the full flag key, per [FlagReferenceLineMarkerProvider.tooltipFor])
 * rather than by the marker's anchor `element.text` -- the anchor is
 * deliberately a **leaf** PSI token, which for a
 * Kotlin `KtStringTemplateExpression` is just the innermost quote/content
 * token, not the whole literal's text the way a Java `PsiLiteralExpression`
 * leaf happens to be.
 */
class FlagReferenceLineMarkerProviderTest : BasePlatformTestCase() {

    private fun collectMarkers(): List<LineMarkerInfo<*>> {
        myFixture.doHighlighting()
        return DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
    }

    private fun refreshIndex() {
        FlagReferenceIndex.getInstance(project).refresh()
    }

    /** Finds the one marker whose tooltip is about exactly this flag key (quoted, so "checkout_v2" never matches a marker for "checkout_v2_extended"). */
    private fun markerForKey(key: String): LineMarkerInfo<*> {
        return collectMarkers().first { it.lineMarkerTooltip?.contains("\"$key\"") == true }
    }

    fun `test before any refresh a flag call site shows the not yet scanned icon`() {
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean on = client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        val marker = markerForKey("checkout_v2")
        assertSame(FlagReferenceIcons.NOT_YET_SCANNED, marker.icon)
    }

    fun `test a flag with only one call site is an orphan candidate after refresh`() {
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean on = client.isEnabled("legacy_export_flow");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val marker = markerForKey("legacy_export_flow")
        assertSame(FlagReferenceIcons.ORPHAN_CANDIDATE, marker.icon)
    }

    fun `test a flag with two or more call sites is marked in use after refresh`() {
        myFixture.addFileToProject(
            "OtherUsage.java",
            """
            class OtherUsage {
                void check() {
                    boolean on = client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean on = client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val marker = markerForKey("checkout_v2")
        assertSame(FlagReferenceIcons.IN_USE, marker.icon)
    }

    fun `test exact key match never counts a different key as the same flag`() {
        myFixture.addFileToProject(
            "OtherUsage.java",
            """
            class OtherUsage {
                void check() {
                    boolean on = client.isEnabled("checkout_v2_extended");
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean on = client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val marker = markerForKey("checkout_v2")
        assertSame(FlagReferenceIcons.ORPHAN_CANDIDATE, marker.icon)
    }

    fun `test multiple recognized SDK method names are all detected`() {
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean a = client.isEnabled("flag_a");
                    boolean b = client.boolVariation("flag_b", false);
                    Object c = client.getFlag("flag_c");
                    boolean d = client.evaluateFlag("flag_d");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        for (flag in listOf("flag_a", "flag_b", "flag_c", "flag_d")) {
            val marker = markerForKey(flag)
            assertSame("expected $flag to be an orphan candidate (only 1 call site)", FlagReferenceIcons.ORPHAN_CANDIDATE, marker.icon)
        }
    }

    fun `test kotlin flag calls are detected the same as java`() {
        myFixture.configureByText(
            "FeatureFlags.kt",
            """
            class FeatureFlags {
                fun check() {
                    val on = client.isEnabled("checkout_v2_kt")
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val marker = markerForKey("checkout_v2_kt")
        assertSame(FlagReferenceIcons.ORPHAN_CANDIDATE, marker.icon)
    }

    fun `test a project with no feature flag pattern produces no markers and no crash`() {
        myFixture.configureByText(
            "PlainClass.java",
            """
            class PlainClass {
                void doWork() {
                    System.out.println("hello");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val markers = collectMarkers()
        assertTrue(markers.isEmpty())
    }

    fun `test manual refresh after editing code updates the verdict`() {
        myFixture.configureByText(
            "FeatureFlags.java",
            """
            class FeatureFlags {
                void check() {
                    boolean on = client.isEnabled("rollout_flag");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val beforeMarker = markerForKey("rollout_flag")
        assertSame(FlagReferenceIcons.ORPHAN_CANDIDATE, beforeMarker.icon)

        myFixture.addFileToProject(
            "NewCaller.java",
            """
            class NewCaller {
                void check() {
                    boolean on = client.isEnabled("rollout_flag");
                }
            }
            """.trimIndent(),
        )
        refreshIndex()
        val afterMarker = markerForKey("rollout_flag")
        assertSame(FlagReferenceIcons.IN_USE, afterMarker.icon)
    }
}
