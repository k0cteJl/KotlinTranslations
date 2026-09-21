package io.github.k0ctejl.translations

/**
 * A `.lang` value compiled once into a flat list of [MessageSegment]s, so
 * that [format] never re-scans the source string or relies on regular
 * expressions / [String.format] on the hot path.
 *
 * Placeholders use a `{N}` syntax, e.g. `"Hello, {0}! You have {1} messages."`.
 * An argument missing for a given placeholder is rendered as an empty string.
 *
 * A value containing `\n` (written with a literal `\n` escape in the `.lang` file) is also
 * pre-split into [lines] at compile time, so [formatLines] costs nothing beyond a single flat
 * traversal per call - see [Translator.translateLines].
 */
public class MessageTemplate private constructor(
    public val raw: String,
    public val segments: List<MessageSegment>,
    public val placeholderCount: Int,
    public val lines: List<List<MessageSegment>>
) {

    private val isPlainLiteral: Boolean = placeholderCount == 0

    /** Substitutes `{0}`, `{1}`, ... in this template with [args] and returns the result. */
    public fun format(vararg args: Any?): String {
        if (isPlainLiteral) return raw
        return formatSegments(segments, args)
    }

    /**
     * Formats this template like [format], then splits the result on line breaks - one entry
     * per line of a multi-line `.lang` value. A template with no `\n` returns a single-element
     * list equivalent to `listOf(format(*args))`.
     */
    public fun formatLines(vararg args: Any?): List<String> {
        if (lines.size == 1) return listOf(format(*args))
        return lines.map { formatSegments(it, args) }
    }

    private fun formatSegments(lineSegments: List<MessageSegment>, args: Array<out Any?>): String {
        val builder = StringBuilder(raw.length + args.size * 8)
        for (segment in lineSegments) {
            when (segment) {
                is MessageSegment.Literal -> builder.append(segment.text)
                is MessageSegment.Placeholder -> {
                    if (segment.index < args.size) builder.append(args[segment.index])
                }
            }
        }
        return builder.toString()
    }

    override fun toString(): String = raw

    public companion object {
        private const val OPEN: Char = '{'
        private const val CLOSE: Char = '}'

        /** Parses [raw] into a [MessageTemplate], splitting it into literal and `{N}` placeholder segments. */
        @JvmStatic
        public fun compile(raw: String): MessageTemplate {
            val segments = ArrayList<MessageSegment>()
            val literal = StringBuilder()
            var maxIndex = -1

            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (c == OPEN) {
                    val close = raw.indexOf(CLOSE, i + 1)
                    val digits = close - i - 1
                    val isPlaceholder = close > i + 1 && digits <= 9 && (i + 1 until close).all { raw[it].isDigit() }
                    if (isPlaceholder) {
                        if (literal.isNotEmpty()) {
                            segments.add(MessageSegment.Literal(literal.toString()))
                            literal.setLength(0)
                        }
                        val index = raw.substring(i + 1, close).toInt()
                        segments.add(MessageSegment.Placeholder(index))
                        if (index > maxIndex) maxIndex = index
                        i = close + 1
                        continue
                    }
                }
                literal.append(c)
                i++
            }
            if (literal.isNotEmpty()) {
                segments.add(MessageSegment.Literal(literal.toString()))
            }
            return MessageTemplate(raw, segments, maxIndex + 1, splitIntoLines(segments))
        }

        private fun splitIntoLines(segments: List<MessageSegment>): List<List<MessageSegment>> {
            val lines = ArrayList<List<MessageSegment>>()
            var current = ArrayList<MessageSegment>()
            for (segment in segments) {
                when (segment) {
                    is MessageSegment.Placeholder -> current.add(segment)
                    is MessageSegment.Literal -> {
                        val parts = segment.text.split("\n")
                        for ((index, part) in parts.withIndex()) {
                            if (part.isNotEmpty()) current.add(MessageSegment.Literal(part))
                            if (index != parts.lastIndex) {
                                lines.add(current)
                                current = ArrayList()
                            }
                        }
                    }
                }
            }
            lines.add(current)
            return lines
        }
    }
}
