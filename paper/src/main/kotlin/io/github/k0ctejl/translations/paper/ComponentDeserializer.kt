package io.github.k0ctejl.translations.paper

import net.kyori.adventure.text.Component

/**
 * Turns a literal segment of a `.lang` value into a [Component]. Implemented by MiniMessage
 * (the default) and legacy `&`-code parsing, and pluggable so callers can bring their own.
 */
public fun interface ComponentDeserializer {
    public fun deserialize(text: String): Component
}
