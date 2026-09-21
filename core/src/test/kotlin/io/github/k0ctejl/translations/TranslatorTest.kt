package io.github.k0ctejl.translations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslatorTest {

    private fun translator(): Translator =
        Translator.create(defaultLocale = "en")
            .loadLanguage("en", mapOf("greeting" to "Hello, {0}!", "farewell" to "Bye"))
            .loadLanguage("ru", mapOf("greeting" to "Привет, {0}!"))

    @Test
    fun `translates using the default locale`() {
        assertEquals("Hello, Bob!", translator().translate("greeting", "Bob"))
    }

    @Test
    fun `translates for an explicit locale`() {
        assertEquals("Привет, Bob!", translator().translateFor("ru", "greeting", "Bob"))
    }

    @Test
    fun `falls back to the default locale when the key is missing in the requested locale`() {
        assertEquals("Bye", translator().translateFor("ru", "farewell"))
    }

    @Test
    fun `returns the raw key when undefined everywhere`() {
        assertEquals("nonexistent.key", translator().translate("nonexistent.key"))
    }

    @Test
    fun `hasTranslation respects locale fallback`() {
        val t = translator()
        assertTrue(t.hasTranslation("farewell", "ru"))
        assertFalse(t.hasTranslation("nonexistent.key"))
    }

    @Test
    fun `unloadLanguage removes a catalog`() {
        val t = translator()
        t.unloadLanguage("ru")
        assertFalse(t.isLoaded("ru"))
        assertEquals("Hello, Bob!", t.translateFor("ru", "greeting", "Bob"))
    }

    @Test
    fun `availableLocales reflects loaded catalogs`() {
        assertEquals(setOf("en", "ru"), translator().availableLocales())
    }

    @Test
    fun `defaultLocale can be changed at runtime`() {
        val t = translator()
        t.defaultLocale = "ru"
        assertEquals("Привет, Bob!", t.translate("greeting", "Bob"))
    }
}
