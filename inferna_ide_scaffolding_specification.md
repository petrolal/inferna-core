# Inferna IDE - Project Scaffolding Guide

This document contains the complete file tree and code for the **Inferna IDE** project, built atop the open-source **IntelliJ Platform SDK (Community Edition)** with Gradle Kotlin DSL, Java 21, and Kotlin 2.0.

---

## 1. Directory Structure

```text
inferna-ide/
├── settings.gradle.kts
├── gradle.properties
├── build.gradle.kts
└── src/
    └── main/
        ├── kotlin/
        │   └── org/inferna/
        │       ├── core/
        │       │   └── InfernaLifecycleListener.kt
        │       ├── spring/
        │       │   └── SpringBeanLineMarkerProvider.kt
        │       └── dashboard/
        │           └── InfernaDashboardToolWindowFactory.kt
        └── resources/
            ├── META-INF/
            │   └── plugin.xml
            └── theme/
                └── inferna.theme.json
```

---

## 2. Root Build Configurations

### `settings.gradle.kts`
```kotlin
rootProject.name = "inferna-ide"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

### `gradle.properties`
```properties
# Kotlin & JVM
kotlin.version=2.0.20
java.version=21

# IntelliJ Platform SDK
platformVersion=2024.2.3
platformPluginVersion=2.0.1

# JVM Memory and Optimization for Platform SDK Builds
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
```

### `build.gradle.kts`
```kotlin
plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "org.inferna"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        pluginVerifier()
        zipSigner()
    }

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id.set("org.inferna.ide")
        name.set("Inferna IDE")
        version.set(project.version.toString())
        description.set("Biopunk JVM standalone environment powered by the IntelliJ Platform SDK.")
        vendor {
            name.set("Inferna Project")
            url.set("https://github.com/inferna")
        }
        ideaVersion {
            sinceBuild.set("242")
            untilBuild.set("243.*")
        }
    }

    publishing {
        token.set(providers.environmentVariable("INTELLIJ_PUBLISH_TOKEN"))
    }
}

tasks {
    runIde {
        jvmArgs("-Xmx4g", "-XX:+UseG1GC", "-Dawt.useSystemAAFontSettings=on")
    }
}
```

---

## 3. Metadata & Theme Definitions

### `src/main/resources/META-INF/plugin.xml`
```xml
<idea-plugin>
    <id>org.inferna.ide</id>
    <name>Inferna IDE</name>
    <vendor>Inferna Project</vendor>

    <description><![CDATA[
      Custom biopunk developer platform and JVM tooling with native Kotlin and Spring support.
    ]]></description>

    <!-- Essential platform dependencies -->
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.java</depends>
    <depends>org.jetbrains.kotlin</depends>

    <applicationListeners>
        <listener class="org.inferna.core.InfernaLifecycleListener"
                  topic="com.intellij.ide.AppLifecycleListener"/>
    </applicationListeners>

    <extensions defaultExtensionNs="com.intellij">
        <!-- Biopunk Theme Provider -->
        <themeProvider id="org.inferna.theme.visceral" path="/theme/inferna.theme.json"/>

        <!-- Universal Spring Bean LineMarker (Polyglot: Java & Kotlin via UAST) -->
        <codeInsight.lineMarkerProvider
            language="UAST"
            implementationClass="org.inferna.spring.SpringBeanLineMarkerProvider"/>

        <!-- Right Docked Dashboard ToolWindow -->
        <toolWindow id="Inferna Dashboard"
                    anchor="right"
                    canCloseContent="true"
                    factoryClass="org.inferna.dashboard.InfernaDashboardToolWindowFactory"/>
    </extensions>
</idea-plugin>
```

### `src/main/resources/theme/inferna.theme.json`
```json
{
  "name": "Inferna Visceral",
  "author": "Inferna Project",
  "dark": true,
  "editorScheme": "/theme/inferna.theme.json",
  "colors": {
    "inferna.bg.dark": "#0A0405",
    "inferna.bg.panel": "#140709",
    "inferna.bg.active": "#1F0B0E",
    "inferna.accent.arterial": "#B81424",
    "inferna.accent.crimson": "#E6192E",
    "inferna.fg.light": "#E3D1D1",
    "inferna.fg.muted": "#7A6363"
  },
  "ui": {
    "*": {
      "background": "#0A0405",
      "foreground": "#E3D1D1",
      "selectionBackground": "#1F0B0E",
      "selectionForeground": "#FFFFFF",
      "selectionInactiveBackground": "#140709",
      "borderColor": "#2D0E12",
      "separatorColor": "#2D0E12"
    },
    "Editor": {
      "background": "#0A0405",
      "foreground": "#E3D1D1",
      "shortcutForeground": "#E6192E"
    },
    "EditorTabs": {
      "background": "#0A0405",
      "underHoverBackground": "#140709",
      "underHoverForeground": "#E3D1D1",
      "activeBackground": "#1F0B0E",
      "activeForeground": "#FFFFFF",
      "underlineColor": "#B81424",
      "underlineHeight": 2
    },
    "ToolWindow": {
      "Header": {
        "background": "#140709",
        "inactiveBackground": "#0A0405"
      },
      "Button": {
        "selectedBackground": "#1F0B0E",
        "selectedForeground": "#FFFFFF"
      }
    },
    "Tree": {
      "background": "#0A0405",
      "selectionBackground": "#1F0B0E",
      "selectionForeground": "#FFFFFF"
    },
    "StatusBar": {
      "background": "#0D0507",
      "foreground": "#7A6363",
      "borderColor": "#2D0E12"
    }
  }
}
```

---

## 4. Kotlin Source Implementations

### `src/main/kotlin/org/inferna/core/InfernaLifecycleListener.kt`
```kotlin
package org.inferna.core

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger

class InfernaLifecycleListener : AppLifecycleListener {
    private val logger = Logger.getInstance(InfernaLifecycleListener::class.java)

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        logger.info("=========================================")
        logger.info(" Inferna IDE Engine Initializing...     ")
        logger.info(" Runtime: JVM 21 | Target: Biopunk OS   ")
        logger.info("=========================================")
    }

    override fun appStarted() {
        logger.info("Inferna Platform active. All subsystems dispatched.")
    }
}
```

### `src/main/kotlin/org/inferna/spring/SpringBeanLineMarkerProvider.kt`
```kotlin
package org.inferna.spring

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElement

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    private val autowiredAnnotation = "org.springframework.beans.factory.annotation.Autowired"
    private val springAnnotations = setOf(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Repository",
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController"
    )

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uField = element.toUElement() as? UField ?: return

        // Intercept injection points
        if (!uField.hasAnnotation(autowiredAnnotation)) return

        val fieldType = uField.type
        val targetClass = (fieldType as? PsiClassType)?.resolve() ?: return
        val project = element.project

        val candidates = mutableListOf<PsiClass>()

        if (targetClass.isInterface) {
            AllClassesSearch.search(GlobalSearchScope.projectScope(project), project).forEach { psiClass ->
                if (psiClass.isInheritor(targetClass, true) && hasSpringStereotype(psiClass)) {
                    candidates.add(psiClass)
                }
            }
        } else if (hasSpringStereotype(targetClass)) {
            candidates.add(targetClass)
        }

        if (candidates.isNotEmpty()) {
            val anchor = uField.sourcePsi?.firstChild ?: element
            val lineMarker = NavigationGutterIconBuilder.create(AllIcons.Nodes.Services)
                .setTargets(candidates)
                .setTooltipText("Inferna: Navigate to Spring Bean Declaration")
                .createLineMarkerInfo(anchor)

            result.add(lineMarker)
        }
    }

    private fun hasSpringStereotype(psiClass: PsiClass): Boolean {
        return springAnnotations.any { psiClass.hasAnnotation(it) }
    }
}
```

### `src/main/kotlin/org/inferna/dashboard/InfernaDashboardToolWindowFactory.kt`
```kotlin
package org.inferna.dashboard

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.SwingConstants

class InfernaDashboardToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            background = Color(0x0A, 0x04, 0x05)
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        }

        val headerLabel = JBLabel("INFERNA BIOMETRICS", SwingConstants.CENTER).apply {
            foreground = Color(0xE6, 0x19, 0x2E)
            font = font.deriveFont(Font.BOLD, 14f)
        }

        val runtime = Runtime.getRuntime()
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        val statsPanel = JBPanel<JBPanel<*>>(GridLayout(4, 1, 8, 8)).apply {
            background = Color(0x14, 0x07, 0x09)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(0x2D, 0x0E, 0x12)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )

            add(JBLabel("Project: ${project.name}").apply { foreground = Color(0xE3, 0xD1, 0xD1) })
            add(JBLabel("Subsystem: Active").apply { foreground = Color(0xB8, 0x14, 0x24) })
            add(JBLabel("JVM Memory: ${usedMemoryMb}MB / ${totalMemoryMb}MB").apply { foreground = Color(0x7A, 0x63, 0x63) })
            add(JBLabel("Polyglot Engine: Java / Kotlin UAST").apply { foreground = Color(0x7A, 0x63, 0x63) })
        }

        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(statsPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "Overview", false)
        toolWindow.contentManager.addContent(content)
    }
}
```

---

## 5. Bootstrapping Instructions

1. **Bootstrap the Gradle Wrapper:**
   If you have Gradle installed locally:
   ```bash
   gradle wrapper --gradle-version 8.10.2 --distribution-type bin
   ```

2. **Launch the Isolated Development Sandbox:**
   ```bash
   ./gradlew runIde
   ```
   Gradle will download the IntelliJ Community Platform runtime, inject your theme, listener, and UAST line markers, and launch a completely isolated window (`build/idea-sandbox`) without altering your local configs.