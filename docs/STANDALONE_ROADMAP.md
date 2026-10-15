# Inferna IDE — Standalone Master Roadmap (Android Studio Model)

> Platform engineering technical document. Defines the transition from the
> current stage (a Gradle plugin run via `runIde` on top of a local IntelliJ
> IDEA Community distribution) to a distributable standalone IDE — "Inferna
> IDE" — following the same methodology Google applied to build Android Studio
> on top of `intellij-community`.

---

## 0. Audit of the current codebase (state of fact, 2026-10-15)

This section summarizes what was found during the repository audit and
grounds the phases below. Nothing here is aspirational — it reflects the
actual state of the code at commit `a56da3e`.

### 0.1 Build (`build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`)

- Gradle plugin `org.jetbrains.intellij.platform` **2.2.1**, Kotlin **2.2.20**,
  JVM toolchain **21**.
- `platformVersion=2025.3`, `sinceBuild=253` / `untilBuild=253.*` — the target
  platform is pinned to build `253.x` (2025.3.4), the same one used locally via
  `localIdeDir` (automatic resolution of `/run/current-system/sw/bin/intellij-idea-oss`,
  a NixOS convention). There are no published builds against `251`/`252`,
  despite artifacts under `.intellijPlatform/localPlatformArtifacts` indicating
  earlier testing against `251.23774.435`.
- Plugin dependencies: `com.intellij.java` and `org.jetbrains.kotlin` (bundled),
  plus three marketplace plugins pinned to exact versions: `org.intellij.scala
  2025.3.41`, `com.explyt.spring 253.34.104` (community Spring tooling, used
  instead of the Ultimate-only HTTP Client), `Docker 253.32098.40`. All pinned
  to build `253.x` — any platform bump requires re-validating these three
  versions.
- **Branding is already partially solved via a workaround, not via a native
  `ApplicationInfo.xml`**: the `prepareBranding` task synthesizes, at runtime, a
  jar (`inferna-branding.jar`) containing a generated `idea/ApplicationInfo.xml`
  (name, vendor, logo, icons, build number) and injects it via
  `-Xbootclasspath/a:` plus the system property
  `idea.platform.load.app.info.from.resources=true` on the `runIde` task. This
  is a **development-sandbox hack**: it only works inside `runIde`
  (Gradle-manipulated classpath/bootclasspath) and does not exist in a real
  standalone distribution, where `ApplicationInfo.xml` needs to be embedded in
  the platform jar before packaging.
- There is also a runtime patch of `coroutines-javaagent.jar` (injecting a stub
  `AgentPremain` class via embedded Base64 bytecode) to work around a version
  mismatch in `kotlinx-coroutines-debug` in the local Nix-provided IDE. This is
  specifically a **Nix/local environment** workaround and must not carry over
  into the standalone build pipeline (which should resolve the correct
  dependency, not patch the jar).
- `zipSigner()` and `pluginVerifier()` are already configured — a sign that
  packaging/signing discipline is already taken seriously, even at the plugin
  stage.
- Not yet present: a "release" build profile, a standalone packaging task, any
  reference to `intellij-community` as a submodule/vendored source, CI
  (`.github/` absent), automated tests (`src/test` absent).

### 0.2 `plugin.xml`

- `id=org.inferna.ide`, depends on `com.intellij.modules.platform`,
  `com.intellij.modules.java`, `org.jetbrains.kotlin`.
- Registers: 1 `applicationListener` (`InfernaLifecycleListener` on
  `AppLifecycleListener`), 1 `themeProvider` (`targetUi="islands"` —
  **coupled to the New UI / Islands**, with no fallback for the classic UI), 1
  `bundledColorScheme`, 1 `codeInsight.lineMarkerProvider` (language `UAST`,
  covering polyglot Java+Kotlin), 1 `toolWindow` anchored to the right.
- No `action`, no `postStartupActivity`/`ProjectActivity`, no custom
  inspection, no application/project service declared — today's "Spring
  tooling" layer is just the LineMarker; inspections, dependency injection, and
  property resolution don't exist yet in the project's own code (they rely on
  the third-party `com.explyt.spring` plugin).

### 0.3 Branding, theme, and icons

- `inferna.theme.json`: a "Visceral" dark theme (`parentTheme:
  ExperimentalDark`, `targetUi: islands`), "industrial biopunk" palette
  (arterial red `#b81424`/`#ff1e38` over near-black `#070203`/`#0a0405`).
  Broad component coverage (Editor, EditorTabs, ToolWindow, Terminal incl. the
  new Block Terminal, MainToolbar, StatusBar, Popup, ScrollBar, ProgressBar,
  Tree/List).
- `inferna.xml`: editor color scheme, `parent_scheme="Darcula"` (inherits
  whatever it doesn't override), covers general syntax plus Kotlin-specific
  overrides (`KOTLIN_LABEL`, `KOTLIN_SMART_CAST_VALUE`, etc.) and full
  Terminal/Block Terminal ANSI colors.
- Icons: a complete set of 16–512px PNGs + `.ico` duplicated across three
  separate trees (`assets/icons/`, `launcher/icons/`,
  `src/main/resources/icons/`) — **today manually duplicated across 3 trees**,
  with no sync task; the risk of drift already exists (e.g. an icon updated in
  one place and forgotten in the others). Dedicated 16x16 SVGs exist
  (`pluginIcon16.svg`) with an explicit build comment explaining why the
  80x80 SVG isn't reused for the 16px slot (it stretches incorrectly). Splash
  art exists in multiple resolutions/formats (`.png`, `@2x.png`, hi-res
  `.jpeg`).
- `InfernaLifecycleListener`: besides applying the theme, it **actively
  unregisters** every other `themeProvider`/`bundledColorScheme` in the IDE at
  runtime (keeping only the New UI's mandatory anchors — "Islands Dark/Light" —
  and "Darcula" for inheritance), and enforces the active theme/scheme in
  `appStarted`. This theme-lockdown is done through the dynamic extension API
  (`dynamic="true"`), not by patching a file — the right approach at the
  plugin stage, but it is exactly the kind of logic that becomes **redundant**
  once Phase 2 removes the competing themes directly from the final artifact
  (there's no point unregistering at runtime what was never packaged in the
  first place).

### 0.4 Kotlin code (functional, but early-stage)

| File | Role | State |
|---|---|---|
| `core/InfernaLifecycleListener.kt` | Applies/locks the theme at boot | Functional; contains documented EDT and restart-dialog workarounds |
| `dashboard/InfernaDashboardToolWindowFactory.kt` | "Inferna Dashboard" tool window | Prototype: shows project name, JVM memory (polled via a `javax.swing.Timer` every 2s), static text. No real project/build/Spring data |
| `icons/InfernaIcons.kt` | Central icon registry | Only 2 icons registered (Dashboard, SpringBean) |
| `spring/SpringBeanLineMarkerProvider.kt` | Gutter icon on `@Autowired` fields pointing to candidate beans | Only in-house "Spring productivity" feature; uses UAST (polyglot), `ClassInheritorsSearch` with caching keyed on `PsiModificationTracker` — a correct and efficient implementation, but covers only field injection via `@Autowired`, not constructor injection, `@Qualifier`, or XML/Java-config beans |

There is no `ProjectActivity`, no inspections (`LocalInspectionTool`), no
property resolution (`@Value`, `application.properties`/`.yml`), and no
quick-fixes.

### 0.5 Audit conclusion

The project is, in fact, at an **advanced plugin-prototype stage for
branding/theming** but **early stage for in-house Spring tooling**, with a
consistent engineering standard (documented workarounds, correct caching,
clear separation of concerns). The biggest structural risk for the standalone
journey is not the Kotlin code — it's the `prepareBranding`/synthesized
`ApplicationInfo.xml`, which **does not survive** the shift from "plugin
running on top of a downloaded IDE" to "compiled platform build with branding
embedded in the jar". That is the critical item Phase 2/3 must resolve before
anything else.

---

## 1. Phase 1 — Core Stabilization & In-House Tooling (Plugin Stage)

**Goal:** exhaust what can be delivered as a plugin before paying the cost of
standalone build infrastructure. Standalone build work on top of incomplete
tooling only multiplies the cost of iteration.

### 1.1 Execution checklist

- [ ] **In-house Spring tooling** (reduce reliance on the third-party
      `com.explyt.spring`, today pinned to a specific build version):
  - [ ] `LineMarkerProvider` for **constructor** injection (not only field
        `@Autowired`), reusing the already-cached `findSpringBeanCandidates`.
  - [ ] `@Qualifier` / `@Primary` support in candidate resolution.
  - [ ] Custom inspection: unresolved bean (`@Autowired` with no unique
        candidate) with a suggested quick-fix.
  - [ ] `@Value("${'$'}{prop}")` navigation → `application.properties`/`.yml`
        (reference contributor).
- [ ] **Palette, fonts, tree icons (16x16), and New UI (`ExpUI`)**:
  - [ ] Unify the 3 icon trees (`assets/`, `launcher/`,
        `src/main/resources/icons/`) into a single source of truth with a
        Gradle sync/generation task, eliminating today's drift risk.
  - [ ] Audit every New UI action/gutter icon against its actual render slot
        (replicating the care already documented for `pluginIcon16.svg`) —
        gutter icons, toolwindow stripe icons, notification icons.
  - [ ] Validate `inferna.theme.json` against `targetUi: islands` **and**
        against classic mode (`targetUi: classic`), or explicitly document that
        Inferna is Islands-only — today there is no fallback, and this is a
        product decision that must be made explicit before Phase 2 locks the
        app's default UI.
  - [ ] Revise the Dashboard
        (`InfernaDashboardToolWindowFactory`) to move past prototype: real
        metrics (build events, compilation errors, Spring context status), not
        just JVM heap.
- [ ] **Workaround hygiene** (non-blocking for Phase 1, but must be resolved
      before Phase 3):
  - [ ] Confirm whether the `coroutines-javaagent.jar` patch is still needed
        against the `kotlinx-coroutines` version that will be pinned in the
        standalone build — it most likely **won't be** (standalone resolves its
        own dependencies via `intellij-community`, not via the local Nix IDE).

### 1.2 Technical risks

| Risk | Mitigation |
|---|---|
| Third-party `com.explyt.spring` and `Docker` plugins, pinned to `253.32098.40`/`253.34.104`, become unavailable/incompatible on a base-platform change | Treat as explicitly versioned external dependencies; re-validate on every `platformVersion` bump |
| In-house Spring tooling is still shallow, risking expectations of Ultimate parity | Keep scope explicit and documented (field + constructor + `@Value`), without over-promising |

### 1.3 Acceptance criteria

- `./gradlew runIde` boots with a single Inferna theme (no competing options in
  the theme picker), consistent icons across the 3 legacy trees (now generated
  from a single source), and the LineMarker covering both field and
  constructor injection.
- No third-party dependency without a pinned, documented version.

---

## 2. Phase 2 — Application Identity & Native Branding

**Goal:** replace the `prepareBranding` workaround (a jar injected via
bootclasspath only inside `runIde`) with a real `ApplicationInfo.xml`, compiled
into the distribution, the same way Android Studio has its own
(`android/tools/idea/community/.../AndroidStudioApplicationInfo.xml`).

### 2.1 Execution checklist

- [ ] Create `resources/idea/InfernaApplicationInfo.xml`, version-controlled in
      the repo (not generated at runtime), with the fields currently
      synthesized by `prepareBranding`: `<names product="Inferna"
      fullname="Inferna IDE" script="inferna"/>`, `<version>`, `<company>`,
      `<build>`, `<logo>`, `<icon>`/`<icon-eap>`, `<essential-plugin>`.
  - [ ] Parameterize `version`/`build`/`majorReleaseDate` via a build template
        (Gradle `processResources` with `filesMatching` + `expand`, replacing
        today's `providers.gradleProperty(...).get()` calls inside the Kotlin
        heredoc string).
  - [ ] Preserve the observation already documented in the current build:
        dedicated 16px icons (`icon-ce_16.svg`) for slots rendered at 16x16,
        never the large SVG.
- [ ] **Splash screen and "About" box**: migrate `artwork/inferna_splash*.png`/`.jpeg`,
      currently copied manually into the branding jar, into the platform's
      standard resources pipeline (same path/name the native
      `ApplicationInfo.xml` expects, without the bootclasspath trick).
- [ ] **Update channels**: `<update-urls>` in `InfernaApplicationInfo.xml`
      pointing to a self-hosted endpoint (see Phase 4 — update channel XML
      independent of JetBrains). In this phase, only declare the schema; the
      actual infra serving the XML belongs to Phase 4.
- [ ] **System properties / config directories**:
  - [ ] `idea.platform.prefix=Inferna` as a native launcher system property
        (Phase 3), no longer passed via `runIde`'s `jvmArgs`.
  - [ ] Confirm the resulting path on each OS: `~/.config/InfernaIDE` (Linux),
        `~/Library/Application Support/InfernaIDE` (macOS),
        `%APPDATA%\InfernaIDE` (Windows) — validate that the chosen prefix
        doesn't collide with an existing IntelliJ install on the same machine
        (an explicit requirement, since today's dev `runIde` uses its own
        sandbox under `build/idea-sandbox` and masks the problem).
- [ ] Remove/retire the `prepareBranding` task and all of the
      `-Xbootclasspath/a:` / `idea.platform.load.app.info.from.resources=true`
      logic once the native `ApplicationInfo.xml` is in place — keep both in
      parallel only during the transition, never as a permanent solution.
- [ ] Re-evaluate `InfernaLifecycleListener.restrictToInfernaOnly()`: in the
      standalone build, the other `themeProvider`/`bundledColorScheme`
      extensions simply won't be packaged (Phase 3, plugin/resource strip), so
      this runtime unregistration can be **removed** — it's a safety net that
      only makes sense while Inferna runs as a plugin on top of a full
      third-party distribution.

### 2.2 Technical risks

| Risk | Mitigation |
|---|---|
| The native `ApplicationInfo.xml` has a stricter schema than today's synthesized XML string (which has never gone through `plugin-verifier` validation at this point) | Validate against the platform's internal XSD/schema before removing the workaround |
| Config-directory collision with an existing IntelliJ install | Manual test on a clean environment per OS before clearing Phase 3 |
| "About"/third-party licenses screen (Scala, Explyt Spring, Docker plugin) needs to reflect the native licenses screen | Gather and embed license texts for the 3 marketplace plugins currently used |

### 2.3 Acceptance criteria

- The build produces a version-controlled, PR-reviewable `ApplicationInfo.xml`
  (no longer a Kotlin heredoc string), readable by the platform without a
  bootclasspath hack.
- `idea.platform.prefix=Inferna` resolves to an isolated config directory,
  validated on the 3 target OSes.

---

## 3. Phase 3 — Standalone Build Engineering (Android Studio Model)

**Goal:** move from "plugin + locally downloaded/Nix-provided community IDE"
to a build that produces a complete, independent JVM distribution, using the
same framework JetBrains/Google use (`org.jetbrains.intellij.build` on top of
an `intellij-community` checkout).

### 3.1 Execution checklist

- [ ] **Decide the platform-source architecture** (see open questions below):
      full `intellij-community` checkout as a submodule/vendored source vs.
      consuming published platform artifacts (`intellijIdeaCommunity(...)`,
      already used today) and applying `org.jetbrains.intellij.build` only on
      top of them.
  - Since the project **today already depends on published artifacts** (no
    `intellij-community` checkout exists), the path of least friction is to
    start with the "artifacts + build framework" model, moving to a full
    checkout only if/when source-level removal of bundled plugins (not just
    their jars) becomes necessary for licensing or size reasons.
- [ ] **Dedicated build module** (`build-tools/` or similar, separate from the
      current plugin module) that:
  - [ ] Declares the allow-list of bundled plugins (an explicit strip of
        unused IntelliJ Community plugins — today the project is already
        selective via `bundledPlugin(...)` for Java/Kotlin only, but the
        actual strip must happen at the full platform-distribution level, not
        just in the plugin's Gradle dependencies).
  - [ ] Assembles the final directories: `bin/`, `lib/`, `plugins/` (with
        Inferna, Scala, Explyt Spring, and Docker already included as bundled
        plugins — no longer downloaded from the marketplace at runtime),
        `jbr/` (embedded Java runtime).
  - [ ] Generates the executable binaries: a shell script `inferna`
        (Linux/macOS, based on the platform's standard `idea.sh`, rebranded)
        and `inferna.exe` (Windows, via `intellij-community`'s `WinLauncher`,
        recompiled with the resources/icons from `launcher/icons/`).
  - [ ] A `buildStandaloneDistribution` Gradle task that depends on Phase 2's
        `prepareBranding` successor and packages everything above.
- [ ] **Removal of unnecessary plugins**: start with the list of IntelliJ
      Community bundled plugins that today ship "for free" in the base
      distribution but aren't used by Inferna's JVM/Spring profile (e.g.
      support for non-JVM languages the Community edition bundles by default).
      Measure before/after: cold-start time and memory footprint of the
      process (`runIde` already partially exposes this via
      `InfernaDashboardToolWindowFactory`, which can be reused as an internal
      measurement instrument during this phase).
- [ ] Port the `coroutines-javaagent.jar` patch (today gated on
      `localIdeDir != null`, i.e. only runs in dev via Nix) — it should **not**
      enter the standalone build pipeline; if the standalone build correctly
      resolves the `kotlinx-coroutines-core` version via
      `intellijDependencies()`, the entire patch is removed in this phase, not
      merely disabled by condition.

### 3.2 Technical risks

| Risk | Mitigation |
|---|---|
| `org.jetbrains.intellij.build` is the internal framework JetBrains itself uses to assemble official distributions; public documentation is scarce | Study Android Studio's build script (the public AOSP fork of `intellij-community`) as a direct reference, already cited as this roadmap's own model |
| The native `WinLauncher` requires a C++/MSVC toolchain to recompile with new icons/name | Evaluate whether re-signing/renaming the default `.exe` with swapped resources (icon, manifest) is enough before recompiling from scratch |
| "Plugin strip" footprint reduction could break functionality used implicitly (e.g. the images plugin used by Inferna's own SVG theme icons) | Manual smoke-test suite (open a real Spring Boot project, run the debugger, open a terminal, apply the theme) before each bundled-plugin removal |
| The current dev environment depends on Nix (`shell.nix`) only for native JBR libs on NixOS; the standalone build must run on standard CI (Ubuntu/macOS/Windows runners) without Nix | The standalone build must have no Nix dependency; `shell.nix` remains a dev convenience tool, not part of the release pipeline (Phase 4) |

### 3.3 Acceptance criteria

- `./gradlew buildStandaloneDistribution` (or an equivalent task) produces,
  without `runIde`, a `bin/`+`lib/`+`plugins/` folder directly executable via
  `bin/inferna` on a clean machine (no Nix, no pre-installed IntelliJ IDE).
- Cold-start time and memory usage measured and recorded as a baseline for
  comparison against future platform bumps.

---

## 4. Phase 4 — Packaging, Distribution, and CI/CD

**Goal:** a repeatable pipeline that publishes signed final artifacts for the
3 target OSes and maintains a self-hosted update channel, with license
compliance.

### 4.1 Execution checklist

- [ ] **CI** (today nonexistent — no `.github/workflows`): a pipeline that runs
      on every push/PR: `./gradlew build test verifyPlugin` (using the already
      configured `pluginVerifier()`) plus a headless smoke test of `runIde`
      (`./gradlew runIdeForUiTests` or equivalent), before any release task.
- [ ] **Release artifacts**:
  - [ ] Linux: `.tar.gz` with an executable `bin/inferna` (+ a `.desktop` file
        referencing the icons already present in `launcher/icons/`).
  - [ ] Windows: a signed `inferna.exe` (reusing the already configured
        `zipSigner()` as a baseline of signing discipline, extended to the
        installer) + a `.zip` package as a portable alternative.
  - [ ] macOS: a `.dmg` with an `.app` bundle, `.icns` icon generated from the
        existing `inferna_icon_512x512.png`.
- [ ] **Independent update channel**: serve a self-hosted `updates.xml` (schema
      compatible with the `<update-urls>` declared in Phase 2), hosted outside
      JetBrains infrastructure — hosting choice is a product decision, not an
      architectural one (can be GitHub Releases + a static file, or a
      dedicated server).
- [ ] **Legal compliance**:
  - [ ] Inferna's own license: **Apache 2.0** (compatible with
        `intellij-community`, already cited in the stated goal) — add a
        `LICENSE` file at the repo root (absent today).
  - [ ] JetBrains trademark-removal checklist: name, logo, "IntelliJ"/"IDEA"
        strings in UI, splash, About box, installers — specifically audit
        Phase 2's `ApplicationInfo.xml` and any residual string coming from
        the third-party bundled plugins.
  - [ ] License attribution for the bundled third-party plugins (Scala, Explyt
        Spring, Docker) in the licenses/about screen, as already surfaced in
        Phase 2.

### 4.2 Technical risks

| Risk | Mitigation |
|---|---|
| macOS code signing (Apple notarization) and Windows signing (Authenticode) have their own cost/process not covered by the plugin's `zipSigner()` (which signs the *plugin* `.zip`, not the final *application* installer) | Budget per-OS signing certificates as a separate infrastructure line item before promising public distribution |
| A self-hosted update channel without the JetBrains `pluginVerifier`/delta-update infrastructure may require downloading the full package on every version | Acceptable for v1; only revisit binary patch-updates if package size becomes a real problem |

### 4.3 Acceptance criteria

- A release tag triggers CI → build → packaging → publication of 3 artifacts
  (`.tar.gz`, `.exe`/`.zip`, `.dmg`) with no manual step.
- Apache 2.0 `LICENSE` present at the repo root; no "JetBrains"/"IntelliJ"
  string visible in the final UI outside of third-party license attributions.

---

## 5. Expected build commands (quick reference)

```bash
# Phase 1 (already functional today)
./gradlew runIde
./gradlew verifyPlugin
./gradlew test

# Phase 2 (to be implemented)
./gradlew processResources   # should embed the real InfernaApplicationInfo.xml

# Phase 3 (to be implemented)
./gradlew buildStandaloneDistribution

# Phase 4 (to be implemented)
./gradlew packageLinuxTarGz
./gradlew packageWindowsExe
./gradlew packageMacDmg
./gradlew publishUpdateChannel
```

---

## 6. Open questions (pending product/architecture decisions)

1. **Full `intellij-community` checkout vs. published artifacts +
   `org.jetbrains.intellij.build`**: the project does not currently vendor the
   platform. Moving to a full checkout is a large complexity jump (multi-GB
   repository, JetBrains' own build system) and should only be done if Phase 3
   proves published artifacts don't give enough control over plugin/native
   binary stripping.
2. **Classic (non-Islands) UI support**: `inferna.theme.json` today only
   declares `targetUi="islands"`. Confirm whether Inferna will be
   Islands-only before Phase 2 locks this in as permanent product identity.
3. **Update channel hosting scope**: GitHub Releases is sufficient for v1, but
   not yet decided — it impacts the exact schema of Phase 2's
   `<update-urls>`.

---

*Last updated: 2026-10-15, based on the audit of commit `a56da3e`
(branch `master`).*
