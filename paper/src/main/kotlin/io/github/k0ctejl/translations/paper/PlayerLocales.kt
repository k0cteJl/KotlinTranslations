package io.github.k0ctejl.translations.paper

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory registry of per-player locale overrides - set explicitly (e.g. via a
 * `/language ru` command) rather than detected from the client's own Minecraft settings.
 *
 * Not persisted across server restarts: if a plugin wants overrides to survive a restart, it
 * should save [get]'s result to its own storage and call [set] again when the player rejoins
 * (e.g. in a `PlayerJoinEvent` listener). An override does survive a relog within the same
 * server run, since it's keyed by [UUID] rather than tied to a specific `Player` instance.
 *
 * [Player.setLocale][setLocale] and [Player.resolveLocale][resolveLocale] are the usual
 * entry points; this object is the backing store for both and is only exposed directly for
 * bulk operations (e.g. clearing overrides on plugin disable).
 */
public object PlayerLocales {
    private val overrides = ConcurrentHashMap<UUID, String>()

    /** Sets an explicit locale override for [playerId]. */
    public fun set(playerId: UUID, locale: String) {
        overrides[playerId] = locale
    }

    /** The explicit override for [playerId], or `null` if none was set. */
    public fun get(playerId: UUID): String? = overrides[playerId]

    /** Removes the override for [playerId], if any. */
    public fun clear(playerId: UUID) {
        overrides.remove(playerId)
    }
}
