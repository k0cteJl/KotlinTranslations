package io.github.k0ctejl.translations

/** Thrown when a `.lang` file cannot be parsed, e.g. a malformed line or a missing closing quote. */
public class LangParseException(public val line: Int, reason: String) :
    RuntimeException("Line $line: $reason")
