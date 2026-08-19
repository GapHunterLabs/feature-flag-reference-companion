package dev.gaphunter.featureflagreferencecompanion.index

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.featureflagreferencecompanion.detect.JavaFlagCheckFinder

class FlagReferenceIndexTest : BasePlatformTestCase() {

    fun `test refresh counts a single call site as one reference`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    client.isEnabled("solo_flag");
                }
            }
            """.trimIndent(),
        )
        val index = FlagReferenceIndex.getInstance(project)
        index.refresh()
        assertTrue(index.hasRunAtLeastOnce())

        val call = JavaFlagCheckFinder.findAll(file).single()
        val result = index.resultFor(call)
        assertEquals(1, result?.totalReferenceCount)
    }

    fun `test refresh counts call sites across multiple files`() {
        myFixture.addFileToProject(
            "Other.java",
            """
            class Other {
                void m() {
                    client.isEnabled("shared_flag");
                }
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    client.isEnabled("shared_flag");
                }
            }
            """.trimIndent(),
        )
        val index = FlagReferenceIndex.getInstance(project)
        index.refresh()

        val call = JavaFlagCheckFinder.findAll(file).single()
        val result = index.resultFor(call)
        assertEquals(2, result?.totalReferenceCount)
    }

    fun `test refresh matches by exact key text not substring`() {
        myFixture.addFileToProject(
            "Other.java",
            """
            class Other {
                void m() {
                    client.isEnabled("checkout_v2_extended");
                }
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        val index = FlagReferenceIndex.getInstance(project)
        index.refresh()

        val call = JavaFlagCheckFinder.findAll(file).single()
        val result = index.resultFor(call)
        assertEquals(1, result?.totalReferenceCount)
    }
}
