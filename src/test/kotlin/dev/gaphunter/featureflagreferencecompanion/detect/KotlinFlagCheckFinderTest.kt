package dev.gaphunter.featureflagreferencecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinFlagCheckFinderTest : BasePlatformTestCase() {

    fun `test finds a simple isEnabled call`() {
        val file = myFixture.configureByText(
            "F.kt",
            """
            class F {
                fun m() {
                    val on = client.isEnabled("checkout_v2")
                }
            }
            """.trimIndent(),
        )
        val hits = KotlinFlagCheckFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("checkout_v2", hits[0].key)
    }

    fun `test finds an unqualified call with no receiver`() {
        val file = myFixture.configureByText(
            "F.kt",
            """
            class F {
                fun m() {
                    val on = isEnabled("checkout_v2")
                }
            }
            """.trimIndent(),
        )
        val hits = KotlinFlagCheckFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("checkout_v2", hits[0].key)
    }

    fun `test ignores unrelated calls`() {
        val file = myFixture.configureByText(
            "F.kt",
            """
            class F {
                fun m() {
                    println("checkout_v2")
                    val o = repository.getValue("checkout_v2")
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinFlagCheckFinder.findAll(file).isEmpty())
    }

    fun `test an interpolated string key is not treated as an exact literal`() {
        val file = myFixture.configureByText(
            "F.kt",
            """
            class F {
                fun m(env: String) {
                    val on = client.isEnabled("checkout_${'$'}env")
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinFlagCheckFinder.findAll(file).isEmpty())
    }

    fun `test a file with no flag pattern produces no hits and no crash`() {
        val file = myFixture.configureByText(
            "F.kt",
            """
            class F {
                fun m() {
                    println("hello")
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinFlagCheckFinder.findAll(file).isEmpty())
    }
}
