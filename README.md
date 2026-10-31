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
- A structure view listing a file's top-level definitions.
- Go to declaration (Ctrl+B / Ctrl+click), with K3 scoping:
  - Inside a lambda, a name is first looked up among the lambda's parameters (`{[a;b] ...}`), or
    the implicit `x`, `y` and `z` when it has no signature. Implicit parameters go to the lambda.
  - Next come names assigned in that same lambda. `name:expr` inside a lambda is local, while
    `name::expr` assigns a global.
  - K3 has no closures, so a nested lambda doesn't see the enclosing lambda's locals.
  - Otherwise a name refers to a global: an assignment outside any lambda, or a `name::expr`
    anywhere. The current file is searched first, then the other `.k` files in the project.
  - The first assignment of a name is its declaration, and later assignments (including indexed
    ones like `a[i]:v`) count as uses.
- Rename (in place or with the dialog), Find Usages, and highlighting of a name's uses in the
  file. Rename accepts only valid K identifiers and refuses keywords and builtins.
- Refactorings, from the usual Refactor menu actions and shortcuts:
  - **Extract Function** (Ctrl+Alt+M) works on a selected expression or on whole statements. It
    creates a new function before the enclosing top-level statement and replaces the selection
    with a call such as `name[a;b]`, or `name[]` when there are no parameters.
    - Names that are parameters or locals of the enclosing lambda become parameters, in order of
      first use. Globals stay as they are.
    - With no parameters the function is `name:{...}`.
    - With one to three parameters and **Use implicit x/y/z parameters** checked (the default),
      the parameters are renamed to `x`, `y`, `z` and the signature is left out: `name:{x+y}`.
      Unchecked, or with more than three parameters, the signature is explicit and keeps the
      original names: `name:{[a;b] a+b}`.
    - If the selection already uses `x`, `y` or `z` for something else (a global, or a local it
      declares), the implicit form would change its meaning, so the signature is explicit
      (`{[] ...}` when there are no parameters).
  - **Introduce Variable** (Ctrl+Alt+V) puts `name:expr` just before the current statement, in the
    same scope.
  - **Inline Variable / Inline Function** (Ctrl+Alt+N) works on a name that is assigned once.
    - A variable's value replaces every use.
    - A function whose body is a single expression has each call `f[a;b]` (or `f a` for one
      argument) replaced by its body, with the arguments substituted.
    - The definition is then removed.
  - **Safe Delete** works on an unused definition.

  K evaluates right to left, so a complete subexpression is either a single term or the
  right-hand end of an expression: in `a-b+c`, you can select `b+c`, `c` or `b`, but not `a-b`.
  When inlining, the plugin adds parentheses where they are needed: when the inlined code becomes
  a left operand, is indexed, or sits next to another value as in `1 2 (3+4)`. A refactoring that
  would change behavior is refused with an explanation, for example:
  - a local assigned in the selection is used after it;
  - a name is reassigned or modified in place;
  - a name would mean something else at its new location;
  - a call doesn't match the function's parameters.

  The plugin doesn't check side effects. Inlining can repeat or reorder evaluation, and Introduce
  Variable evaluates its expression before the rest of the statement.
- Completion of the active dialect's builtins (`_bin`, `_sqrt`, ...) and keywords, from the
  dialect's primitive table. Completion also offers the names visible at the caret.
- Not yet supported: dotted names (`.k.foo`, `a.b`), names written as symbols (`` `a ``), and
  K3 directories (`\d`).

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
| `src/com/tangentcode/intellik/*.java` | Language, file type, parser definition, syntax highlighter, color settings page, completion, structure view, name resolution (`KResolver`, `KReference`), find usages and rename support |
| `src/com/tangentcode/intellik/dialect/` | `KDialect`, `KDialectSettings`, `KDialectService`, `PrimitiveTable` |
| `src/com/tangentcode/intellik/psi/` | Hand-written PSI support: element and token types, `KFile`, `KNamedElement`, `KElementFactory`, and the mixins in `impl/` for assignments, parameters and name references |
| `resources/META-INF/plugin.xml` | Plugin registration |
| `resources/dialects/<id>.json` | One primitive table per dialect. Only `k3.json` exists so far. |
| `test/com/tangentcode/intellik/` | Lexer, parser, completion, dialect, structure view, resolve and refactoring tests |

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
