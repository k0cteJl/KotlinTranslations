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

## Инстанс по умолчанию

Передавать `Translator` в каждый вызов раздражает, как только у плагина появляется больше одного
класса, которому он нужен. Зарегистрируйте один как дефолтный — и всё, что в нём нуждается,
достанет его само, без передачи вручную:

```kotlin
Translator.create("en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .makeDefault()

// где угодно ещё, без единой ссылки на Translator в области видимости:
Translator.requireDefault().translate("greeting", "Bob")
```

`requireDefault()` выбрасывает `IllegalStateException` с понятным сообщением, если `makeDefault()`
ещё не вызывался. Это полностью опционально — явный инстанс `Translator` по-прежнему работает как
раньше и остаётся лучшим выбором, если плагину реально нужно несколько (например, отдельные файлы
переводов на под-модуль). См. [Paper: Component-интеграция](#paper-component-интеграция) — там
такой же `ComponentTranslator.default`, позволяющий `Audience`/`Player`-функциям вроде
`sendTranslation` полностью опустить аргумент `translator`.

## Загрузка целой директории

`loadLanguage` теперь принимает и директорию вместо одного файла: все `*.lang`-файлы прямо внутри
неё (без рекурсии) разбираются и объединяются в один каталог для указанной локали — удобно,
чтобы разбить один язык на несколько файлов вместо одного огромного `.lang`:

```
lang/en/messages.lang
lang/en/commands.lang
```

```kotlin
val translator = Translator.create("en").loadLanguage("en", Path.of("lang/en/"))
```

Если один и тот же ключ встречается больше чем в одном файле директории, выбрасывается
`IllegalArgumentException`.

Для обратного случая — один файл на локаль, сразу несколько локалей — используйте
`loadLanguagesFromDirectory` на папку с `.lang`-файлами: локаль каждого файла берётся из его
имени (`lang/en.lang` → `en`, `lang/ru.lang` → `ru`):

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
полноценный компонент — например, имя игрока с hover-событием — и оно сохранится при подстановке
(собственный явный стиль аргумента всё равно переопределяет фоновый стиль от окружающих тегов).

Весь шаблон разбирается парсером за один проход, а не по кускам, поэтому незакрытый
цветовой/стилевой тег продолжает действовать и на плейсхолдер, идущий после него:

```
# оба {0} будут красными — "<red>" не закрыт, значит действует до конца строки
greeting="<red>Hello, {0}!"
```

По-настоящему самозакрывающийся тег (`<red/>`) — это, как и в любой другой MiniMessage-строке,
пустая область нулевой ширины: он ничего не красит, потому что между открывающей и закрывающей
частью ничего нет. Чтобы ограничить цвет частью строки, закрывайте тег явно
(`<red>Hello</red>, {0}!`).

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

Как и обычный `Translator`, `ComponentTranslator` можно зарегистрировать дефолтным, чтобы
`Audience`/`Player`-функции вообще не требовали его передачи:

```kotlin
ComponentTranslator(translator).makeDefault()

// нигде ниже нет аргумента ComponentTranslator:
audience.sendTranslation("greeting", Component.text(player.name))
audience.sendTranslationFor("ru", "greeting", Component.text(player.name))
player.sendTranslation("greeting", Component.text(player.name))   // по-прежнему учитывает локаль через resolveLocale
player.render("greeting", Component.text(player.name))
player.translate("greeting", player.name)                         // использует Translator.default, не ComponentTranslator.default
```

`ComponentTranslator.requireDefault()` выбрасывает `IllegalStateException`, если `makeDefault()`
ещё не вызывался. Обратите внимание: `player.translate(...)` (та, что возвращает обычную `String`)
читает `Translator.default`, а не `ComponentTranslator.default` — зарегистрируйте оба, если
используете оба варианта.

## Свои MiniMessage-теги

`loadTags` читает файл в формате `.lang` и регистрирует каждую запись как MiniMessage-тег, так
что `<key>` становится доступен в любом литеральном тексте `.lang` — без дополнительной настройки,
каждый `ComponentTranslator`, построенный из этого `Translator`, подхватывает теги автоматически:

```
# lang/values.lang
lumen_green="#297D3F"
prefix="<gray>[<gold>Server</gold>]</gray> "
```

```kotlin
val translator = Translator.create("en")
    .loadLanguage("en", Path.of("lang/en.lang"))
    .loadTags(Path.of("lang/values.lang"))

val messages = ComponentTranslator(translator) // уже знает <lumen_green> и <prefix>

// greeting="<lumen_green>Hello</lumen_green>, {0}!"
// announce="<prefix>Server restarting in {0} minutes."
```

Значение, похожее на hex-цвет (`#rrggbb` или `rrggbb`), становится настоящим цветовым тегом — а
не просто текстом — так что `<lumen_green>text</lumen_green>` реально красит `text`. Любое другое
значение вставляется как есть и повторно разбирается как MiniMessage, так что может само содержать
разметку (как `prefix` выше). Теги подхватываются `ComponentTranslator`/
`ComponentTranslator.miniMessage(...)`; передайте явный инстанс `MiniMessage` в
`ComponentTranslator.miniMessage(translator, mm)`, чтобы отказаться от этого. У
`ComponentTranslator.legacy(...)` MiniMessage вообще не используется, поэтому свои теги там
не применяются.

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

Каждая функция перевода/рендера/отправки с приёмником `Player` автоматически использует
`resolveLocale`, так что ни одной из них не нужен параметр локали — только `translator`/
`messages`, ключ и аргументы плейсхолдеров:

```kotlin
player.sendTranslation(messages, "greeting", Component.text(player.name))       // рендерит и отправляет Component
player.sendTranslationLines(messages, "motd", Component.text(player.name))      // ...отдельным сообщением на строку

player.render(messages, "greeting", Component.text(player.name))                // рендерит без отправки —
player.renderLines(messages, "motd", Component.text(player.name))               // например, для display name предмета

player.translate(translator, "greeting", player.name)                           // обычная String, без Component вообще
player.translateLines(translator, "motd", player.name)
```

Это отдельные, более специфичные перегрузки по сравнению с locale-агностичными
`Audience.sendTranslation`/`Translator.translate` из примеров выше (те всегда используют
`Translator.defaultLocale`): вызов одного из этих имён на значении, статически типизированном
как `Player`, выбирает версию с `resolveLocale`, на обычном `Audience`/`CommandSender` (например,
консоли) или при прямом вызове метода `Translator`/`ComponentTranslator` — locale-агностичную.
Override хранится только в памяти и не переживает перезапуск сервера — сохраняйте его сами и
вызывайте `setLocale` заново при входе, если это нужно.

Каждая из них тоже полностью опускает аргумент `translator`/`messages`, если
[зарегистрирован дефолтный инстанс](#инстанс-по-умолчанию):

```kotlin
player.sendTranslation("greeting", Component.text(player.name))   // ComponentTranslator.default
player.render("greeting", Component.text(player.name))            // ComponentTranslator.default
player.translate("greeting", player.name)                         // Translator.default
player.resolveLocale()                                             // Translator.default
```

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
