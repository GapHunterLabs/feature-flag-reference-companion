# Privacy Policy — Feature Flag Reference Companion

**Effective date:** 2026-08-19

Feature Flag Reference Companion is a Gap Hunter Labs plugin for
IntelliJ Platform IDEs. This policy is short because the plugin's
design makes it short: there is nothing to disclose beyond what's
below.

## What this plugin collects

**Nothing.** Feature Flag Reference Companion does not collect, store,
transmit, or sell any data — no source code, no file contents, no file
paths, no usage analytics, no telemetry, no crash reports, no
personally identifiable information. Java/Kotlin source text read from
your local project exists only in memory for as long as the IDE is
open, and only long enough to compute each flag key's reference count.

## Network access

**None.** Feature Flag Reference Companion makes zero network calls
during normal operation. Every reference count shown is computed
directly from files already present on your local disk — no
LaunchDarkly/Unleash/ConfigCat/any provider API call, ever. The plugin
never connects to any feature-flag provider or service.

## Third parties

None. Feature Flag Reference Companion has no third-party SDKs, no
analytics libraries, no ad networks, no external dependencies that
phone home. Java/Kotlin parsing uses only the bundled Java plugin's PSI
and the bundled Kotlin plugin's PSI.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
