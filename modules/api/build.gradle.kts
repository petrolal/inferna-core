/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araujo Petrola <petrolalucas@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Shared SPI contracts (bridges + their neutral DTOs) between the root plugin and the optional
 * language-engine modules. Depends on nothing but the IntelliJ platform itself, so both the root
 * project and :modules:scala/:modules:groovy can depend on it without a dependency cycle.
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
    intellijPlatform {
        if (localIdeDir != null) {
            local(localIdeDir)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }
    }
}

kotlin {
    jvmToolchain(21)
}
