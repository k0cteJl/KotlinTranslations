package io.github.k0ctejl.translations

/**
 * A key-set mismatch between [locale] and the [Translator]'s default locale, as reported by
 * [Translator.validate].
 *
 * @property missingKeys keys defined for the default locale but absent from [locale]
 * @property extraKeys keys defined for [locale] but absent from the default locale
 */
public data class ValidationIssue(
    public val locale: String,
    public val missingKeys: Set<String>,
    public val extraKeys: Set<String>
)
