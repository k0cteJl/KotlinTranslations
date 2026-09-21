@file:JvmName("PlayerLocaleExtensions")

package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.Translator
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

/**
 * Renders [key] via [translator] using this player's [resolveLocale] (their [setLocale]
 * override, or else their client's language) and sends it. A statically-typed [Player]
 * reference resolves to this overload instead of the locale-agnostic
 * `Audience.sendTranslation` - use that one directly (e.g. via an `Audience`-typed
 * variable) if you always want [Translator.defaultLocale] regardless of the player.
 */
public fun Player.sendTranslation(translator: ComponentTranslator, key: String, vararg args: ComponentLike): Unit =
    sendMessage(translator.renderFor(resolveLocale(translator.translator), key, *args))

/** Same as [sendTranslation], but for a multi-line value (see [ComponentTranslator.renderLines]). */
public fun Player.sendTranslationLines(translator: ComponentTranslator, key: String, vararg args: ComponentLike) {
    val locale = resolveLocale(translator.translator)
    for (line in translator.renderLinesFor(locale, key, *args)) sendMessage(line)
}
