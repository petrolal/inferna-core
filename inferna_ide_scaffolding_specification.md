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
                ├── inferna.theme.json
                └── inferna.xml
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
kotlin.version=2.2.20
java.version=21

# IntelliJ Platform SDK
platformVersion=2025.1
platformPluginVersion=2.2.1

# JVM Memory and Optimization for Platform SDK Builds
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
```

### `build.gradle.kts`
```kotlin
import java.io.File
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    id("java")
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "org.inferna"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

val localIdeDir = providers.gradleProperty("localPlatformPath").orNull?.let { file(it) }
    ?: file("/run/current-system/sw/bin/intellij-idea-oss").takeIf { it.exists() }?.let { binary ->
        binary.toPath().toRealPath().parent?.parent?.toFile()?.takeIf { file ->
            file.resolve("product-info.json").exists()
        }
    }

dependencies {
    intellijPlatform {
        if (localIdeDir != null) {
            local(localIdeDir)
        } else {
            intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        }
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
            sinceBuild.set("251")
            untilBuild.set("253.*")
        }
    }

    publishing {
        token.set(providers.environmentVariable("INTELLIJ_PUBLISH_TOKEN"))
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    named("initializeIntellijPlatformPlugin") {
        doLast {
            if (localIdeDir != null) {
                val agentJar = layout.buildDirectory.file("coroutines-javaagent.jar").get().asFile
                if (agentJar.exists()) {
                    val classEntry = "kotlinx/coroutines/debug/AgentPremain.class"
                    val b64 = "yv66vgAAAEEADwoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAla290bGlueC9jb3JvdXRpbmVzL2RlYnVnL0FnZW50UHJlbWFpbgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAAdwcmVtYWluAQA7KExqYXZhL2xhbmcvU3RyaW5nO0xqYXZhL2xhbmcvaW5zdHJ1bWVudC9JbnN0cnVtZW50YXRpb247KVYBAApTb3VyY2VGaWxlAQARQWdlbnRQcmVtYWluLmphdmEAIQAHAAIAAAAAAAIAAQAFAAYAAQAJAAAAHQABAAEAAAAFKrcAAbEAAAABAAoAAAAGAAEAAAADAAkACwAMAAEACQAAABkAAAACAAAAAbEAAAABAAoAAAAGAAEAAAAEAAEADQAAAAIADg=="
                    val classBytes = Base64.getDecoder().decode(b64)
                    val tempJar = File(agentJar.parentFile, "coroutines-javaagent-temp.jar")
                    ZipInputStream(agentJar.inputStream()).use { zin ->
                        ZipOutputStream(tempJar.outputStream()).use { zout ->
                            var entry = zin.nextEntry
                            var hasClass = false
                            while (entry != null) {
                                zout.putNextEntry(ZipEntry(entry.name))
                                zin.copyTo(zout)
                                if (entry.name == classEntry) hasClass = true
                                entry = zin.nextEntry
                            }
                            if (!hasClass) {
                                zout.putNextEntry(ZipEntry(classEntry))
                                zout.write(classBytes)
                            }
                        }
                    }
                    tempJar.copyTo(agentJar, overwrite = true)
                    tempJar.delete()
                }
            }
        }
    }

    runIde {
        jvmArgs(
            "-Xmx4g",
            "-XX:+UseG1GC",
            "-Dawt.useSystemAAFontSettings=on",
            "-Dgradle.compatibility.update.interval=0",
            "-Dgradle.compatibility.config.url=",
            "-Didea.fatal.error.notification=disabled",
            "-Didea.is.internal=false"
        )
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
        <!-- Biopunk Theme Provider & Editor Color Scheme -->
        <themeProvider id="org.inferna.theme.visceral" path="/theme/inferna.theme.json" targetUi="islands"/>
        <bundledColorScheme id="Inferna Visceral Dark" path="/theme/inferna.xml"/>

        <!-- Universal Spring Bean LineMarker (Polyglot: Java & Kotlin via UAST) -->
        <codeInsight.lineMarkerProvider
            language="UAST"
            implementationClass="org.inferna.spring.SpringBeanLineMarkerProvider"/>

        <!-- Right Docked Dashboard ToolWindow -->
        <toolWindow id="Inferna Dashboard"
                    anchor="right"
                    factoryClass="org.inferna.dashboard.InfernaDashboardToolWindowFactory"/>
    </extensions>

    <extensions defaultExtensionNs="org.jetbrains.kotlin">
        <supportsKotlinPluginMode supportsK1="true" supportsK2="true"/>
    </extensions>
</idea-plugin>
```

### `src/main/resources/theme/inferna.theme.json`
```json
{
  "name": "Inferna Visceral Dark",
  "dark": true,
  "author": "Inferna Project",
  "editorScheme": "/theme/inferna.xml",
  "parentTheme": "ExperimentalDark",
  "colors": {
    "CanvasVoid": "#070203",
    "IslandPanel": "#100507",
    "IslandEditor": "#0a0405",
    "IslandBorder": "#1c0a0e",
    "ArterialRed": "#b81424",
    "ArterialBright": "#ff1e38",
    "VisceralSelection": "#3d141a",
    "NecroticText": "#e6d5d5",
    "NecroticMuted": "#8f6f73",
    "BorderDark": "#240d12",
    "BorderLight": "#2d1116",
    "BorderTransparent": "#00000000"
  },
  "ui": {
    "*": {
      "background": "#100507",
      "foreground": "#e6d5d5",
      "infoForeground": "#8f6f73",
      "selectionBackground": "#3d141a",
      "selectionForeground": "#ffffff",
      "selectionInactiveBackground": "#240d12",
      "disabledBackground": "#100507",
      "disabledForeground": "#422127",
      "acceleratorForeground": "#8f6f73",
      "acceleratorSelectionForeground": "#ffffff",
      "errorForeground": "#ff334b",
      "borderColor": "#240d12",
      "separatorColor": "#240d12"
    },

    "Islands": 1,
    "Island": {
      "arc": 20,
      "arc.compact": 16,
      "borderArcLength": 14,
      "borderArcLength.compact": 10,
      "borderWidth": 6,
      "borderWidth.compact": 4,
      "borderColor": "#100507",
      "inactiveAlpha": 0.44,
      "inactiveAlphaInStatusBar": {
        "os.mac": 0.2,
        "os.windows": 0.2,
        "os.linux": 0.15
      }
    },

    "MainWindow.background": "#070203",
    "Borders.color": "#240d12",
    "Borders.ContrastBorderColor": "#240d12",

    "Component": { "arc": 8 },
    "Button": { "arc": 8 },
    "TextField": { "arc": 8 },
    "Popup": { "arc": 8 },
    "ProgressBar": { "arc": 8 }
  }
}
```

---

## 4. Kotlin Source Implementations

### `src/main/kotlin/org/inferna/core/InfernaLifecycleListener.kt`
```kotlin
package org.inferna.core

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.ui.LafManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager

private const val INFERNA_THEME_NAME = "Inferna Visceral Dark"
private const val DEFAULT_THEME_APPLIED_KEY = "org.inferna.theme.defaultApplied"
private const val DEFAULT_SCHEME_APPLIED_KEY = "org.inferna.theme.editorSchemeApplied.v2"

class InfernaLifecycleListener : AppLifecycleListener {
    private val logger = Logger.getInstance(InfernaLifecycleListener::class.java)

    init {
        // Suppress remote Gradle JVM compatibility updates to prevent JavaVersion.parse crashes
        // caused by newer Java versions (e.g. Java 25+) listed in JetBrains' online compatibility matrix.
        System.setProperty("gradle.compatibility.update.interval", "0")
        System.setProperty("gradle.compatibility.config.url", "")
    }

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        System.setProperty("gradle.compatibility.update.interval", "0")
        System.setProperty("gradle.compatibility.config.url", "")
        logger.info("=========================================")
        logger.info(" Inferna IDE Engine Initializing...     ")
        logger.info(" Runtime: JVM 21 | Target: Biopunk OS   ")
        logger.info("=========================================")

        try {
            AdvancedSettings.setBoolean("ide.ui.theme.custom.islands", true)
        } catch (e: Throwable) {
            logger.debug("AdvancedSettings ide.ui.theme.custom.islands not available: ${e.message}")
        }

        runOnEdt {
            applyDefaultThemeOnFirstRun()
        }
    }

    override fun appStarted() {
        logger.info("Inferna Platform active. All subsystems dispatched.")
        runOnEdt {
            applyDefaultThemeOnFirstRun()

            val lafManager = LafManager.getInstance()
            if (lafManager.currentUIThemeLookAndFeel?.name == INFERNA_THEME_NAME) {
                lafManager.updateUI()
            }
        }
    }

    private fun runOnEdt(action: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDispatchThread) {
            app.invokeLater(action, ModalityState.any())
        } else {
            action()
        }
    }

    // Only forces the theme & color scheme on the very first launch of a fresh sandbox/install,
    // so a user who later picks a different theme/scheme is never overridden again.
    private fun applyDefaultThemeOnFirstRun() {
        val properties = PropertiesComponent.getInstance()

        if (!properties.getBoolean(DEFAULT_THEME_APPLIED_KEY, false)) {
            val lafManager = LafManager.getInstance()
            val infernaLaf = lafManager.installedThemes.firstOrNull { it.name == INFERNA_THEME_NAME }
            if (infernaLaf != null) {
                lafManager.setCurrentLookAndFeel(infernaLaf, true)
                lafManager.updateUI()
                properties.setValue(DEFAULT_THEME_APPLIED_KEY, true)
                logger.info("Inferna Visceral applied as the default theme.")
            } else {
                logger.warn("Inferna Visceral theme not found among installed themes; default not applied.")
            }
        }

        val editorColorsManager = EditorColorsManager.getInstance()
        val infernaScheme = editorColorsManager.getScheme(INFERNA_THEME_NAME)
            ?: editorColorsManager.allSchemes.firstOrNull { it.name == INFERNA_THEME_NAME }
        if (infernaScheme != null) {
            val schemeNeedsApply = !properties.getBoolean(DEFAULT_SCHEME_APPLIED_KEY, false) ||
                editorColorsManager.globalScheme.name == INFERNA_THEME_NAME
            if (schemeNeedsApply) {
                editorColorsManager.setGlobalScheme(infernaScheme)
                properties.setValue(DEFAULT_SCHEME_APPLIED_KEY, true)
                logger.info("Inferna Visceral Dark applied as the default editor color scheme.")
            }
        } else {
            logger.warn("Inferna Visceral Dark editor color scheme not found among installed schemes.")
        }
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