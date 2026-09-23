package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.MessageSegment
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent

/**
 * Renders [segments] through [deserializer] as a single call and splices [args] into the
 * result, instead of deserializing each literal segment independently.
 *
 * Deserializing segment-by-segment (the naive approach) breaks style that isn't explicitly
 * closed within one segment: `"<red>{0}"` would deserialize `"<red>"` alone - an empty,
 * effectively invisible styled component - and then append the placeholder's component as an
 * unstyled sibling, so the color never reaches it. Building one marked-up string and
 * deserializing it in one pass lets the underlying parser (MiniMessage, legacy, ...) thread
 * color/style across the whole template exactly like it would for any other string, then
 * [spliceArguments] walks the resulting tree and swaps each marker back out for the real
 * argument component, inheriting whatever style ended up wrapping that marker.
 *
 * Each argument in [args] is used as-is if it's already a [ComponentLike] (so a caller can
 * still pass a fully built [Component] - e.g. a hover event on a player's name); anything
 * else (a `String`, a number, ...) is wrapped in [Component.text] automatically via
 * [toComponentLike], so callers don't have to write that wrapping themselves.
 *
 * The result has italic explicitly turned off by default - see [withoutDefaultItalic] - so it
 * renders correctly when used as an item's display name/lore, not italic unless asked for.
 */
internal fun renderWithArguments(
    segments: List<MessageSegment>,
    deserializer: ComponentDeserializer,
    args: Array<out Any?>
): Component {
    val result = if (segments.none { it is MessageSegment.Placeholder }) {
        val literal = segments.joinToString("") { (it as MessageSegment.Literal).text }
        deserializer.deserialize(literal)
    } else {
        spliceArguments(deserializer.deserialize(buildMarkedRaw(segments)), args)
    }
    return result.withoutDefaultItalic()
}

private const val MARKER_OPEN = ''
private const val MARKER_CLOSE = ''

private fun buildMarkedRaw(segments: List<MessageSegment>): String {
    val sb = StringBuilder()
    for (segment in segments) {
        when (segment) {
            is MessageSegment.Literal -> sb.append(segment.text)
            is MessageSegment.Placeholder -> sb.append(MARKER_OPEN).append(segment.index).append(MARKER_CLOSE)
        }
    }
    return sb.toString()
}

private sealed class MarkedPiece {
    class Text(val value: String) : MarkedPiece()
    class Arg(val index: Int) : MarkedPiece()
}

private fun splitMarked(text: String): List<MarkedPiece> {
    val pieces = ArrayList<MarkedPiece>()
    val literal = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == MARKER_OPEN) {
            val close = text.indexOf(MARKER_CLOSE, i + 1)
            val index = if (close > i + 1) text.substring(i + 1, close).toIntOrNull() else null
            if (index != null) {
                if (literal.isNotEmpty()) {
                    pieces.add(MarkedPiece.Text(literal.toString()))
                    literal.setLength(0)
                }
                pieces.add(MarkedPiece.Arg(index))
                i = close + 1
                continue
            }
        }
        literal.append(c)
        i++
    }
    if (literal.isNotEmpty()) pieces.add(MarkedPiece.Text(literal.toString()))
    return pieces
}

private fun spliceArguments(component: Component, args: Array<out Any?>): Component {
    val originalChildren = component.children()
    val rebuiltChildren = if (originalChildren.isEmpty()) originalChildren else originalChildren.map { spliceArguments(it, args) }

    if (component is TextComponent && component.content().indexOf(MARKER_OPEN) >= 0) {
        val builder = Component.text().style(component.style())
        for (piece in splitMarked(component.content())) {
            when (piece) {
                is MarkedPiece.Text -> builder.append(Component.text(piece.value))
                is MarkedPiece.Arg -> args.getOrNull(piece.index)?.let { builder.append(it.toComponentLike()) }
            }
        }
        for (child in rebuiltChildren) builder.append(child)
        return builder.build()
    }

    return if (rebuiltChildren !== originalChildren) component.children(rebuiltChildren) else component
}

/** [this] as-is if it's already a [ComponentLike], otherwise wrapped in [Component.text]. */
private fun Any?.toComponentLike(): ComponentLike = if (this is ComponentLike) this else Component.text(this.toString())
