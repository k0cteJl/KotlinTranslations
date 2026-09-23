package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.Translator
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom MiniMessage tags, keyed by the [Translator] they belong to. Registered via [register]
 * or [loadTags]; looked up automatically by [ComponentTranslator]'s MiniMessage-based
 * constructors, so a tag registered for a [Translator] is available as `<name>` in every literal
 * `.lang` string rendered through a [ComponentTranslator] built from it - no extra wiring needed.
 */
public object TagRegistry {
    private val resolvers = ConcurrentHashMap<Translator, TagResolver>()

    /** Registers a single tag named [name] for [translator], merging with any already registered. */
    public fun register(translator: Translator, name: String, tag: Tag) {
        merge(translator, TagResolver.resolver(name, tag))
    }

    /** The combined [TagResolver] of every tag registered for [translator], or `null` if none. */
    public fun get(translator: Translator): TagResolver? = resolvers[translator]

    /** Removes every tag registered for [translator]. */
    public fun clear(translator: Translator) {
        resolvers.remove(translator)
    }

    internal fun merge(translator: Translator, resolver: TagResolver) {
        resolvers.merge(translator, resolver) { existing, added -> TagResolver.resolver(existing, added) }
    }
}
