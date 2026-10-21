/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araujo Petrola <petrolalucas@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij.platform.module")
}

apply(from = "${rootDir}/gradle/scripts/local-ide.gradle.kts")

@Suppress("UNCHECKED_CAST")
val localIdeDir = project.extra["localIdeDir"] as File?

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // :modules:api carries ScalaAnalyzerBridge/DeepScalaStats and depends on nothing in this
    // project tree, so depending on it (rather than on the root plugin project directly) avoids
    // a root -> :modules:scala -> root dependency cycle through pluginModule(...) bundling.
    compileOnly(project(":modules:api"))

    intellijPlatform {
        if (localIdeDir != null) {
            local(localIdeDir)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }

        // Scala PSI types extend Java PSI supertypes (PsiClass, PsiMethod, ...); needed at
        // compile time here for the same reason the root project depends on it.
        bundledPlugin("com.intellij.java")

        // Provided by the Scala Marketplace plugin at runtime — never bundled into our zip.
        // Pin to a release that matches the root `platformVersion`; bump alongside it.
        plugin(providers.gradleProperty("scalaPluginVersion").map { "org.intellij.scala:$it" })
    }
}

kotlin {
    jvmToolchain(21)
}
