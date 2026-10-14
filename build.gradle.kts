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
        // Required so the `instrumentCode` task can resolve the Java compiler
        // used to instrument NotNull/Nullable annotations and form bindings.
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
            "-Dgradle.compatibility.config.url="
        )
    }
}
