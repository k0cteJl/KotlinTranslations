package io.github.k0ctejl.translations

import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Parses the `.lang` file format:
 *
 * ```
 * # comment
 * key.name="value"
 * greeting="Hello, {0}! You have {1} messages."
 * ```
 *
 * Rules:
 * - Blank lines, and lines starting with `#` or `//` (after trimming), are ignored.
 * - A line is `key="value"`; the key may contain letters, digits, `.`, `_` and `-`.
 * - Values support `\"`, `\\`, `\n` and `\t` escapes.
 * - Trailing content after the closing quote is only allowed if it is a `#`/`//` comment.
 *
 * Parsing is a single linear pass with no regex/backtracking on the value body, so loading
 * a language file stays cheap even for large translation sets.
 */
public object LangFileParser {

    private val KEY_PATTERN: Regex = Regex("[A-Za-z0-9_.\\-]+")

    /** Parses every `key="value"` line from [reader] into an insertion-ordered map. */
    @JvmStatic
    public fun parse(reader: BufferedReader): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var lineNumber = 0
        reader.forEachLine { rawLine ->
            lineNumber++
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachLine

            val eq = line.indexOf('=')
            if (eq < 0) throw LangParseException(lineNumber, "missing '=' separator")

            val key = line.substring(0, eq).trim()
            if (key.isEmpty() || !KEY_PATTERN.matches(key)) {
                throw LangParseException(lineNumber, "invalid key '$key'")
            }
            if (result.containsKey(key)) {
                throw LangParseException(lineNumber, "duplicate key '$key'")
            }

            result[key] = parseValue(line, eq + 1, lineNumber)
        }
        return result
    }

    /** Reads [input] as [charset] and parses it as a `.lang` file. */
    @JvmStatic
    @JvmOverloads
    public fun parse(input: InputStream, charset: Charset = StandardCharsets.UTF_8): Map<String, String> =
        input.bufferedReader(charset).use { parse(it) }

    /** Reads the file at [path] as [charset] and parses it as a `.lang` file. */
    @JvmStatic
    @JvmOverloads
    public fun parse(path: Path, charset: Charset = StandardCharsets.UTF_8): Map<String, String> =
        Files.newBufferedReader(path, charset).use { parse(it) }

    private fun parseValue(line: String, valueStart: Int, lineNumber: Int): String {
        var i = valueStart
        while (i < line.length && line[i].isWhitespace()) i++
        if (i >= line.length || line[i] != '"') {
            throw LangParseException(lineNumber, "value must start with '\"'")
        }
        i++

        val sb = StringBuilder(line.length - i)
        var closed = false
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                when (line[i + 1]) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
                continue
            }
            if (c == '"') {
                closed = true
                i++
                break
            }
            sb.append(c)
            i++
        }
        if (!closed) throw LangParseException(lineNumber, "unterminated string literal")

        val trailing = line.substring(i).trim()
        if (trailing.isNotEmpty() && !trailing.startsWith("#") && !trailing.startsWith("//")) {
            throw LangParseException(lineNumber, "unexpected content after value: '$trailing'")
        }

        return sb.toString()
    }
}
