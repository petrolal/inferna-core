<!--
  ~ Inferna Core - Biopunk JVM & Spring Developer Environment
  ~ Copyright (C) 2026 Lucas Henrique Novais de Araújo Petrola <petrolalucas@gmail.com>
  ~ SPDX-License-Identifier: GPL-3.0-or-later
  -->
# Inferna Core — Freemium Open-Core Architecture (Option A)

> Records the decision to ship Inferna first as a plugin (Community + Ultimate),
> open-core/freemium. This repository, `inferna-core`, is public under
> GPL-3.0-or-later and contains **only** the free tier plus a neutral licensing
> SPI (§2–§3) — no commercial logic, no mock entitlement rules, nothing that
> would leak or weaken a paid offering by being open-sourced. Complements
> `docs/STANDALONE_ROADMAP.md` (the separate, longer-horizon plan for a
> standalone distribution); that roadmap is **partly stale** against the
> current `master` (see §1.4) and should be re-audited before its Phase 2+
> work resumes.
>
> **2026-10-20 update:** the first pass at this architecture (§3, original)
> added an in-repo `org.inferna.pro` package — license mock, feature-flag
> plumbing, and three unimplemented Pro extension points — directly inside
> this now-public repository. That package has been **removed**. A
> public GPLv3 repo is not the place for any of a commercial tier's shape,
> even unimplemented: it couples the open core to decisions that belong to a
> closed-source `inferna-pro`, and ships infrastructure (a `PropertiesComponent`
> flag flipping on "Pro") that has no legitimate reason to exist in public
> source. §2–§4 below describe the replacement: a minimal, neutral SPI with no
> opinion about what a paid tier is or does.

---

## 1. Diagnosis (audit of commit `d5113cb`, 2026-10-19)

### 1.1 Build (`build.gradle.kts`, `gradle.properties`)

- Gradle plugin `org.jetbrains.intellij.platform` 2.2.1, Kotlin 2.2.20, JVM
  toolchain 21. `platformVersion=2025.3`, `sinceBuild=253`, `untilBuild` unset
  (no upper cap).
- **Blocking issue found:** the `else` branch of `localIdeDir` resolution (hit
  whenever no local Nix-provided IDE is found, e.g. plain CI) called
  `intellijIdeaUltimate(...)`. That makes the plugin's own build/test/verify
  pipeline — not just the local dev shortcut — depend on an Ultimate
  distribution, contradicting the stated goal of running on Community too.
  **Fixed** in this change: switched to `intellijIdeaCommunity(...)`, the
  correct lowest-common-denominator base; Ultimate is a strict superset of
  Community's API surface, so nothing written against Community breaks on
  Ultimate.
- `com.intellij.java`, `org.jetbrains.kotlin`, `com.intellij.properties` are
  bundled plugins available in both editions — already correct, no change
  needed there.
- No third-party marketplace plugin dependency exists today (Scala/Explyt
  Spring/Docker, previously documented in `STANDALONE_ROADMAP.md`, are gone
  from the current `build.gradle.kts` — that document predates their
  removal).

### 1.2 `plugin.xml`

- `<depends>` are all Community-safe: `com.intellij.modules.platform`,
  `com.intellij.modules.java`, `org.jetbrains.kotlin`, `com.intellij.properties`.
  No hard dependency on `com.intellij.spring` or any other Ultimate-only
  module — Ultimate detection is already done correctly at **runtime**
  (`SpringBeanResolver.isNativeSpringPluginActive()` via `PluginManagerCore`),
  not via a compile-time `<depends>`. This was already right; nothing to fix
  here.
- Registers: 1 `applicationListener` (theme/branding lifecycle), 1
  `themeProvider` (`targetUi="islands"`, Islands-only by product decision,
  see `STANDALONE_ROADMAP.md` §6.2), 1 `bundledColorScheme`, 1
  `codeInsight.lineMarkerProvider` (UAST, polyglot Java+Kotlin), 1 right-docked
  `toolWindow`, 1 `psi.referenceContributor`, 1 `localInspection`.
- No extension point existed for future paid features, and no licensing
  concept existed anywhere in the codebase — the Pro tier had no seam to grow
  from. **Added** in this change (§3).

### 1.3 Theme (`inferna.theme.json`, `inferna.xml`) and icons

- `inferna.theme.json` (302 lines): `parentTheme: ExperimentalDark`,
  `targetUi: islands`, full biopunk palette (`CanvasVoid #070203`,
  `ArterialRed #b81424`, `ArterialBright #ff1e38`, etc.), two `ToolWindow`
  color blocks (dark/light variants at lines 96 and 223). Broad coverage:
  Editor, EditorTabs, ToolWindow, Terminal/Block Terminal, MainToolbar,
  StatusBar, Popup, ScrollBar, ProgressBar, Tree/List.
- `inferna.xml` (494 lines): editor color scheme, `parent_scheme="Darcula"`,
  general syntax + Kotlin-specific overrides + full terminal ANSI palette.
- Icons: a single hand-authored source (`assets/icons/`) now fans out via
  Gradle (`syncResourceIcons`/`syncLauncherIcons`/`syncBrandingIcons`) into
  `src/main/resources/icons/` and `launcher/icons/` — the 3-tree drift problem
  noted in `STANDALONE_ROADMAP.md` is already resolved.
- Ultimate-exclusive tool windows (Endpoints, Database, Profiler) have **no**
  theme coverage today; they fall back to the parent theme
  (`ExperimentalDark`) rather than Inferna's palette. Tracked as a Pro-tier
  follow-up (§4), not fixed in this pass — it's a design/asset task (new
  icon/color mappings), not a mechanical one.

### 1.4 Kotlin/Java code

| File | Tier | State |
|---|---|---|
| `core/InfernaLifecycleListener.kt` | Free | Applies theme + splash/icon branding on boot and on LaF change. Functional, documented EDT/splash-cache workarounds. |
| `dashboard/InfernaDashboardToolWindowFactory.kt` | Free | JVM heap, live compilation status, live Spring bean count. |
| `icons/InfernaIcons.kt` | Free | 2 icons registered (Dashboard, SpringBean). |
| `spring/SpringBeanLineMarkerProvider.kt` + `SpringBeanResolver.kt` + `SpringValueReferenceContributor.kt` + `UnresolvedSpringBeanInspection.kt` | Free | Field + constructor `@Autowired` navigation, `@Qualifier`/`@Primary` disambiguation, `@Value` → `.properties` navigation, unresolved/ambiguous-bean inspection with `@Qualifier` quick-fixes. All correctly defer to Ultimate's native `com.intellij.spring` when present. |
| `spring/interop/*.java` | Free | Java interop bridges around Kotlin-`internal` platform APIs (`JvmElementActionFactories`, `UastReferenceRegistrar`). |

This is a correctly-scoped, already-free "Community" feature set: no file in
the pre-existing tree does anything that belongs in a paid tier. There was
simply no code expressing the Free/Pro boundary — this change adds that
boundary without touching any existing behavior.

`STANDALONE_ROADMAP.md` is stale in two concrete ways worth flagging for
whoever resumes it: it still documents `restrictToInfernaOnly()`/theme-lockdown
logic that no longer exists in `InfernaLifecycleListener.kt`, and it describes
third-party Scala/Explyt/Docker plugin dependencies that are no longer in
`build.gradle.kts`. Re-audit it before acting on its Phase 2+ checklists.

---

## 2. Package/directory layout

The free tier's existing packages are untouched. Licensing is a **contract
only**, so it lives inside the Core's own namespace — `org.inferna.core.licensing`
— rather than a separate `org.inferna.pro` tree. There is no package in this
repository that represents "the paid tier"; there is only an extension point a
paid tier elsewhere can implement.

```
src/main/kotlin/org/inferna/
├── core/
│   ├── InfernaLifecycleListener.kt     # Free — lifecycle, theme/branding sync
│   └── licensing/                      # Neutral SPI (new)
│       ├── LicenseTier.kt                     # enum COMMUNITY | PRO
│       ├── InfernaLicenseProvider.kt          # extension-point interface + EP_NAME
│       ├── DefaultInfernaLicenseProvider.kt   # Community-only fallback, registered order="last"
│       └── InfernaLicenseService.kt           # applicationService; delegates to the active provider
├── dashboard/                # Free — Biometrics tool window
├── icons/                    # Free — icon registry
└── spring/                   # Free — UAST Spring navigation/inspection
    └── interop/
```

Rules going forward:

- **This repository ships exactly one `InfernaLicenseProvider` implementation
  — `DefaultInfernaLicenseProvider` — and it only ever returns `COMMUNITY` /
  `false`.** No mock flag, no `PropertiesComponent`-backed override, no
  internal "pretend it's Pro" switch belongs in public source; that kind of
  toggle is exactly the fragility this refactor removes.
- **A commercial tier is a separate plugin outside this repository.** It
  depends on `inferna-core`, implements `InfernaLicenseProvider` with
  `order="first"` relative to the Core's fallback, and ships whatever paid
  feature logic it wants — none of which this repository needs to know
  about, declare an extension point for, or stub out.
- Any future paid feature's *own* extension points (validation, analysis,
  endpoint mapping, or anything else) are declared by that commercial
  plugin, in its own `plugin.xml`, not pre-declared here on spec. Declaring
  them here, even empty, is itself a form of leaking the commercial
  roadmap into public source — this repository does not do that.

---

## 3. Exact changes applied

### 3.1 Build/platform base (2026-10-19 pass)

- `intellijIdeaUltimate(providers.gradleProperty("platformVersion"))` →
  `intellijIdeaCommunity(providers.gradleProperty("platformVersion"))` in the
  `dependencies { intellijPlatform { ... } }` block (the `local(localIdeDir)`
  branch, used whenever a Nix-provided local IDE is found, is unchanged).
- `pluginConfiguration.description`/`changeNotes` reworded from
  "IntelliJ IDEA Ultimate" to "IntelliJ IDEA — both Community and Ultimate" /
  "Community and Ultimate (2025.3 – 2026.x+)".
- No change to `<depends>` — already Community-safe (§1.2).

### 3.2 Public-repo decoupling refactor (2026-10-20 pass)

Removed entirely:

- The `org.inferna.pro` package (`license/LicenseTier.kt`,
  `license/InfernaLicenseService.kt`, `license/DefaultLicenseService.kt`,
  `extensions/PropertiesValidationProvider.kt`,
  `extensions/CircularDependencyAnalyzer.kt`,
  `extensions/EndpointMappingProvider.kt`) — six files, zero callers, deleted
  wholesale.
- The three `org.inferna.pro.*` extension points and the
  `org.inferna.pro.license.InfernaLicenseService` applicationService
  registration in `plugin.xml`.

Added in `src/main/kotlin/org/inferna/core/licensing/` (§2):

- `LicenseTier.kt` — `enum class LicenseTier { COMMUNITY, PRO }`.
- `InfernaLicenseProvider.kt` — the extension-point interface
  (`getTier()`, `isFeatureEnabled(featureId: String)`) plus its
  `ExtensionPointName<InfernaLicenseProvider>` companion
  (`org.inferna.core.licenseProvider`).
- `DefaultInfernaLicenseProvider.kt` — the Community-only fallback; no
  persisted flag, no override path, unconditionally `COMMUNITY`/`false`.
- `InfernaLicenseService.kt` — the `applicationService` interface + impl
  (`InfernaLicenseServiceImpl`) that reads
  `InfernaLicenseProvider.EP_NAME.extensionList.firstOrNull()` and delegates
  to it.

`plugin.xml` changes:

- `<extensionPoints>` now declares exactly one point:
  `<extensionPoint name="licenseProvider" interface="org.inferna.core.licensing.InfernaLicenseProvider" dynamic="true"/>`
  — its fully qualified name resolves to `org.inferna.core.licenseProvider`
  (the plugin `<id>` plus the `name` attribute).
- `<applicationService serviceInterface="org.inferna.core.licensing.InfernaLicenseService" serviceImplementation="org.inferna.core.licensing.InfernaLicenseServiceImpl"/>`
  replaces the old Pro-namespaced service registration.
- A new `<extensions defaultExtensionNs="org.inferna.core">` block registers
  the Core's own fallback on its own extension point:
  `<licenseProvider implementation="org.inferna.core.licensing.DefaultInfernaLicenseProvider" order="last"/>`.
  `order="last"` is what lets an external, higher-priority provider (a
  commercial plugin's own registration, `order="first"`) become
  `extensionList.firstOrNull()` instead, with no code change on either side.

### 3.3 Verification performed

- `./gradlew compileKotlin` — clean, no warnings.
- `./gradlew verifyPluginStructure` — clean (validates the new
  `extensionPoints`/`applicationService`/`extensions` XML and that
  `org.inferna.core.licensing.*` class references resolve).
- `grep` across every git-tracked file for `org.inferna.pro` — zero
  remaining references outside this document's own history section (§1
  intro).

---

## 4. Roadmap (Free → Pro feature map and monetization)

| Capability | Tier | Status |
|---|---|---|
| Biopunk theme + editor scheme (Islands UI) | Free | Shipping |
| Splash/icon branding sync | Free | Shipping |
| Biometrics dashboard (JVM heap, compile status, bean count) | Free | Shipping |
| UAST Spring navigation (`@Autowired` field/constructor → bean) | Free | Shipping |
| `@Qualifier`/`@Primary` disambiguation | Free | Shipping |
| `@Value` → `.properties` navigation | Free | Shipping |
| Unresolved/ambiguous bean inspection + quick-fixes | Free | Shipping |
| Ultimate-native Spring/theme coexistence (no duplicate gutters) | Free | Shipping |
| `application.properties`/`.yml` real-time validation | Pro | Lives in a separate, closed-source `inferna-pro` plugin — not declared here |
| Circular bean-dependency analysis | Pro | Lives in a separate, closed-source `inferna-pro` plugin — not declared here |
| HTTP/REST endpoint mapping (`@RequestMapping` family) | Pro | Lives in a separate, closed-source `inferna-pro` plugin — not declared here |
| Ultimate-only tool window theming (Endpoints/Database/Profiler) | Free (cosmetic parity) | Not started — asset/design task, see §1.3 |
| Licensing SPI (`InfernaLicenseProvider`/`InfernaLicenseService`) | Core (neutral) | Shipping — contract + Community-only fallback only, see §2–§3.2 |
| Real entitlement check (JetBrains Marketplace Licensing API, or any other) | Pro | Implemented by `inferna-pro`'s own `InfernaLicenseProvider`, registered with `order="first"` — this repository has no opinion on how entitlement is checked |

**Monetization sequencing:** this repository's contribution to monetization
ends at the SPI in §2–§3.2. A separate `inferna-pro` repository (closed-source,
distributed outside the JetBrains Marketplace or as a paid Marketplace plugin)
depends on `inferna-core`, implements `InfernaLicenseProvider`, and owns every
feature in the table above plus the real entitlement check. Nothing about that
plugin's shape, feature set, or licensing mechanics needs to — or should —
exist as a stub, mock, or extension point in this public repository ahead of
time; `InfernaLicenseProvider`/`EP_NAME` is the entire surface it needs to
integrate.
