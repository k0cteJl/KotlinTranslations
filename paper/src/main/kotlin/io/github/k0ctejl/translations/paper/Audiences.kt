@file:JvmName("Audiences")

package io.github.k0ctejl.translations.paper

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.ComponentLike

/** Renders [key] via [translator] and sends it to this [Audience] as a rich-text component. */
public fun Audience.sendTranslation(translator: ComponentTranslator, key: String, vararg args: ComponentLike): Unit =
    sendMessage(translator.render(key, *args))

/** Same as [sendTranslation], but for an explicit [locale] rather than the translator's default. */
public fun Audience.sendTranslationFor(
    translator: ComponentTranslator,
    locale: String,
    key: String,
    vararg args: ComponentLike
): Unit = sendMessage(translator.renderFor(locale, key, *args))
