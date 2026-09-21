[English](README.md) | Русский

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

| Модуль        | Назначение                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `core`        | Формат `.lang`, компиляция шаблонов, `Translator` — не зависит от Minecraft |
| `paper`       | Рендер переводов в `net.kyori.adventure.text.Component`, расширения для Paper API (`Audience`, `Player`, `JavaPlugin`) — работает и на форках Paper/Folia с тем же API, например CanvasMC |
| `benchmarks`  | JMH-бенчмарки для `Translator.translate` и `LangFileParser.parse` |

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

## Загрузка целой директории

Вместо вызова `loadLanguage` для каждой локали отдельно можно указать `loadLanguagesFromDirectory`
на папку с `.lang`-файлами — локаль каждого файла берётся из его имени (`lang/en.lang` → `en`,
`lang/ru.lang` → `ru`). Без рекурсии; файлы не с расширением `.lang` игнорируются.

```kotlin
val translator = Translator.create("en").loadLanguagesFromDirectory(Path.of("lang"))
```

## Проверка переводов

`validate()` сравнивает набор ключей каждой не-дефолтной локали с `defaultLocale` и сообщает,
чего не хватает или что лишнее — так устаревший файл перевода можно поймать в тесте или на этапе
сборки, а не молча получить откат на дефолтную локаль в рантайме:

```kotlin
val issues = translator.validate()
check(issues.isEmpty()) { issues.joinToString("\n") }
// ValidationIssue(locale=ru, missingKeys=[farewell], extraKeys=[])
```

## Многострочные переводы

Значение в `.lang` может занимать несколько строк через экранирование `\n` — удобно для MOTD,
help-сообщения или любого другого блока текста. `translateLines`/`translateLinesFor` переводят
так же, как `translate`/`translateFor`, а затем разбивают результат на список строк:

```
# lang/en.lang
motd="Welcome to the server, {0}!\nType /help to get started."
```

```kotlin
translator.translateLines("motd", player.name)
// ["Welcome to the server, Bob!", "Type /help to get started."]
```

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

По умолчанию литеральный текст разбирается как MiniMessage. Для плагинов/конфигов, всё ещё
использующих legacy `&`-цветовые коды, используйте `ComponentTranslator.legacy(...)`:

```kotlin
val messages = ComponentTranslator.legacy(translator) // разбирает "&cHello" вместо "<red>Hello"
val messages = ComponentTranslator.legacy(translator, character = '§')
val messages = ComponentTranslator.miniMessage(translator, myMiniMessageInstance)
```

Многострочное значение рендерится построчно — каждая строка своим `Component`, с сохранением
форматирования, — а `sendTranslationLines` отправляет их отдельными сообщениями в чат:

```kotlin
// motd="Welcome, <gold>{0}</gold>!\nType /help to get started."
player.sendTranslationLines(messages, "motd", Component.text(player.name))
```

## Локаль игрока

По умолчанию локаль игрока определяется по языку его Minecraft-клиента (`player.locale().language`).
Чтобы дать игроку (или плагину) явно выбрать язык — например, через команду `/language ru` —
используйте `setLocale`; значение хранится в памяти на игрока и имеет приоритет над языком
клиента везде, где локаль резолвится:

```kotlin
player.setLocale("ru")                 // явное переопределение, например из команды
player.localeOverride()                // "ru", или null, если не задано
player.clearLocaleOverride()           // снова использовать язык клиента

player.resolveLocale(translator)       // override -> язык клиента -> Translator.defaultLocale
```

`sendTranslation`/`sendTranslationLines`, вызванные на `Player`, автоматически используют
`resolveLocale`, так что в большинстве случаев вызывать его вручную не нужно:

```kotlin
// использует override игрока (если задан и загружен) или язык его клиента
player.sendTranslation(messages, "greeting", Component.text(player.name))
```

Это отдельная, более специфичная перегрузка по сравнению с locale-агностичным
`Audience.sendTranslation` из примера выше (тот всегда использует `Translator.defaultLocale`):
вызов на значении, статически типизированном как `Player`, выбирает эту версию, на обычном
`Audience`/`CommandSender` (например, консоли) — ту. Override хранится только в памяти и не
переживает перезапуск сервера — сохраняйте его сами и вызывайте `setLocale` заново при входе,
если это нужно.

## Сборка

```
./gradlew :core:test
./gradlew build
./gradlew :benchmarks:jmh
```

Модуль `paper` резолвит `io.papermc.paper:paper-api` из `repo.papermc.io` — как и любой
Paper-плагин, для сборки нужен доступ к этому репозиторию. `:benchmarks:jmh` запускает JMH-сьют
(настройки — в `benchmarks/build.gradle.kts`) и печатает throughput для `translate()` (простой
текст, один плейсхолдер, несколько плейсхолдеров) и для парсинга `.lang`-файла. Для разового
запуска с собственными флагами JMH (например, меньше итераций) соберите shaded-jar один раз и
запустите его напрямую:

```
./gradlew :benchmarks:jmhJar
java -jar benchmarks/build/libs/kotlintranslations-benchmarks-*-jmh.jar -wi 1 -i 1 -f 1
```
