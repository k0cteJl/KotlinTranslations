package io.github.k0ctejl.translations

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class TranslatorDefaultTest {

    @AfterTest
    fun resetDefault() {
        Translator.default = null
    }

    @Test
    fun `default is null until something sets it`() {
        assertNull(Translator.default)
    }

    @Test
    fun `makeDefault registers the instance and returns it for chaining`() {
        val translator = Translator.create("en")
            .loadLanguage("en", mapOf("greeting" to "Hi"))
            .makeDefault()

        assertSame(translator, Translator.default)
    }

    @Test
    fun `requireDefault returns the registered default`() {
        val translator = Translator.create("en")
            .loadLanguage("en", mapOf("greeting" to "Hi, {0}!"))
            .loadLanguage("ru", mapOf("greeting" to "Привет, {0}!"))
            .makeDefault()

        assertSame(translator, Translator.requireDefault())
        assertEquals("Hi, Bob!", Translator.requireDefault().translate("greeting", "Bob"))
        assertEquals("Привет, Bob!", Translator.requireDefault().translateFor("ru", "greeting", "Bob"))
    }

    @Test
    fun `requireDefault sees translateLines through the registered default`() {
        Translator.create("en")
            .loadLanguage("en", mapOf("motd" to "Line one\nLine two"))
            .makeDefault()

        assertEquals(listOf("Line one", "Line two"), Translator.requireDefault().translateLines("motd"))
        assertEquals(listOf("Line one", "Line two"), Translator.requireDefault().translateLinesFor("en", "motd"))
    }

    @Test
    fun `requireDefault throws when no default is registered`() {
        assertFailsWith<IllegalStateException> { Translator.requireDefault() }
    }
}
