/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araujo Petrola <petrolalucas@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import java.io.File
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    id("java")
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "org.inferna"
version = "1.0.1-beta.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

apply(from = "gradle/scripts/local-ide.gradle.kts")

@Suppress("UNCHECKED_CAST")
val localIdeDir = project.extra["localIdeDir"] as File?

dependencies {
    intellijPlatform {
        if (localIdeDir != null) {
            local(localIdeDir)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }

        // Bundled platform plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.properties")

        // Optional language-engine modules: each is an IntelliJ *content module* (see plugin.xml's
        // <content><module name="..."/></content>), packaged as its own lib/modules/*.jar and
        // activated by the platform only when its own optional <dependencies><plugin id="..."/></
        // is satisfied. Each compiles against its own Marketplace/bundled plugin in isolation, so
        // neither leaks onto this project's classpath — a missing Scala/Groovy plugin on the
        // user's IDE can never throw ClassNotFoundException/NoClassDefFoundError from this module.
        pluginModule(implementation(project(":modules:scala")))
        pluginModule(implementation(project(":modules:groovy")))

        pluginVerifier()
        zipSigner()
    }

    // Plain dependency, deliberately NOT wrapped in pluginModule(...): :modules:api carries the
    // bridge interfaces/DTOs (e.g. ScalaAnalyzerBridge) that this project's own always-loaded
    // dashboard code imports directly, so its classes must land on the main plugin jar's
    // classpath — not in a lib/modules/*.jar content module nothing in plugin.xml ever activates.
    implementation(project(":modules:api"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id.set("org.inferna.core")
        name.set("Inferna Core")
        version.set(project.version.toString())
        description.set(
            """
            <p><strong>Inferna Visceral</strong> brings a focused, industrial biopunk aesthetic, splash branding, and real-time JVM &amp; Spring telemetry to <strong>IntelliJ IDEA</strong> — both Community and Ultimate.</p>
            <h3>Features</h3>
            <ul>
              <li><strong>Inferna Visceral Dark UI Theme &amp; Editor Color Scheme:</strong> Deep obsidian surfaces (<code>#070203</code> / <code>#0a0405</code>) paired with high-contrast arterial crimson accents (<code>#b81424</code> / <code>#ff1e38</code>) tailored for IntelliJ's modern Islands UI, editor tabs, popups, and Block Terminal.</li>
              <li><strong>Automatic Splash Screen &amp; Icon Branding:</strong> Automatically synchronizes the Inferna Visceral splash screen and window icons while the Inferna theme is active, and cleanly restores the default IDE splash screen if you switch to another theme.</li>
              <li><strong>Inferna Biometrics Dashboard:</strong> Right-docked telemetry tool window tracking live JVM heap utilization, compilation status, and project-wide Spring bean counts (<code>@Component</code>, <code>@Service</code>, <code>@Repository</code>, <code>@Controller</code>, <code>@Configuration</code>, and <code>@Bean</code> declarations).</li>
              <li><strong>Smart Spring Tooling Integration:</strong> Seamlessly defers to IntelliJ IDEA Ultimate's native Spring engine when active (preventing duplicate gutter icons or conflicting inspections), while providing polyglot UAST Spring gutter navigation, <code>@Value</code> property resolution, and bean ambiguity quick-fixes on Community or whenever Ultimate's Spring plugin is absent.</li>
            </ul>
            """.trimIndent()
        )
        changeNotes.set(
            """
            <h3>1.0.0</h3>
            <ul>
              <li>Full compatibility with IntelliJ IDEA Community and Ultimate (2025.3 – 2026.x+) and JetBrains Marketplace.</li>
              <li>Added automatic, reversible splash screen cache synchronization and window icon branding when the Inferna Visceral Dark theme is active.</li>
              <li>Preserved all built-in IDE themes and color schemes in Settings &rarr; Appearance.</li>
              <li>Automatic detection of IntelliJ IDEA Ultimate's native Spring plugin to prevent duplicate gutter markers and false-positive inspections.</li>
              <li>Non-blocking background Spring bean indexing (including <code>@Configuration</code> and <code>@Bean</code> methods) in the Inferna Biometrics Dashboard.</li>
            </ul>
            """.trimIndent()
        )
        vendor {
            name.set("Lucas Henrique Novais de Araujo Petrola")
            email.set("petrolalucas@gmail.com")
            url.set("https://github.com/petrolal/inferna-core")
        }
        ideaVersion {
            sinceBuild.set("253")
            untilBuild.set(provider { null })
        }
    }

    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN"))
        channels.set(
            listOf(
                project.version.toString()
                    .substringAfter('-', missingDelimiterValue = "default")
                    .substringBefore('.')
                    .lowercase()
            )
        )
    }
}

tasks.register("printVersion") {
    description = "Prints the plugin version this build would publish, for CI tag/version checks."
    doLast { println(project.version) }
}

tasks {
    withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) {
            into("META-INF")
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    val brandingIconFiles = listOf(
        "inferna.ico", "inferna.png",
        "inferna_icon_16x16.png", "inferna_icon_24x24.png", "inferna_icon_32x32.png",
        "inferna_icon_48x48.png", "inferna_icon_64x64.png", "inferna_icon_128x128.png",
        "inferna_icon_256x256.png", "inferna_icon_512x512.png"
    )

    val syncResourceIcons = register<Copy>("syncResourceIcons") {
        from("assets/icons") { include(brandingIconFiles) }
        into("src/main/resources/icons")
    }

    val syncLauncherIcons = register<Copy>("syncLauncherIcons") {
        from("assets/icons") { include(brandingIconFiles) }
        into("launcher/icons")
    }

    register("syncBrandingIcons") {
        description = "Refreshes every generated copy of the canonical assets/icons/* raster icon set."
        dependsOn(syncResourceIcons, syncLauncherIcons)
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
                                logger.lifecycle("Patching $agentJar: injecting stub $classEntry (missing from resolved jar)")
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

    val prepareBranding = register("prepareBranding") {
        dependsOn(syncResourceIcons)
        val splash1x = file("src/main/resources/artwork/inferna_splash.png")
        val splash2x = file("src/main/resources/artwork/inferna_splash@2x.png")
        val splashJpeg = file("src/main/resources/artwork/inferna_core_ce_splash_screen.jpeg")
        val pluginIconSvg = file("src/main/resources/META-INF/pluginIcon.svg")
        val pluginIcon16Svg = file("src/main/resources/icons/pluginIcon16.svg")
        val dashboardSvg = file("src/main/resources/icons/infernaDashboard.svg")
        val icon512 = file("src/main/resources/icons/inferna_icon_512x512.png")
        val icon128 = file("src/main/resources/icons/inferna_icon_128x128.png")
        val icon32 = file("src/main/resources/icons/inferna_icon_32x32.png")
        val icon16 = file("src/main/resources/icons/inferna_icon_16x16.png")
        val brandingJar = layout.buildDirectory.file("inferna-branding.jar")
        inputs.files(splash1x, splash2x, splashJpeg, pluginIconSvg, pluginIcon16Svg, dashboardSvg, icon512, icon128, icon32, icon16)
        outputs.file(brandingJar)

        doLast {
            val jarFile = brandingJar.get().asFile
            jarFile.parentFile.mkdirs()
            ZipOutputStream(jarFile.outputStream()).use { zout ->
                fun addEntry(name: String, bytes: ByteArray) {
                    zout.putNextEntry(ZipEntry(name))
                    zout.write(bytes)
                    zout.closeEntry()
                }

                val b1x = splash1x.readBytes()
                val b2x = splash2x.readBytes()
                val bJpeg = splashJpeg.readBytes()
                val bSvg = pluginIconSvg.readBytes()
                val bSvg16 = pluginIcon16Svg.readBytes()
                val bDashSvg = dashboardSvg.readBytes()

                addEntry("idea_logo.png", b1x)
                addEntry("idea_logo@2x.png", b2x)
                addEntry("idea_logo_simplified.png", b1x)
                addEntry("idea_logo_simplified@2x.png", b2x)
                addEntry("idea_logo_subscription.png", b1x)
                addEntry("idea_logo_subscription@2x.png", b2x)
                addEntry("idea_logo_java_30.png", b1x)
                addEntry("idea_logo_java_30@2x.png", b2x)
                addEntry("idea_logo_kotlin_15.png", b1x)
                addEntry("idea_logo_kotlin_15@2x.png", b2x)
                addEntry("idea_logo_subscription_kotlin_15.png", b1x)
                addEntry("idea_logo_subscription_kotlin_15@2x.png", b2x)

                addEntry("idea_community_logo.png", b1x)
                addEntry("idea_community_logo@2x.png", b2x)
                addEntry("idea_community_logo_java_30.png", b1x)
                addEntry("idea_community_logo_java_30@2x.png", b2x)
                addEntry("idea_community_logo_kotlin_15.png", b1x)
                addEntry("idea_community_logo_kotlin_15@2x.png", b2x)

                addEntry("inferna_splash.png", b1x)
                addEntry("inferna_splash@2x.png", b2x)
                addEntry("artwork/inferna_splash.png", b1x)
                addEntry("artwork/inferna_splash@2x.png", b2x)
                addEntry("artwork/inferna_core_ce_splash_screen.jpeg", bJpeg)
                addEntry("splash.png", b1x)

                addEntry("inferna.svg", bSvg)
                addEntry("inferna_16.svg", bSvg16)
                addEntry("idea.svg", bSvg)
                addEntry("idea_16.svg", bSvg16)
                addEntry("idea_16@2x.svg", bSvg16)
                addEntry("idea-eap.svg", bSvg)
                addEntry("idea-eap_16.svg", bSvg16)
                addEntry("idea-eap_16@2x.svg", bSvg16)
                addEntry("idea-ce.svg", bSvg)
                addEntry("idea-ce_16.svg", bSvg16)
                addEntry("idea-ce_16@2x.svg", bSvg16)
                addEntry("idea-ce-eap.svg", bSvg)
                addEntry("idea-ce-eap_16.svg", bSvg16)
                addEntry("idea-ce-eap_16@2x.svg", bSvg16)
                addEntry("icons/infernaDashboard.svg", bDashSvg)
                addEntry("icon.png", icon512.readBytes())
                addEntry("icon_512.png", icon512.readBytes())
                addEntry("icon_128.png", icon128.readBytes())
                addEntry("icon_32.png", icon32.readBytes())
                addEntry("icon_16.png", icon16.readBytes())
            }
        }
    }

    named("processResources") {
        dependsOn(syncResourceIcons)
    }

    runIde {
        dependsOn(prepareBranding, syncLauncherIcons)
        val brandingJar = layout.buildDirectory.file("inferna-branding.jar").get().asFile
        classpath = files(brandingJar, classpath)
        jvmArgs(
            "-Xbootclasspath/a:${brandingJar.absolutePath}",
            "-Dsplash=true",
            "-Didea.show.splash.longer=true",
            "-Xmx4g",
            "-XX:+UseG1GC",
            "-Dawt.useSystemAAFontSettings=on"
        )
        doFirst {
            fileTree(layout.buildDirectory.dir("idea-sandbox")).matching {
                include("**/system/splash/**")
                include("**/system/icon-cache*")
            }.forEach { it.delete() }
        }
    }
}