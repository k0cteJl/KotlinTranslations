package io.github.k0ctejl.translations

/**
 * A single precompiled piece of a [MessageTemplate]: either a literal run of
 * text or a positional placeholder such as `{0}`.
 *
 * Exposed publicly so integration modules (e.g. the Paper/Adventure module)
 * can walk a compiled template's structure - to build a rich-text chat
 * component, for example - without re-parsing the raw `.lang` value.
 */
public sealed class MessageSegment {
    public data class Literal(public val text: String) : MessageSegment()
    public data class Placeholder(public val index: Int) : MessageSegment()
}
