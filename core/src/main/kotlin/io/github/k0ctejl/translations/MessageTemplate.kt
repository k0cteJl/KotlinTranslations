package io.github.k0ctejl.translations

/**
 * A `.lang` value compiled once into a flat list of [MessageSegment]s, so
 * that [format] never re-scans the source string or relies on regular
 * expressions / [String.format] on the hot path.
 *
 * Placeholders use a `{N}` syntax, e.g. `"Hello, {0}! You have {1} messages."`.
 * An argument missing for a given placeholder is rendered as an empty string.
 */
public class MessageTemplate private constructor(
    public val raw: String,
    public val segments: List<MessageSegment>,
    public val placeholderCount: Int
) {

    private val isPlainLiteral: Boolean = placeholderCount == 0

    /** Substitutes `{0}`, `{1}`, ... in this template with [args] and returns the result. */
    public fun format(vararg args: Any?): String {
        if (isPlainLiteral) return raw

        val builder = StringBuilder(raw.length + args.size * 8)
        for (segment in segments) {
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
            return MessageTemplate(raw, segments, maxIndex + 1)
        }
    }
}
