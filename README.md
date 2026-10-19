<!--
  ~ Inferna Visceral — Biopunk Theme, Splash Branding & Biometrics for IntelliJ IDEA
  ~ Copyright (C) 2026 Lucas Petrola <petrolalucas@gmail.com>
  ~ SPDX-License-Identifier: GPL-3.0-or-later
  -->
# Inferna Visceral — Theme, Splash Branding & Biometrics for IntelliJ IDEA

<p align="center">
  <img src="assets/icons/inferna.png" alt="Inferna Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>Biopunk Theme, Automatic Splash Branding & JVM/Spring Biometrics Plugin</strong><br />
  <em>Built for IntelliJ IDEA Ultimate & Community Edition (2025.1 – 2026.x+)</em>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPLv3" /></a>
  <img src="https://img.shields.io/badge/Java-21%2B-orange.svg" alt="Java 21+" />
  <img src="https://img.shields.io/badge/Kotlin-2.2.20-purple.svg" alt="Kotlin 2.2" />
  <img src="https://img.shields.io/badge/IntelliJ%20IDEA-Ultimate%20%26%20Community-red.svg" alt="IntelliJ IDEA Ultimate & Community" />
  <img src="https://img.shields.io/badge/Version-1.0.0-crimson.svg" alt="Version 1.0.0" />
</p>

---

## 🌟 Overview

**Inferna Visceral** is a Marketplace-ready plugin for **IntelliJ IDEA Ultimate** (and Community Edition) that brings an industrial biopunk design language, automatic splash screen & window icon branding, and a real-time JVM & Spring Biometrics Dashboard to your IDE—without sacrificing any of Ultimate's native capabilities.

---

## ⚡ Key Features

- **Biopunk Design Language & "Inferna Visceral Dark" Theme**
  - Custom UI palette designed for deep focus and low eye-strain, featuring obsidian backgrounds (`#070203` / `#0a0405`), visceral crimson accents (`#b81424` / `#ff1e38`), and sharp syntax highlights across Java, Kotlin, and the Block Terminal.
  - Tailored for IntelliJ's modern **Islands UI**.
  - Non-destructive theme management: automatically activates on first install while keeping all built-in IntelliJ themes available in **Settings → Appearance & Behavior → Appearance**.

- **Automatic, Reversible Splash Screen & Window Icon Branding**
  - [`InfernaLifecycleListener`](src/main/kotlin/org/inferna/core/InfernaLifecycleListener.kt) automatically synchronizes IntelliJ's splash screen cache (`<systemPath>/splash/*.ij`) and window icons with the Inferna artwork whenever **Inferna Visceral Dark** is active.
  - Switching to any other theme in **Settings → Appearance** cleanly restores the original IntelliJ IDEA Ultimate splash screen from backup.

- **Inferna Biometrics Dashboard**
  - Dedicated right-docked tool window ([`InfernaDashboardToolWindowFactory`](src/main/kotlin/org/inferna/dashboard/InfernaDashboardToolWindowFactory.kt)) providing live telemetry:
    - Real-time JVM heap memory tracking
    - Build & compilation status
    - Non-blocking background count of project-wide Spring beans (`@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration`, `@SpringBootApplication`, and `@Bean` methods)

- **Smart Spring Tooling Integration**
  - Automatically detects **IntelliJ IDEA Ultimate's** native `com.intellij.spring` plugin and defers editor gutter icons, `@Value` references, and inspections to Ultimate's native engine—preventing duplicate gutter markers or false-positive warnings.
  - When used in environments without Ultimate's Spring plugin, provides polyglot UAST `@Autowired` / constructor bean gutter navigation, `@Value("${...}")` `.properties` resolution, and `@Qualifier` disambiguation quick-fixes.

---

## 🚀 Installation in IntelliJ IDEA Ultimate

### From Disk (Pre-built Release Zip)

1. Build the plugin distribution:
   ```bash
   ./gradlew buildPlugin
   ```
2. Open **IntelliJ IDEA Ultimate** → **Settings** (`Ctrl+Alt+S`) → **Plugins**.
3. Click the gear icon (**⚙️**) → **Install Plugin from Disk...**
4. Select `build/distributions/inferna-ide-1.0.0.zip` and restart/apply if prompted.

### Publishing to JetBrains Marketplace

```bash
INTELLIJ_PUBLISH_TOKEN="<your-marketplace-token>" ./gradlew publishPlugin
```

---

## 🛠️ Development & Verification

### Prerequisites
- **JDK 21** or later
- On **NixOS**, use `nix-shell` to provide X11/Wayland/font libraries for the sandboxed runtime if running `runIde`.

### Build Commands

```bash
# Package the Marketplace-ready plugin zip (build/distributions/inferna-ide-1.0.0.zip)
./gradlew buildPlugin

# Run automated tests
./gradlew test

# Launch a development sandbox IDE
./gradlew runIde
```

---

## 📄 License & Copyright

Copyright (C) 2026 **Lucas Petrola** `<petrolalucas@gmail.com>`

This project is licensed under the **GNU General Public License v3.0 or later** (`GPL-3.0-or-later`).  
See the [`LICENSE`](LICENSE) file for the full license text.


