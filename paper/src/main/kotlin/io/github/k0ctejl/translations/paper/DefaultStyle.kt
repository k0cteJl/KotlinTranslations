package io.github.k0ctejl.translations.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

/**
 * Minecraft's client renders a [Component] used as an item's display name/lore (and some other
 * UI contexts) in italics by default, unless the component - or an ancestor of it - explicitly
 * says otherwise. That default has nothing to do with anything a `.lang` value's markup asked
 * for, so every [Component] this library hands back gets italic turned off as a *fallback*:
 * [Component.applyFallbackStyle] only fills in properties that nothing in the tree already set
 * explicitly, so a genuine `<italic>`/`<i>` tag (or legacy `&o`) in the source still applies.
 */
private val NOT_ITALIC: Style = Style.style().decoration(TextDecoration.ITALIC, false).build()

internal fun Component.withoutDefaultItalic(): Component = applyFallbackStyle(NOT_ITALIC)
