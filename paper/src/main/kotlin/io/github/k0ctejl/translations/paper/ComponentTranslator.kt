package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.MessageSegment
import io.github.k0ctejl.translations.Translator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Renders a [Translator]'s templates as Adventure [Component]s instead of plain strings, so a
 * translated message keeps colors, hover/click events, etc. - the rich-text system used by
 * PaperMC (and Paper/Folia forks such as CanvasMC, which share the same API) and every other
 * modern Adventure-based platform.
 *
 * Literal segments of the `.lang` value are parsed as [MiniMessage] (`<red>...</red>`, `<hover:...>`,
 * ...). Placeholder arguments are inserted as-is via [ComponentLike], so callers can pass a fully
 * built [Component] - e.g. a player's display name with its own hover event - instead of a plain
 * string, and it survives the substitution untouched.
 */
public class ComponentTranslator @JvmOverloads constructor(
    private val translator: Translator,
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
) {

    /** Renders [key] using [Translator.defaultLocale]. Returns [key] as plain text if undefined. */
    public fun render(key: String, vararg args: ComponentLike): Component =
        renderFor(translator.defaultLocale, key, *args)

    /** Renders [key] for [locale], falling back to [Translator.defaultLocale] and finally to plain text. */
    public fun renderFor(locale: String, key: String, vararg args: ComponentLike): Component {
        val template = translator.template(key, locale) ?: return Component.text(key)

        val builder = Component.text()
        for (segment in template.segments) {
            when (segment) {
                is MessageSegment.Literal -> builder.append(miniMessage.deserialize(segment.text))
                is MessageSegment.Placeholder -> {
                    val arg = args.getOrNull(segment.index)
                    if (arg != null) builder.append(arg)
                }
            }
        }
        return builder.build()
    }
}
