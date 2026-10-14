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
        // Required so the `instrumentCode` task can resolve the Java compiler
        // used to instrument NotNull/Nullable annotations and form bindings.
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        pluginVerifier()
        zipSigner()
        // Provides the Java compiler dependency consumed by `instrumentCode`.
        instrumentationTools()
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
