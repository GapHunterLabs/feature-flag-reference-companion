<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Feature Flag Reference Companion Changelog

## [Unreleased]

## [0.1.1]

### Added

- Review/star CTA: after 10 distinct orphan-candidate findings (never
  counted for the informational IN_USE/NOT_YET_SCANNED icons, only
  real actionable ones), a one-time notification asks whether to rate
  the plugin on Marketplace, with a permanent "Don't ask again"
  option. Standard mechanism used catalog-wide since 2026-08-24
 , rolled out to this plugin now.

## [0.1.0]

### Added

- **Gutter icon next to every recognized feature-flag-check call's key
  literal** in Java and Kotlin code, showing whether that exact key is
  referenced anywhere else in the open project.
- **Recognized SDK method names (matched by simple name, not resolved
  symbol)**: `isEnabled`, `isFeatureEnabled`, `isFlagEnabled`,
  `getFlag`, `getBooleanFlag`, `variation`, `boolVariation`, `isOn`,
  `evaluateFlag` -- common shapes across LaunchDarkly, Unleash,
  ConfigCat, and in-house wrappers.
- **Manual refresh, not per-keystroke**: project-wide reference
  counting runs only when "Refresh Feature Flag References" (Find
  Action or Tools menu) is explicitly triggered -- never recomputed on
  every edit. Before the first refresh, call sites show a neutral
  "not yet scanned" icon instead of a guessed verdict.
- **Exact key text matching only**: a flag counted as referenced
  requires the exact same key text, never a substring match.
- **Honest handling of no flag pattern**: a project with no recognized
  flag-check calls produces no gutter icons and no crash.
- 100% static PSI analysis -- no network call, no provider connection,
  no telemetry.

[Unreleased]: https://github.com/GapHunterLabs/feature-flag-reference-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/feature-flag-reference-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/feature-flag-reference-companion/commits/0.1.0
