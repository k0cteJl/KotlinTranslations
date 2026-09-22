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

| Module        | Purpose                                                                       |
|---------------|--------------------------------------------------------------------------------|
| `core`        | The `.lang` format, template compilation, `Translator` — no Minecraft dependency |
| `paper`       | Renders translations as `net.kyori.adventure.text.Component`, extension functions for the Paper API (`Audience`, `Player`, `JavaPlugin`) — also works on Paper/Folia forks sharing the same API, such as CanvasMC |
| `benchmarks`  | JMH benchmarks for `Translator.translate` and `LangFileParser.parse` |

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

## Loading a whole directory

`loadLanguage` also accepts a directory instead of a single file: every `*.lang` file directly
inside it (not recursive) is parsed and merged into one catalog for the given locale — handy for
splitting one language across several files instead of one giant `.lang`:

```
lang/en/messages.lang
lang/en/commands.lang
```

```kotlin
val translator = Translator.create("en").loadLanguage("en", Path.of("lang/en/"))
```

A key defined in more than one file of the directory throws `IllegalArgumentException`.

To go the other way — one file per locale, several locales at once — point
`loadLanguagesFromDirectory` at a folder of `.lang` files; each file's locale is taken from its
name (`lang/en.lang` → `en`, `lang/ru.lang` → `ru`):

```kotlin
val translator = Translator.create("en").loadLanguagesFromDirectory(Path.of("lang"))
```

## Validating translations

`validate()` compares every non-default locale's key set against `defaultLocale`'s and reports
what's missing or extra, so a stale translation file can fail a test or a build step instead of
silently falling back to the default locale at runtime:

```kotlin
val issues = translator.validate()
check(issues.isEmpty()) { issues.joinToString("\n") }
// ValidationIssue(locale=ru, missingKeys=[farewell], extraKeys=[])
```

## Multi-line translations

A `.lang` value can span multiple lines using the `\n` escape - handy for a MOTD, a help
message, or any other block of text. `translateLines`/`translateLinesFor` translate it like
`translate`/`translateFor`, then split the result into one entry per line:

```
# lang/en.lang
motd="Welcome to the server, {0}!\nType /help to get started."
```

```kotlin
translator.translateLines("motd", player.name)
// ["Welcome to the server, Bob!", "Type /help to get started."]
```

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
component — e.g. a player's name with a hover event — and it survives the substitution intact
(an argument's own explicit style still overrides ambient style from surrounding tags).

A whole template is deserialized in one pass rather than segment by segment, so a color/style
tag left open still applies to a placeholder that follows it:

```
# both {0} render red - "<red>" isn't closed, so it applies through the whole line
greeting="<red>Hello, {0}!"
```

A genuinely self-closed tag (`<red/>`) still means an empty, zero-width scope, same as in any
other MiniMessage string - it colors nothing, since there's nothing between its open and close.
Close the tag explicitly (`<red>Hello</red>, {0}!`) to scope color to part of a line.

By default literal text is parsed as MiniMessage. For plugins/configs still using legacy
`&`-color codes, use `ComponentTranslator.legacy(...)` instead:

```kotlin
val messages = ComponentTranslator.legacy(translator) // parses "&cHello" instead of "<red>Hello"
val messages = ComponentTranslator.legacy(translator, character = '§')
val messages = ComponentTranslator.miniMessage(translator, myMiniMessageInstance)
```

A multi-line value renders each line as its own `Component`, keeping per-line formatting intact,
and `sendTranslationLines` sends them as separate chat messages:

```kotlin
// motd="Welcome, <gold>{0}</gold>!\nType /help to get started."
player.sendTranslationLines(messages, "motd", Component.text(player.name))
```

## Custom MiniMessage tags

`loadTags` reads a `.lang`-format file and registers every entry as a MiniMessage tag, so
`<key>` becomes usable in any literal `.lang` text - no extra wiring needed, every
`ComponentTranslator` built from this `Translator` picks the tags up automatically:

```
# lang/values.lang
lumen_green="#297D3F"
prefix="<gray>[<gold>Server</gold>]</gray> "
```

```kotlin
val translator = Translator.create("en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .loadTags(Path.of("lang/values.lang"))

val messages = ComponentTranslator(translator) // already knows <lumen_green> and <prefix>

// greeting="<lumen_green>Hello</lumen_green>, {0}!"
// announce="<prefix>Server restarting in {0} minutes."
```

A value that looks like a hex color (`#rrggbb` or `rrggbb`) becomes a real color tag - not just
literal text - so `<lumen_green>text</lumen_green>` actually colors `text`. Any other value is
inserted as-is and re-parsed as MiniMessage, so it can itself contain markup (like `prefix`
above). Tags are looked up by `ComponentTranslator`/`ComponentTranslator.miniMessage(...)`; pass
an explicit `MiniMessage` instance to `ComponentTranslator.miniMessage(translator, mm)` to opt
out. `ComponentTranslator.legacy(...)` doesn't use MiniMessage at all, so custom tags don't apply
there.

## Player locale

By default a player's locale is detected from their client's own Minecraft language setting
(`player.locale().language`). To let players (or your plugin) pick a language explicitly instead
- e.g. via a `/language ru` command - use `setLocale`; it's stored in-memory per player and takes
priority over the client's setting wherever a locale is resolved:

```kotlin
player.setLocale("ru")                 // explicit override, e.g. from a command
player.localeOverride()                // "ru", or null if none was set
player.clearLocaleOverride()           // fall back to the client's own language again

player.resolveLocale(translator)       // override -> client language -> Translator.defaultLocale
```

Every `Player`-receiver translate/render/send function uses `resolveLocale` automatically, so
none of them need a locale argument - `translator`/`messages` is all they take besides the key
and placeholder args:

```kotlin
player.sendTranslation(messages, "greeting", Component.text(player.name))       // renders + sends a Component
player.sendTranslationLines(messages, "motd", Component.text(player.name))      // ... one message per line

player.render(messages, "greeting", Component.text(player.name))                // renders without sending -
player.renderLines(messages, "motd", Component.text(player.name))               // e.g. for an item's display name

player.translate(translator, "greeting", player.name)                           // plain String, no Component at all
player.translateLines(translator, "motd", player.name)
```

These are separate, more specific overloads from the locale-agnostic `Audience.sendTranslation`/
`Translator.translate` shown earlier (which always use `Translator.defaultLocale`); calling one
of these names on a value statically typed as `Player` picks the resolveLocale-aware version, on
a plain `Audience`/`CommandSender` (e.g. the console) or by calling the `Translator`/
`ComponentTranslator` method directly it picks the locale-agnostic one. The override is kept in
memory only and is not persisted across server restarts - persist it yourself and call
`setLocale` again on join if you need that.

## Building

```
./gradlew :core:test
./gradlew build
./gradlew :benchmarks:jmh
```

The `paper` module resolves `io.papermc.paper:paper-api` from `repo.papermc.io` — like any
Paper plugin, building it requires access to that repository. `:benchmarks:jmh` runs the JMH
suite (configured in `benchmarks/build.gradle.kts`) and prints throughput for `translate()`
(plain text, one placeholder, several placeholders) and for parsing a `.lang` file. For an
ad-hoc run with JMH's own flags (e.g. fewer iterations), build the shaded jar once and run it
directly:

```
./gradlew :benchmarks:jmhJar
java -jar benchmarks/build/libs/kotlintranslations-benchmarks-*-jmh.jar -wi 1 -i 1 -f 1
```
