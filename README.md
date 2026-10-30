# IntelliK

IntelliK is an IntelliJ Platform plugin for the [K](https://en.wikipedia.org/wiki/K_(programming_language))
family of array languages.

K3 is the only dialect supported today. The goal is to support several dialects from one shared
lexer and parser, with a primitive table for each dialect: q/k4, Kona, K#, ngn/k, oK and k9.

## Features

- Syntax highlighting for `.k` files. Colors for comments, numbers, strings, symbols, primitives,
  adverbs, builtins, keywords, commands and brackets can be changed in
  *Settings → Editor → Color Scheme → K*.
- A parser for K3: statements, lambdas with signatures, lists, dicts, amend `.[...]`, conditional
  `:[...]` and `if`/`do`/`while`. The lexer handles `/` and `\` being either a comment or command
  (at the start of a line or after whitespace) or an adverb (after a token).
- A structure view. It is only a skeleton so far: it shows the file node but no definitions yet.
- Completion of the active dialect's builtins (`_bin`, `_sqrt`, ...) and keywords. The list comes
  from the dialect's primitive table.

## Building and running

You need JDK 21. The Gradle wrapper downloads everything else, including IntelliJ IDEA 2025.3,
which the build targets (since-build 253).

```sh
./gradlew build        # generate the lexer/parser, compile, and run the tests
./gradlew test         # run the tests only
./gradlew runIde       # start a sandbox IDE with the plugin installed
./gradlew buildPlugin  # package the plugin zip
```

`./gradlew buildPlugin` writes the installable plugin zip to `build/distributions/`. To install it, use
*Settings → Plugins → ⚙ → Install Plugin from Disk*.

The lexer (`K.flex`, JFlex) and the parser (`K.bnf`, Grammar-Kit) are generated on every build into
`build/generated/` and are never committed. The grammar files are the source of truth.

## Project layout

| Path | Contents |
|---|---|
| `src/com/tangentcode/intellik/K.flex` | JFlex lexer definition |
| `src/com/tangentcode/intellik/K.bnf` | Grammar-Kit grammar. It generates `parser.KParser` and the `psi` classes. |
| `src/com/tangentcode/intellik/*.java` | Language, file type, parser definition, syntax highlighter, color settings page, completion, structure view |
| `src/com/tangentcode/intellik/dialect/` | `KDialect`, `KDialectSettings`, `KDialectService`, `PrimitiveTable` |
| `src/com/tangentcode/intellik/psi/` | Hand-written PSI support: element and token types, `KFile`, `impl/KPsiImpUtil` |
| `resources/META-INF/plugin.xml` | Plugin registration |
| `resources/dialects/<id>.json` | One primitive table per dialect. Only `k3.json` exists so far. |
| `test/com/tangentcode/intellik/` | Lexer, parser, completion, dialect and structure view tests |

## Dialects and primitive tables

- **`KDialect`** lists the dialects IntelliK knows about. Each has a stable id: `k3`, `q`, `kona`,
  `ksharp`, `ngn`, `ok`, `k9`.
- **`KDialectSettings`** is a project-level setting, saved in `.idea/intellik.xml`. It defaults to K3.
  There is no settings UI yet.
- **`KDialectService`** decides which dialect a file uses. For now that is always the project setting.
  Planned additions are a per-file override, a `/ dialect: ...` header comment, and mapping the `.q`
  extension to q.
- **`PrimitiveTable`** loads `resources/dialects/<id>.json` for a dialect. A dialect without a file
  gets an empty table. A table has four lists, and each entry has a `name` and an optional
  `description`:

  ```json
  {
    "dialect": "k3",
    "name": "K3",
    "primitives": [{"name": "+", "description": ""}],
    "adverbs":    [{"name": "/:", "description": ""}],
    "builtins":   [{"name": "_sqrt", "description": ""}],
    "keywords":   [{"name": "while", "description": ""}]
  }
  ```

  The lexer reads `builtins` to recognize builtin names. Completion offers `builtins` and
  `keywords`. Glyph, adverb and keyword lexing rules are still written in `K.flex`, so in the table
  those lists don't drive lexing yet.

To add a dialect, write `resources/dialects/<id>.json` for an id that `KDialect` already has.

## History

Michal Wallace started IntelliK in June 2016 as a K3 plugin, under the package
`com.x1010data.intellik`. In 2026 it got a Gradle build, ports to the current IntelliJ APIs, tests,
and the move to the `com.tangentcode.intellik` package.

## License

MIT. See [LICENSE](LICENSE).
