package io.github.k0ctejl.translations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LangFileParserTest {

    @Test
    fun `parses basic key-value pairs`() {
        val result = parse(
            """
            greeting="Hello, world!"
            farewell="Goodbye!"
            """.trimIndent()
        )
        assertEquals("Hello, world!", result["greeting"])
        assertEquals("Goodbye!", result["farewell"])
    }

    @Test
    fun `ignores blank lines and comments`() {
        val result = parse(
            """
            # this is a comment

            // this too
            key="value"
            """.trimIndent()
        )
        assertEquals(1, result.size)
        assertEquals("value", result["key"])
    }

    @Test
    fun `allows dots underscores and dashes in keys`() {
        val result = parse("""some.key_name-1="value"""")
        assertTrue(result.containsKey("some.key_name-1"))
    }

    @Test
    fun `preserves placeholder syntax untouched`() {
        val result = parse("""greeting="Hello, {0}! You have {1} messages."""")
        assertEquals("Hello, {0}! You have {1} messages.", result["greeting"])
    }

    @Test
    fun `unescapes quotes backslashes newlines and tabs`() {
        val result = parse("""key="line1\nline2\ttabbed \"quoted\" back\\slash"""")
        assertEquals("line1\nline2\ttabbed \"quoted\" back\\slash", result["key"])
    }

    @Test
    fun `allows trailing comment after closing quote`() {
        val result = parse("""key="value" # trailing comment""")
        assertEquals("value", result["key"])
    }

    @Test
    fun `throws on missing equals sign`() {
        val exception = assertFailsWith<LangParseException> { parse("not_a_valid_line") }
        assertEquals(1, exception.line)
    }

    @Test
    fun `throws on unterminated string`() {
        assertFailsWith<LangParseException> { parse("""key="unterminated""") }
    }

    @Test
    fun `throws on missing opening quote`() {
        assertFailsWith<LangParseException> { parse("key=value") }
    }

    @Test
    fun `throws on unexpected trailing content`() {
        assertFailsWith<LangParseException> { parse("""key="value" garbage""") }
    }

    @Test
    fun `throws on invalid key characters`() {
        assertFailsWith<LangParseException> { parse("""bad key="value"""") }
    }

    @Test
    fun `throws on duplicate keys`() {
        assertFailsWith<LangParseException> {
            parse(
                """
                key="first"
                key="second"
                """.trimIndent()
            )
        }
    }

    @Test
    fun `reports the correct line number`() {
        val exception = assertFailsWith<LangParseException> {
            parse(
                """
                good="value"
                bad line without equals
                """.trimIndent()
            )
        }
        assertEquals(2, exception.line)
    }

    private fun parse(text: String): Map<String, String> =
        LangFileParser.parse(text.byteInputStream())
}
