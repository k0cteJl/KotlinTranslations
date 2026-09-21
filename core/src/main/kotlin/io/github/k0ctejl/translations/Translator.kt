package io.github.k0ctejl.translations

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry of [TranslationCatalog]s keyed by locale, and the main entry point for
 * looking up and formatting translated strings.
 *
 * Loading a language file (`loadLanguage`) parses and compiles it once; [translate] never
 * touches the parser or runs a regex - it only walks the already-compiled
 * [MessageTemplate.segments], making it cheap enough to call on a hot path such as a chat or
 * command handler.
 */
public class Translator private constructor(@Volatile public var defaultLocale: String) {

    private val catalogs = ConcurrentHashMap<String, TranslationCatalog>()

    /** Registers a catalog for [locale] built directly from already-parsed `key -> value` entries. */
    public fun loadLanguage(locale: String, rawEntries: Map<String, String>): Translator {
        catalogs[locale] = TranslationCatalog.of(locale, rawEntries)
        return this
    }

    /** Parses [input] as a `.lang` file and registers it for [locale]. */
    @JvmOverloads
    public fun loadLanguage(locale: String, input: InputStream, charset: Charset = StandardCharsets.UTF_8): Translator =
        loadLanguage(locale, LangFileParser.parse(input, charset))

    /** Parses the `.lang` file at [path] and registers it for [locale]. */
    @JvmOverloads
    public fun loadLanguage(locale: String, path: Path, charset: Charset = StandardCharsets.UTF_8): Translator =
        loadLanguage(locale, LangFileParser.parse(path, charset))

    /** Removes the catalog for [locale], if any. */
    public fun unloadLanguage(locale: String): Translator {
        catalogs.remove(locale)
        return this
    }

    /** The set of locales currently loaded. */
    public fun availableLocales(): Set<String> = catalogs.keys

    public fun isLoaded(locale: String): Boolean = catalogs.containsKey(locale)

    /** Whether [key] is defined for [locale], falling back to [defaultLocale]. */
    @JvmOverloads
    public fun hasTranslation(key: String, locale: String = defaultLocale): Boolean =
        catalogs[locale]?.has(key) == true || catalogs[defaultLocale]?.has(key) == true

    /** The compiled template for [key] in [locale], falling back to [defaultLocale], or `null` if undefined in both. */
    @JvmOverloads
    public fun template(key: String, locale: String = defaultLocale): MessageTemplate? =
        catalogs[locale]?.template(key) ?: catalogs[defaultLocale]?.template(key)

    /**
     * Translates [key] using [defaultLocale], substituting `{0}`, `{1}`, ... with [args].
     * Returns [key] itself if it isn't defined for [defaultLocale] either.
     */
    public fun translate(key: String, vararg args: Any?): String =
        template(key, defaultLocale)?.format(*args) ?: key

    /**
     * Translates [key] for [locale], falling back to [defaultLocale] and finally to the raw
     * key if undefined in both.
     */
    public fun translateFor(locale: String, key: String, vararg args: Any?): String =
        template(key, locale)?.format(*args) ?: key

    public companion object {
        /** Creates an empty [Translator]; call [loadLanguage] to populate it. */
        @JvmStatic
        @JvmOverloads
        public fun create(defaultLocale: String = "en"): Translator = Translator(defaultLocale)
    }
}
