English | [Русский](README.ru.md)

# KotlinTranslations

A lightweight, fast translation API for JVM applications, with ready-made Paper API
integration (and its forks, e.g. CanvasMC) via Adventure components.

- **No parsing/regex on the hot path.** Every `.lang` file is parsed and compiled into an
  array of segments (literal/placeholder) once, at load time. Calling `translate(...)` just
  walks that already-compiled list and concatenates into a `StringBuilder`.
- **No third-party dependencies in `core`.** Just the Kotlin stdlib and the JDK.
- **Java- and Kotlin-friendly.** `@JvmStatic`/`@JvmOverloads` on every factory method, no
  suspend functions or Kotlin-only tricks in the public API.

## Modules

| Module  | Purpose                                                                       |
|---------|--------------------------------------------------------------------------------|
| `core`  | The `.lang` format, template compilation, `Translator` — no Minecraft dependency |
| `paper` | Renders translations as `net.kyori.adventure.text.Component`, extension functions for the Paper API (`Audience`, `Player`, `JavaPlugin`) — also works on Paper/Folia forks sharing the same API, such as CanvasMC |

## The `.lang` format

```
# comment (# or //)
greeting="Hello, {0}! You have {1} messages."
farewell="Goodbye!"
```

- Key: letters, digits, `.`, `_`, `-`.
- Value must be wrapped in double quotes and supports `\"`, `\\`, `\n`, `\t` escapes.
- Placeholders are `{0}`, `{1}`, ... (positional; can repeat or appear out of order).
- Only a comment (`# ...` / `// ...`) is allowed after the closing quote.

## Core: Kotlin

```kotlin
val translator = Translator.create(defaultLocale = "en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .loadLanguage("ru", Path.of("lang/ru.lang"))

translator.translate("greeting", "Bob", 5)               // "Hello, Bob! You have 5 messages."
translator.translateFor("ru", "greeting", "Bob", 5)        // translate for a specific locale
translator.hasTranslation("greeting", "ru")
```

## Core: Java

```java
Translator translator = Translator.create("en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .loadLanguage("ru", Path.of("lang/ru.lang"));

String message = translator.translate("greeting", "Bob", 5);
String ruMessage = translator.translateFor("ru", "greeting", "Bob", 5);
```

An undefined key is returned as-is (the key itself) — no exceptions on the hot path.
A key missing for a specific locale automatically falls back to `defaultLocale`.

## Paper: Component integration

```kotlin
val translator = Translator.create("en")
    .loadFromPlugin(plugin, "en", "lang/en.lang")
    .loadFromPlugin(plugin, "ru", "lang/ru.lang")

val messages = ComponentTranslator(translator) // renders literals as MiniMessage by default

// greeting="Hello, <gold>{0}</gold>! You have {1} messages."
player.sendTranslation(messages, "greeting", Component.text(player.name), Component.text(unread))

// using this player's own locale
player.sendTranslationFor(messages, player.resolveLocale(translator), "greeting", Component.text(player.name))
```

Placeholders accept `ComponentLike`, so instead of a plain string you can pass a fully built
component — e.g. a player's name with a hover event — and it survives the substitution intact.

## Building

```
./gradlew :core:test
./gradlew build
```

The `paper` module resolves `io.papermc.paper:paper-api` from `repo.papermc.io` — like any
Paper plugin, building it requires access to that repository.
