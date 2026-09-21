package io.github.k0ctejl.translations

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTemplateTest {

    @Test
    fun `formats a plain literal with no placeholders`() {
        val template = MessageTemplate.compile("Hello, world!")
        assertEquals("Hello, world!", template.format())
    }

    @Test
    fun `substitutes positional placeholders`() {
        val template = MessageTemplate.compile("Hello, {0}! You have {1} messages.")
        assertEquals("Hello, Bob! You have 5 messages.", template.format("Bob", 5))
    }

    @Test
    fun `allows a placeholder to repeat`() {
        val template = MessageTemplate.compile("{0} equals {0}")
        assertEquals("42 equals 42", template.format(42))
    }

    @Test
    fun `treats non-numeric braces as literal text`() {
        val template = MessageTemplate.compile("This is a {literal} block, not {0} placeholder")
        assertEquals("This is a {literal} block, not X placeholder", template.format("X"))
    }

    @Test
    fun `treats unclosed brace as literal text`() {
        val template = MessageTemplate.compile("unbalanced { brace")
        assertEquals("unbalanced { brace", template.format())
    }

    @Test
    fun `renders missing arguments as empty string`() {
        val template = MessageTemplate.compile("Hello, {0}!")
        assertEquals("Hello, !", template.format())
    }

    @Test
    fun `placeholders can appear out of order`() {
        val template = MessageTemplate.compile("{1} before {0}")
        assertEquals("second before first", template.format("first", "second"))
    }

    @Test
    fun `toString returns the raw source`() {
        val template = MessageTemplate.compile("raw {0} value")
        assertEquals("raw {0} value", template.toString())
    }

    @Test
    fun `formatLines splits a multi-line value into one entry per line`() {
        val template = MessageTemplate.compile("Line one\nLine two\nLine three")
        assertEquals(listOf("Line one", "Line two", "Line three"), template.formatLines())
    }

    @Test
    fun `formatLines substitutes placeholders independently on each line`() {
        val template = MessageTemplate.compile("Hello, {0}!\nYou have {1} messages.")
        assertEquals(listOf("Hello, Bob!", "You have 5 messages."), template.formatLines("Bob", 5))
    }

    @Test
    fun `formatLines returns a single entry for a template with no newline`() {
        val template = MessageTemplate.compile("Hello, {0}!")
        assertEquals(listOf("Hello, Bob!"), template.formatLines("Bob"))
    }

    @Test
    fun `formatLines preserves empty lines`() {
        val template = MessageTemplate.compile("first\n\nthird")
        assertEquals(listOf("first", "", "third"), template.formatLines())
    }

    @Test
    fun `formatLines handles a placeholder that spans the whole line`() {
        val template = MessageTemplate.compile("{0}\n{1}")
        assertEquals(listOf("a", "b"), template.formatLines("a", "b"))
    }
}
