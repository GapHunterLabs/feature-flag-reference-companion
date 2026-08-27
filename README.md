# Feature Flag Reference Companion

IntelliJ-family plugin. Gutter icon next to every recognized
feature-flag-check call's key literal in Java/Kotlin code
(`isEnabled("checkout_v2")`, `variation("checkout_v2", false)`, and
similar), showing how many times that exact key is referenced anywhere
else in the open project — helps find orphaned flags (dead code left
behind after a rollout completed) without connecting to any provider.
100% static PSI analysis: no network call, no LaunchDarkly/Unleash/
ConfigCat/any provider connection, ever.

## Why it exists

An original idea, not a port of an existing competitor — validated
against this catalog's own idea-validation discipline before
being built. The 3 existing feature-flag plugins on JetBrains
Marketplace are all clients that **connect to a provider's service**
to check a flag's live state — this plugin is deliberately the
opposite: 100% static analysis of references already in your code, no
provider connection, complementary rather than competing. Same
"apuesta consciente sin ancla de mercado" treatment as Refactor
Simulator / Test Scaffold Companion / Circular Dependency Companion /
Unused npm Script Companion: v0.1 ships free, no time/marketing
investment disproportionate to real demand signal until there's
evidence of adoption.

## Recognized SDK method names (v0.1 scope, stated honestly)

Matched by **simple method name only**, never a resolved SDK
symbol/type — the same detection works whether LaunchDarkly, Unleash,
ConfigCat, or an in-house `FeatureFlags` helper is actually on the
classpath:

- `isEnabled`, `isFeatureEnabled`, `isFlagEnabled` — Unleash and common
  generic/home-grown wrapper conventions.
- `variation`, `boolVariation` — LaunchDarkly.
- `getFlag`, `getBooleanFlag`, `evaluateFlag`, `isOn` — other common
  generic/home-grown conventions.

**Deliberately excluded:** `getValue`, `get`, `check`, `test`, `is`
alone — names generic enough that matching them by simple name across
an entire project would produce far more false positives (unrelated
calls with an unrelated first string argument) than real feature-flag
detections. A project using a differently-named wrapper around one of
these SDKs isn't detected — that's a real, documented limitation, not
a bug.

The flag key is always read from the call's **first** argument (every
covered SDK method takes the key first) — and only when it's a plain
literal. A Kotlin string template with interpolation
(`"checkout_${env}"`, a segment with a dollar sign followed by curly
braces) is never treated as an exact key: it's not fixed text, so this
plugin honestly declines to guess at one rather than silently matching
the wrong thing.

## Languages covered in v0.1

**Java and Kotlin.** JavaScript/TypeScript feature-flag SDKs
(LaunchDarkly's `client-side-sdk`, Unleash's `unleash-proxy-client`)
are a real, common shape too, but are **out of scope for v0.1** —
deferred to a possible future version, not silently unsupported. A
project with no Java/Kotlin flag-check calls at all produces no gutter
icons and no error.

## Manual refresh, not per-keystroke

Counting every reference to a flag key across the **whole open
project** is real work — scanning every `.java`/`.kt` file under the
project's content roots. Running that inside the daemon's per-edit
highlighting pass would silently reintroduce exactly the kind of
editor lag this catalog already avoids everywhere else (heavy
computation always off the EDT — this goes one step further and keeps
the whole scan off the *hot path entirely*, not just off the EDT).

- The reference count is computed **only** when you explicitly run
  **Refresh Feature Flag References** (Find Action, or Tools menu).
- Until the first refresh, every recognized call site shows a neutral
  "not yet scanned" icon — never a guessed verdict.
- After editing code that adds or removes a flag reference, the gutter
  icons stay at their last-computed count until you refresh again —
  by design, the same manual-refresh principle already proven by
  `circular-dependency-companion`'s own project-graph scan.

## How the verdict is decided

For each recognized flag-check call site, the count is **every call
site project-wide (across all files) that uses the exact same key
text** — including the call site being annotated itself, so the
minimum possible count is 1.

- **Count == 1** (only this call site) → **orphan candidate**
  (`AllIcons.General.InspectionsWarningEmpty`) — a real candidate for
  cleanup, since nothing else in the project references this key.
- **Count >= 2** → **in use**
  (`AllIcons.General.InspectionsOKEmpty`).

**Exact text matching only, never a substring.** A flag named
`checkout_v2` is never counted as referenced by a call using
`checkout_v2_extended` — different key, different flag, matched
independently.

## Honest handling of no flag pattern

A project with no recognized feature-flag-check calls anywhere
produces no gutter icons and no error — never a crash, never a
misleading state. There's simply nothing to annotate.

## Why a gutter icon, not an inlay hint

Same reasoning already proven by `highlight-companion`'s
cognitive-complexity icon and `unused-npm-script-companion`'s
used/orphaned icon: this feature is a per-line **verdict** about a
whole call site (referenced vs. orphan candidate), not a value
attached to inline text at a specific token. A column of icons down
the gutter is scannable top-to-bottom for the actual task ("which of
these flag checks can I safely remove?") in a way inline text after
each line is not.

## Why built this way

- **Name-based detection, not resolved-symbol-based** — same principle
  already proven by `http-status-inline-companion`'s
  `HttpSignalNames`/`JavaHttpStatusFinder`/`KotlinHttpStatusFinder`:
  resolving every call site's method to a concrete class/type is
  expensive on a pass that already has to visit every file in the
  project, and a fixed method-name convention (`isEnabled`,
  `variation`, ...) generalizes better across SDKs than a hardcoded
  list of resolved framework types would.
- **`LineMarkerInfo` anchored on a real leaf PSI element from the
  start** — never the composite literal node itself. Applied
  proactively in both the Java and Kotlin code paths,
  not discovered live: a `PsiLiteralExpression`/`KtStringTemplateExpression`
  passed directly to `LineMarkerInfo` triggers a "Performance warning"
  that fails tests and spams the log in a real IDE session.
- **Manually refreshed project-wide index, not a per-keystroke scan** —
  same principle already used by `circular-dependency-companion`'s
  `ProjectGraphAnalyzer`: heavy computation stays out of the daemon's
  hot path entirely, refreshed only on an explicit user action.
- **Extraction is dumb, interpretation is smart.** `JavaFlagCheckFinder`/
  `KotlinFlagCheckFinder` only extract `FlagCheckCall`s from PSI;
  deciding the verdict lives in `FlagReferenceIndex`/
  `FlagReferenceResult`, decoupled from any PSI/VFS dependency — same
  split this catalog already uses everywhere.

## v0.1 scope

Free, all of it — no paywall, nothing held back for a future tier.
Deferred to a possible future v0.2 (not started, not promised):
JavaScript/TypeScript SDK coverage, and multi-repo scanning (useful in
microservice architectures where a flag is referenced across several
repos, per the original idea's own validation note).

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
