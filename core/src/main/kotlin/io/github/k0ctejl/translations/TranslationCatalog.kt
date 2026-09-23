package io.github.k0ctejl.translations

/** An immutable set of precompiled [MessageTemplate]s for a single locale. */
public class TranslationCatalog internal constructor(
    public val locale: String,
    private val templates: Map<String, MessageTemplate>
) {
    public fun template(key: String): MessageTemplate? = templates[key]

    public fun has(key: String): Boolean = templates.containsKey(key)

    public val keys: Set<String> get() = templates.keys

    public val size: Int get() = templates.size

    public companion object {
        /** Compiles every raw `key -> value` entry into a [TranslationCatalog] for [locale]. */
        @JvmStatic
        public fun of(locale: String, rawEntries: Map<String, String>): TranslationCatalog {
            val compiled = LinkedHashMap<String, MessageTemplate>(rawEntries.size)
            for ((key, value) in rawEntries) {
                compiled[key] = MessageTemplate.compile(value)
            }
            return TranslationCatalog(locale, compiled)
        }
    }
}
