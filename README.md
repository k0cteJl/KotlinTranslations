# KotlinTranslations

Максимально лёгкий и быстрый API для переводов текста в JVM-приложениях, с готовой
интеграцией под Paper API (и его форки, например CanvasMC) через компоненты Adventure.

- **Без парсинга/regex на горячем пути.** Каждый `.lang`-файл разбирается и компилируется в
  массив сегментов (литерал/плейсхолдер) один раз при загрузке. Вызов `translate(...)` — это
  просто проход по уже готовому списку сегментов и конкатенация в `StringBuilder`.
- **Без сторонних зависимостей в `core`.** Только Kotlin stdlib и JDK.
- **Java- и Kotlin-friendly.** `@JvmStatic`/`@JvmOverloads` на всех фабричных методах,
  никаких suspend-функций или Kotlin-специфичных трюков в публичном API.

## Модули

| Модуль  | Назначение                                                                 |
|---------|-----------------------------------------------------------------------------|
| `core`  | Формат `.lang`, компиляция шаблонов, `Translator` — не зависит от Minecraft |
| `paper` | Рендер переводов в `net.kyori.adventure.text.Component`, расширения для Paper API (`Audience`, `Player`, `JavaPlugin`) — работает и на форках Paper/Folia с тем же API, например CanvasMC |

## Формат `.lang`

```
# комментарий (# или //)
greeting="Hello, {0}! You have {1} messages."
farewell="Goodbye!"
```

- Ключ: буквы, цифры, `.`, `_`, `-`.
- Значение — обязательно в двойных кавычках, поддерживает экранирование `\"`, `\\`, `\n`, `\t`.
- Плейсхолдеры — `{0}`, `{1}`, ... (позиционные, можно повторять и использовать не по порядку).
- После закрывающей кавычки допустим только комментарий (`# ...` / `// ...`).

## Core: Kotlin

```kotlin
val translator = Translator.create(defaultLocale = "en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .loadLanguage("ru", Path.of("lang/ru.lang"))

translator.translate("greeting", "Bob", 5)              // "Hello, Bob! You have 5 messages."
translator.translateFor("ru", "greeting", "Bob", 5)       // перевод на конкретную локаль
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

Незаданный ключ возвращается как есть (сам ключ) — без исключений на хот-пути.
Отсутствующий в конкретной локали ключ автоматически откатывается на `defaultLocale`.

## Paper: Component-интеграция

```kotlin
val translator = Translator.create("en")
    .loadFromPlugin(plugin, "en", "lang/en.lang")
    .loadFromPlugin(plugin, "ru", "lang/ru.lang")

val messages = ComponentTranslator(translator) // по умолчанию рендерит литералы как MiniMessage

// greeting="Hello, <gold>{0}</gold>! You have {1} messages."
player.sendTranslation(messages, "greeting", Component.text(player.name), Component.text(unread))

// с локалью конкретного игрока
player.sendTranslationFor(messages, player.resolveLocale(translator), "greeting", Component.text(player.name))
```

Плейсхолдеры принимают `ComponentLike`, поэтому вместо простой строки можно передать
полноценный компонент — например, имя игрока с hover-событием — и оно сохранится при подстановке.

## Сборка

```
./gradlew :core:test
./gradlew build
```

Модуль `paper` резолвит `io.papermc.paper:paper-api` из `repo.papermc.io` — как и любой
Paper-плагин, для сборки нужен доступ к этому репозиторию.
