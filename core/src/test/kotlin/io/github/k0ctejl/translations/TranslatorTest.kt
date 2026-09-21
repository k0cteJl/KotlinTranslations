package io.github.k0ctejl.translations

import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun `loadLanguagesFromDirectory registers a locale per lang file`() {
        val dir = createTempDirectory("kotlintranslations-test")
        try {
            (dir / "en.lang").writeText("""greeting="Hello, {0}!"""")
            (dir / "ru.lang").writeText("""greeting="Привет, {0}!"""")
            (dir / "notes.txt").writeText("this is not a .lang file")

            val t = Translator.create("en").loadLanguagesFromDirectory(dir)

            assertEquals(setOf("en", "ru"), t.availableLocales())
            assertEquals("Привет, Bob!", t.translateFor("ru", "greeting", "Bob"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `loadLanguagesFromDirectory throws when given a file instead of a directory`() {
        val dir = createTempDirectory("kotlintranslations-test")
        val file = dir / "en.lang"
        try {
            file.writeText("""greeting="Hello!"""")
            assertFailsWith<IllegalArgumentException> {
                Translator.create("en").loadLanguagesFromDirectory(file)
            }
        } finally {
            file.deleteExisting()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `validate reports no issues when locales share the same keys`() {
        val t = Translator.create("en")
            .loadLanguage("en", mapOf("greeting" to "Hi", "farewell" to "Bye"))
            .loadLanguage("ru", mapOf("greeting" to "Привет", "farewell" to "Пока"))

        assertTrue(t.validate().isEmpty())
    }

    @Test
    fun `validate reports missing and extra keys relative to the default locale`() {
        val t = translator() // en: greeting, farewell | ru: greeting only
            .loadLanguage("de", mapOf("greeting" to "Hallo", "farewell" to "Tschuss", "extra" to "Zusatz"))

        val issues = t.validate()

        assertEquals(listOf("de", "ru"), issues.map { it.locale })

        val de = issues.first { it.locale == "de" }
        assertTrue(de.missingKeys.isEmpty())
        assertEquals(setOf("extra"), de.extraKeys)

        val ru = issues.first { it.locale == "ru" }
        assertEquals(setOf("farewell"), ru.missingKeys)
        assertTrue(ru.extraKeys.isEmpty())
    }
}
