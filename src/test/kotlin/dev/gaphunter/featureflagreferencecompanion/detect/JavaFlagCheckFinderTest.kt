package dev.gaphunter.featureflagreferencecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaFlagCheckFinderTest : BasePlatformTestCase() {

    fun `test finds a simple isEnabled call`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    boolean on = client.isEnabled("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        val hits = JavaFlagCheckFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("checkout_v2", hits[0].key)
    }

    fun `test finds multiple recognized method names`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    client.isEnabled("a");
                    client.boolVariation("b", false);
                    client.getFlag("c");
                    client.getBooleanFlag("d");
                    client.variation("e", "off");
                    client.isOn("f");
                    client.evaluateFlag("g");
                    client.isFeatureEnabled("h");
                    client.isFlagEnabled("i");
                }
            }
            """.trimIndent(),
        )
        val hits = JavaFlagCheckFinder.findAll(file)
        assertEquals(setOf("a", "b", "c", "d", "e", "f", "g", "h", "i"), hits.map { it.key }.toSet())
    }

    fun `test ignores unrelated method calls`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    System.out.println("checkout_v2");
                    Object o = repository.getValue("checkout_v2");
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaFlagCheckFinder.findAll(file).isEmpty())
    }

    fun `test only the first string argument is read as the key`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    client.variation("checkout_v2", "not_the_key");
                }
            }
            """.trimIndent(),
        )
        val hits = JavaFlagCheckFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("checkout_v2", hits[0].key)
    }

    fun `test a file with no flag pattern produces no hits and no crash`() {
        val file = myFixture.configureByText(
            "F.java",
            """
            class F {
                void m() {
                    System.out.println("hello");
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaFlagCheckFinder.findAll(file).isEmpty())
    }
}
