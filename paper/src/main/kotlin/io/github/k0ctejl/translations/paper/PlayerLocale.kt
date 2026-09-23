@file:JvmName("PlayerLocaleExtensions")

package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.Translator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.bukkit.entity.Player

/**
 * Sets an explicit locale override for this player, e.g. from a `/language ru` command,
 * independent of - and preferred over - their client's own Minecraft language setting.
 * Backed by [PlayerLocales]; see it for persistence caveats.
 */
public fun Player.setLocale(locale: String): Unit = PlayerLocales.set(uniqueId, locale)

/** The locale explicitly set via [setLocale], or `null` if none was set. */
public fun Player.localeOverride(): String? = PlayerLocales.get(uniqueId)

/** Removes this player's [setLocale] override, if any; [resolveLocale] then falls back to their client's language. */
public fun Player.clearLocaleOverride(): Unit = PlayerLocales.clear(uniqueId)

/**
 * The locale to use for this player: their [setLocale] override if one is set and loaded in
 * [translator], otherwise their client's own Minecraft language, otherwise [Translator.defaultLocale].
 */
public fun Player.resolveLocale(translator: Translator): String {
    val override = PlayerLocales.get(uniqueId)
    if (override != null && translator.isLoaded(override)) return override

    val clientLanguage = locale().language
    return if (translator.isLoaded(clientLanguage)) clientLanguage else translator.defaultLocale
}

/** Same as [resolveLocale], but using [Translator.default] instead of taking one explicitly. */
public fun Player.resolveLocale(): String = resolveLocale(Translator.requireDefault())

/**
 * Renders [key] via [translator] using this player's [resolveLocale] (their [setLocale]
 * override, or else their client's language) and sends it. A statically-typed [Player]
 * reference resolves to this overload instead of the locale-agnostic
 * `Audience.sendTranslation` - use that one directly (e.g. via an `Audience`-typed
 * variable) if you always want [Translator.defaultLocale] regardless of the player.
 */
public fun Player.sendTranslation(translator: ComponentTranslator, key: String, vararg args: ComponentLike): Unit =
    sendMessage(render(translator, key, *args))

/** Same as [sendTranslation], but for a multi-line value (see [ComponentTranslator.renderLines]). */
public fun Player.sendTranslationLines(translator: ComponentTranslator, key: String, vararg args: ComponentLike) {
    for (line in renderLines(translator, key, *args)) sendMessage(line)
}

/**
 * Renders [key] via [translator] using this player's [resolveLocale], without sending it -
 * for anything other than a chat message, e.g. an item's display name or a GUI title. See
 * [sendTranslation] to render and send in one call.
 */
public fun Player.render(translator: ComponentTranslator, key: String, vararg args: ComponentLike): Component =
    translator.renderFor(resolveLocale(translator.translator), key, *args)

/** Same as [render], but for a multi-line value (see [ComponentTranslator.renderLines]). */
public fun Player.renderLines(translator: ComponentTranslator, key: String, vararg args: ComponentLike): List<Component> =
    translator.renderLinesFor(resolveLocale(translator.translator), key, *args)

/** Translates [key] via [translator] using this player's [resolveLocale], as a plain string. */
public fun Player.translate(translator: Translator, key: String, vararg args: Any?): String =
    translator.translateFor(resolveLocale(translator), key, *args)

/** Same as [translate], but for a multi-line value (see [Translator.translateLines]). */
public fun Player.translateLines(translator: Translator, key: String, vararg args: Any?): List<String> =
    translator.translateLinesFor(resolveLocale(translator), key, *args)

/** Same as [sendTranslation], but using [ComponentTranslator.default] instead of taking one explicitly. */
public fun Player.sendTranslation(key: String, vararg args: ComponentLike): Unit =
    sendTranslation(ComponentTranslator.requireDefault(), key, *args)

/** Same as [sendTranslationLines], but using [ComponentTranslator.default]. */
public fun Player.sendTranslationLines(key: String, vararg args: ComponentLike) {
    sendTranslationLines(ComponentTranslator.requireDefault(), key, *args)
}

/** Same as [render], but using [ComponentTranslator.default]. */
public fun Player.render(key: String, vararg args: ComponentLike): Component =
    render(ComponentTranslator.requireDefault(), key, *args)

/** Same as [renderLines], but using [ComponentTranslator.default]. */
public fun Player.renderLines(key: String, vararg args: ComponentLike): List<Component> =
    renderLines(ComponentTranslator.requireDefault(), key, *args)

/** Same as [translate], but using [Translator.default] instead of taking one explicitly. */
public fun Player.translate(key: String, vararg args: Any?): String =
    translate(Translator.requireDefault(), key, *args)

/** Same as [translateLines], but using [Translator.default]. */
public fun Player.translateLines(key: String, vararg args: Any?): List<String> =
    translateLines(Translator.requireDefault(), key, *args)
