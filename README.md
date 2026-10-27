# sabren/IntelliK

> An IntelliJ IDEA plugin for the K programming language (v3),
> written in June 2016. The lexer is the most thoughtful part; the
> rest is a well-structured snapshot frozen mid-development — and it
> cannot be built or installed from a fresh clone without regeneration
> work.
> Repo: https://github.com/sabren/IntelliK — private, default branch
> `master`, 12 commits (2016-06-14 → 2016-06-19), all by Michal.
> GitHub description: "K plugin for IntelliJ".
> **Verdict: KEEP, document** (triage decision 2026-10-24; a full code
> review already exists — see below).

## What it is

A JetBrains "custom language" plugin built on the platform SDK's
language-plugin architecture, following the canonical layout. Package
`com.x1010data.intellik` — the K dialect here is the one associated
with 1010data. Registered in `resources/META-INF/plugin.xml` (id
`com.x1010data.intellik`, name "K Plugin", since-build **141.0** =
IntelliJ IDEA 15, 2015-era platform). About 754 lines of hand-written
code, plus a 95-line JFlex lexer definition and a 43-rule Grammar-Kit
BNF grammar.

| Piece | File(s) | Notes |
|---|---|---|
| Language + file type (`.k`) | `KLanguage.java`, `KFileType.java`, `KFileTypeFactory.java` | Standard singletons |
| Lexer | `K.flex` → generated `KLexer`, wrapped by `KLexerAdapter` | See lexer note below |
| Grammar | `K.bnf` | Statements, lambdas `{[x;y] …}`, lists/dicts, `if`/`do`/`while`, amend/cond |
| Parser wiring | `KParserDefinition.java` | References generated `parser.KParser` + `psi.KTypes` |
| Syntax highlighting | `KSyntaxHighlighter.java`, `KSyntaxHighlighterFactory.java`, `KColorSettingsPage.java` | 15 token classes (comments, numbers, strings, symbols, prims, adverbs, builtins, …) |
| Completion | `KCompletionContributor.java` | Stub — see findings |
| Structure view | `KStructureViewFactory/Model/Element.java` | Empty — see findings |

## The lexer: the genuinely good part

K uses `/` both as comment-to-end-of-line and as an adverb, so
`K.flex` runs a two-state machine: at `YYINITIAL` (start of a line /
after whitespace) `/…` lexes as COMMENT and `\…` as COMMAND; after
any "real" token it switches to the `INLINE` state where `/` and `\`
lex as adverbs. That is the hard problem in K tooling, and it is
handled correctly in principle. The BNF is compact but covers the
language's core forms, including the `.[…]` amend and `:[…]`
conditional syntax.

## Findings (from the code review)

1. **It cannot be compiled from a fresh clone — the generated sources
   were never committed.** `KParserDefinition.java` imports
   `com.x1010data.intellik.parser.KParser` and
   `com.x1010data.intellik.psi.KTypes`; `psi/impl/KPsiImpUtil.java`
   imports `psi.KItem`; `KStructureViewElement.java` imports
   `psi.KDefine`; `KLexerAdapter.java` instantiates the
   JFlex-generated `KLexer`. None of these exist in the repo.
   `IntelliK.iml` declares a `gen` source folder for the
   Grammar-Kit output — the folder is absent and isn't even in
   `.gitignore`; it only ever existed on the author's machine.
   Building requires regenerating the parser (Grammar-Kit on `K.bnf`)
   and the lexer (JFlex on `K.flex`, using the bundled
   `idea-flex.skeleton`) inside an IDEA with the Plugin DevKit.
2. **`plugin.xml` still carries tutorial placeholder metadata:**
   vendor "YourCompany", `support@yourcompany.com`,
   `http://www.yourcompany.com`, version `1.0`, change-notes
   "Initial version." It was never packaged for anyone else to
   install; there is no built `.jar`/`.zip` in the repo.
3. **Completion is a joke stub.** `KCompletionContributor` offers
   exactly one completion for identifiers: the literal string
   `"youCompleteMe"`. The author's own commit message says it
   plainly: "added (extremely dumb) completion 'helper'".
4. **The structure view is empty.** `KStructureViewElement.getChildren()`
   has its real body commented out behind a `/* TODO … */` and
   returns `EMPTY_ARRAY` unconditionally (commit: "added (empty)
   structure view for K Files"). Relatedly, `getAlphaSortKey()` and
   `getPresentation()` are annotated `@NotNull` but return `null`
   for non-named elements — platform contract violations that can
   throw inside the IDE.
5. **Stray dead import:** `KSyntaxHighlighter.java` imports
   `com.sun.jna.platform.unix.X11` and never uses it — leftover from
   an edit, and the kind of thing that breaks compilation outside the
   full platform classpath.
6. **Era-locked build:** no Gradle/Maven — an IDEA `.idea` project
   with a `PLUGIN_MODULE` DevKit module (`IntelliK.iml`). The modern
   intellij-platform Gradle plugin workflow postdates this code.
   `KParserDefinition.spaceExistanceTypeBetweenTokens` is a platform
   method name that later platform versions changed/removed, so even
   with the generated sources, a current IDEA would reject parts of
   this API surface.
7. **No tests, no README.** The only documentation is the code, the
   grammar, and the commit log (which is refreshingly honest: "huge
   pile of lexer/parser bugfixes", "simplified grammar…", "fix
   KPsiImpUtil (KAssign no longer exists)").

## Loading it today is a port, not an install

Even with an IDEA present, installing IntelliK "as-is" is not
currently possible, for two independent reasons: there is nothing to
install (no built plugin jar, and the sources don't compile until
the Grammar-Kit/JFlex generated code is regenerated), and the plugin
targets since-build 141 (IDEA 15, 2015) with a 2016 API surface that
has since drifted. Realistically: recreate the project with the
current intellij-platform Gradle plugin, regenerate parser/lexer, fix
the API drift, then run it in the sandbox IDE. That is an afternoon
of archaeology, not a double-click. (The review also notes IntelliJ
IDEA is not installed on the VM where the review was done, so no
install was attempted there.)

## Tech stack

Java, IntelliJ Platform SDK (DevKit module build, IDEA 15 era),
JFlex (`idea-flex.skeleton` bundled), Grammar-Kit BNF. No
Gradle/Maven, no license file, no tests.

## Inventory

| Path | Contents |
|------|----------|
| `resources/META-INF/plugin.xml` | Plugin registration (id, name, since-build 141.0, YourCompany vendor) |
| `src/com/x1010data/intellik/K.flex` | 95-line two-state lexer definition |
| `src/com/x1010data/intellik/K.bnf` | 43-rule grammar |
| `src/com/x1010data/intellik/*.java` | Language, file type, parser definition, highlighter, completion, structure view, lexer adapter |
| `src/com/x1010data/intellik/psi/` | `KElementType`, `KFile`, `KTokenType`, `impl/KPsiImpUtil` |
| `src/com/x1010data/intellik/icons/k-icon.png` | Plugin/file icon |
| `idea-flex.skeleton` | JFlex skeleton for lexer generation |
| `IntelliK.iml`, `.idea/` | IDEA DevKit module project (declares the absent `gen` folder) |

## Surprising / notable

- **The commit log is the documentation, and it's honest.** Six days
  of work, mostly lexer/parser bugfixing, ending with two wry
  admissions — an "extremely dumb" completion helper and an "empty"
  structure view. The plugin stopped exactly where the hard
  infrastructure ended and the product features would have begun.
- **The highlighter's token taxonomy matches K's actual lexical
  structure** (primitives vs. builtins vs. adverbs vs. commands) —
  the author knew the language, not just the SDK tutorial.
- No linked streams or videos were found in the repo.

## Related

Full code review (2026-10-22, including the install-attempt
verdict): `~/workspace/repo-curation/reviews/sabren-intellik-review.md`.

## Verdict

**Keep, document** (this file). The value is as a design record:
the two-state lexer solution and the compact grammar are worth
preserving, and the plugin's architecture is clean enough to port if
K tooling is ever wanted again. It is not, in its current state, a
working plugin — and this doc does not pretend otherwise.
