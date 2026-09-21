package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.MessageSegment
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
 * Literal segments of the `.lang` value are turned into components by a [ComponentDeserializer] -
 * MiniMessage by default (`<red>...</red>`, `<hover:...>`, ...), or legacy `&`-codes via
 * [legacy]. Placeholder arguments are inserted as-is via [ComponentLike], so callers can pass a
 * fully built [Component] - e.g. a player's display name with its own hover event - instead of a
 * plain string, and it survives the substitution untouched.
 *
 * The MiniMessage-based constructors ([ComponentTranslator] itself and [miniMessage]) resolve
 * any tags [translator] has registered via [TagRegistry]/[Translator.loadTags] and merge them
 * with MiniMessage's built-in tags, once at construction time - not on every render.
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
    public fun render(key: String, vararg args: ComponentLike): Component =
        renderFor(translator.defaultLocale, key, *args)

    /** Renders [key] for [locale], falling back to [Translator.defaultLocale] and finally to plain text. */
    public fun renderFor(locale: String, key: String, vararg args: ComponentLike): Component {
        val template = translator.template(key, locale) ?: return Component.text(key)
        return renderSegments(template.segments, args)
    }

    /**
     * Renders each line of [key] (see [Translator.translateLines]) independently using
     * [Translator.defaultLocale]. Returns `listOf(Component.text(key))` if undefined.
     */
    public fun renderLines(key: String, vararg args: ComponentLike): List<Component> =
        renderLinesFor(translator.defaultLocale, key, *args)

    /** Same as [renderLines], but for [locale], falling back to [Translator.defaultLocale] like [renderFor]. */
    public fun renderLinesFor(locale: String, key: String, vararg args: ComponentLike): List<Component> {
        val template = translator.template(key, locale) ?: return listOf(Component.text(key))
        return template.lines.map { lineSegments -> renderSegments(lineSegments, args) }
    }

    private fun renderSegments(segments: List<MessageSegment>, args: Array<out ComponentLike>): Component {
        val builder = Component.text()
        for (segment in segments) {
            when (segment) {
                is MessageSegment.Literal -> builder.append(deserializer.deserialize(segment.text))
                is MessageSegment.Placeholder -> {
                    val arg = args.getOrNull(segment.index)
                    if (arg != null) builder.append(arg)
                }
            }
        }
        return builder.build()
    }

    public companion object {
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
