@file:JvmName("PluginLanguages")

package io.github.k0ctejl.translations.paper

import io.github.k0ctejl.translations.Translator
import org.bukkit.plugin.java.JavaPlugin

/**
 * Loads a `.lang` file bundled as a plugin resource (e.g. `plugin.jar!/lang/en.lang`) and
 * registers it for [locale].
 */
public fun Translator.loadFromPlugin(plugin: JavaPlugin, locale: String, resourcePath: String): Translator {
    val stream = plugin.getResource(resourcePath)
        ?: throw IllegalArgumentException("Resource '$resourcePath' not found in ${plugin.name}")
    return stream.use { loadLanguage(locale, it) }
}
