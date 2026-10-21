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
    // :modules:api carries GroovyAnalyzerBridge/DeepGroovyStats and depends on nothing in this
    // project tree, mirroring :modules:scala's avoidance of a root -> this module -> root cycle
    // through pluginModule(...) bundling.
    compileOnly(project(":modules:api"))

    intellijPlatform {
        if (localIdeDir != null) {
            local(localIdeDir)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }

        // Groovy support ships bundled with every IDEA edition, but kept as its own module
        // (its own optional <depends config-file>) for SRP: no Groovy-aware class is ever
        // classloaded on a run where Groovy support is disabled for the project.
        bundledPlugin("org.intellij.groovy")

        // GrTypeDefinition et al. extend Java PSI supertypes (PsiClass, PsiMethod, ...); needed
        // at compile time for the same reason :modules:scala depends on it.
        bundledPlugin("com.intellij.java")
    }
}

kotlin {
    jvmToolchain(21)
}
