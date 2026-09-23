@file:JvmName("Audiences")

package io.github.k0ctejl.translations.paper

import net.kyori.adventure.audience.Audience

/**
 * Renders [key] via [translator] and sends it to this [Audience] as a rich-text component. Each
 * of [args] is wrapped in a text component automatically unless it's already a `ComponentLike`.
 */
public fun Audience.sendTranslation(translator: ComponentTranslator, key: String, vararg args: Any?): Unit =
    sendMessage(translator.render(key, *args))

/** Same as [sendTranslation], but for an explicit [locale] rather than the translator's default. */
public fun Audience.sendTranslationFor(
    translator: ComponentTranslator,
    locale: String,
    key: String,
    vararg args: Any?
): Unit = sendMessage(translator.renderFor(locale, key, *args))

/** Renders each line of [key] (see [ComponentTranslator.renderLines]) and sends them as separate messages. */
public fun Audience.sendTranslationLines(translator: ComponentTranslator, key: String, vararg args: Any?) {
    for (line in translator.renderLines(key, *args)) sendMessage(line)
}

/** Same as [sendTranslationLines], but for an explicit [locale] rather than the translator's default. */
public fun Audience.sendTranslationLinesFor(
    translator: ComponentTranslator,
    locale: String,
    key: String,
    vararg args: Any?
) {
    for (line in translator.renderLinesFor(locale, key, *args)) sendMessage(line)
}

/**
 * Same as [sendTranslation], but using [ComponentTranslator.default] instead of taking one
 * explicitly. Throws [IllegalStateException] if nothing has called `.makeDefault()` on a
 * [ComponentTranslator] yet.
 */
public fun Audience.sendTranslation(key: String, vararg args: Any?): Unit =
    sendTranslation(ComponentTranslator.requireDefault(), key, *args)

/** Same as [sendTranslationFor], but using [ComponentTranslator.default]. */
public fun Audience.sendTranslationFor(locale: String, key: String, vararg args: Any?): Unit =
    sendTranslationFor(ComponentTranslator.requireDefault(), locale, key, *args)

/** Same as [sendTranslationLines], but using [ComponentTranslator.default]. */
public fun Audience.sendTranslationLines(key: String, vararg args: Any?): Unit =
    sendTranslationLines(ComponentTranslator.requireDefault(), key, *args)

/** Same as [sendTranslationLinesFor], but using [ComponentTranslator.default]. */
public fun Audience.sendTranslationLinesFor(locale: String, key: String, vararg args: Any?): Unit =
    sendTranslationLinesFor(ComponentTranslator.requireDefault(), locale, key, *args)
