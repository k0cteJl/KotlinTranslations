package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.Translator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Renders a [Translator]'s templates as Adventure [Component]s instead of plain strings, so a
 * translated message keeps colors, hover/click events, etc. - the rich-text system used by
 * PaperMC (and Paper/Folia forks such as CanvasMC, which share the same API) and every other
 * modern Adventure-based platform.
 *
 * A template's literal text and placeholders are deserialized by a [ComponentDeserializer] -
 * MiniMessage by default (`<red>...</red>`, `<hover:...>`, ...), or legacy `&`-codes via
 * [legacy] - in a single pass rather than segment by segment, so a color/style tag that isn't
 * explicitly closed still applies to a placeholder that follows it, e.g. `"<red>{0}"` colors the
 * inserted argument red, not just the literal text before it. A placeholder argument that's
 * already a [ComponentLike] is inserted as-is - e.g. a player's display name with its own hover
 * event survives the substitution untouched (its own explicit style still overrides ambient
 * style from surrounding tags) - anything else (a `String`, a number, ...) is wrapped in
 * [Component.text] automatically, so callers don't have to do that themselves for the common
 * case of a plain value.
 *
 * The MiniMessage-based constructors ([ComponentTranslator] itself and [miniMessage]) resolve
 * any tags [translator] has registered via [TagRegistry]/[Translator.loadTags] and merge them
 * with MiniMessage's built-in tags, once at construction time - not on every render.
 *
 * Every rendered [Component] has italic explicitly turned off by default (a fallback, not an
 * override - see [withoutDefaultItalic]), since Minecraft's client otherwise renders a component
 * used as an item's display name/lore in italics for no reason related to anything the `.lang`
 * value's markup asked for. An explicit `<italic>`/`<i>` tag (or legacy `&o`) in the source still
 * applies normally.
 */
public class ComponentTranslator @JvmOverloads constructor(
    /** The underlying [Translator] this renderer wraps, e.g. for [Translator.isLoaded] checks. */
    public val translator: Translator,
    private val deserializer: ComponentDeserializer = run {
        val miniMessage = resolveMiniMessage(translator)
        ComponentDeserializer { miniMessage.deserialize(it) }
    }
) {

    /** Renders [key] using [Translator.defaultLocale]. Returns [key] as plain text if undefined. */
    public fun render(key: String, vararg args: Any?): Component =
        renderFor(translator.defaultLocale, key, *args)

    /** Renders [key] for [locale], falling back to [Translator.defaultLocale] and finally to plain text. */
    public fun renderFor(locale: String, key: String, vararg args: Any?): Component {
        val template = translator.template(key, locale) ?: return Component.text(key).withoutDefaultItalic()
        return renderWithArguments(template.segments, deserializer, args)
    }

    /**
     * Renders each line of [key] (see [Translator.translateLines]) independently using
     * [Translator.defaultLocale]. Returns `listOf(Component.text(key))` if undefined.
     */
    public fun renderLines(key: String, vararg args: Any?): List<Component> =
        renderLinesFor(translator.defaultLocale, key, *args)

    /** Same as [renderLines], but for [locale], falling back to [Translator.defaultLocale] like [renderFor]. */
    public fun renderLinesFor(locale: String, key: String, vararg args: Any?): List<Component> {
        val template = translator.template(key, locale) ?: return listOf(Component.text(key).withoutDefaultItalic())
        return template.lines.map { lineSegments -> renderWithArguments(lineSegments, deserializer, args) }
    }

    /**
     * Registers this instance as [ComponentTranslator.default], so callers don't have to thread
     * it through every call site - see [ComponentTranslator.requireDefault] and the no-translator
     * overloads of `sendTranslation`/`render`/etc. Returns `this` for chaining.
     */
    public fun makeDefault(): ComponentTranslator {
        default = this
        return this
    }

    public companion object {
        /**
         * The application-wide default [ComponentTranslator], registered via [makeDefault].
         * `null` until something sets it. Purely a convenience - nothing in this library
         * requires it, and using an explicit instance instead is always fine.
         */
        @Volatile
        @JvmStatic
        public var default: ComponentTranslator? = null

        /** [default], or throws [IllegalStateException] if nothing has called [makeDefault] yet. */
        @JvmStatic
        public fun requireDefault(): ComponentTranslator =
            default ?: throw IllegalStateException("No default ComponentTranslator set - call ComponentTranslator(...).makeDefault() first")

        /**
         * A [ComponentTranslator] that parses literal text as MiniMessage using [miniMessage].
         * Defaults to [translator]'s registered [TagRegistry] tags merged with the standard
         * MiniMessage tags - pass an explicit [miniMessage] instance to opt out of that.
         */
        @JvmStatic
        @JvmOverloads
        public fun miniMessage(translator: Translator, miniMessage: MiniMessage = resolveMiniMessage(translator)): ComponentTranslator =
            ComponentTranslator(translator, ComponentDeserializer { miniMessage.deserialize(it) })

        /** A [ComponentTranslator] that parses literal text as legacy `&`-coded strings, e.g. `&cHello`. */
        @JvmStatic
        @JvmOverloads
        public fun legacy(translator: Translator, character: Char = '&'): ComponentTranslator {
            val serializer = LegacyComponentSerializer.legacy(character)
            return ComponentTranslator(translator, ComponentDeserializer { serializer.deserialize(it) })
        }
    }
}

/** The default MiniMessage instance for [translator]: standard tags, plus any it has registered in [TagRegistry]. */
private fun resolveMiniMessage(translator: Translator): MiniMessage {
    val custom = TagRegistry.get(translator) ?: return MiniMessage.miniMessage()
    return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), custom)).build()
}
