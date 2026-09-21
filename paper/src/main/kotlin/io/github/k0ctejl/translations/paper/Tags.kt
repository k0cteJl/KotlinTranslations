@file:JvmName("Tags")

package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.LangFileParser
import io.github.k0ctejl.translations.Translator
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Parses [path] as a `.lang` file and registers every `key="value"` entry as a MiniMessage tag
 * named `key` in [TagRegistry], so it can be used as `<key>` anywhere a [ComponentTranslator]
 * built from this [Translator] renders literal text - no extra wiring needed.
 *
 * A value that looks like a hex color (`#rrggbb` or `rrggbb`) becomes a genuine color tag, e.g.
 *
 * ```
 * lumen_green="#297D3F"
 * ```
 *
 * lets you write `<lumen_green>green text</lumen_green>`. Any other value is inserted as-is and
 * re-parsed as MiniMessage, so it can itself contain markup, e.g. a reusable prefix:
 *
 * ```
 * prefix="<gray>[<gold>Server</gold>]</gray> "
 * ```
 */
@JvmOverloads
public fun Translator.loadTags(path: Path, charset: Charset = StandardCharsets.UTF_8): Translator {
    val resolvers = LangFileParser.parse(path, charset).map { (name, value) -> TagResolver.resolver(name, tagFor(value)) }
    TagRegistry.merge(this, TagResolver.resolver(resolvers))
    return this
}

private fun tagFor(value: String): Tag {
    val color = TextColor.fromHexString(if (value.startsWith("#")) value else "#$value")
    return if (color != null) Tag.styling(color) else Tag.preProcessParsed(value)
}
