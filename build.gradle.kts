import java.io.File
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
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
                    // Some local IDE distributions resolve a kotlinx-coroutines-core version whose
                    // coroutines-javaagent.jar is missing kotlinx/coroutines/debug/AgentPremain.class
                    // (it was split out in later releases), which makes the IDE's debug-agent
                    // attachment fail at startup with a NoClassDefFoundError. The class below is a
                    // trivial no-op premain() (compiled from a single-method Java stub, embedded
                    // here as Base64 because the source is not part of this project and recompiling
                    // it per-build would require shipping a throwaway .java file just for this
                    // backport). We only inject it when the real class is absent, so this is a no-op
                    // on any jar that already ships a working AgentPremain.
                    //
                    //   package kotlinx.coroutines.debug;
                    //   public class AgentPremain {
                    //       public static void premain(String args, java.lang.instrument.Instrumentation inst) {}
                    //   }
                    val classEntry = "kotlinx/coroutines/debug/AgentPremain.class"
                    val b64 = "yv66vgAAAEEADwoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAla290bGlueC9jb3JvdXRpbmVzL2RlYnVnL0FnZW50UHJlbWFpbgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAAdwcmVtYWluAQA7KExqYXZhL2xhbmcvU3RyaW5nO0xqYXZhL2xhbmcvaW5zdHJ1bWVudC9JbnN0cnVtZW50YXRpb247KVYBAApTb3VyY2VGaWxlAQARQWdlbnRQcmVtYWluLmphdmEAIQAHAAIAAAAAAAIAAQAFAAYAAQAJAAAAHQABAAEAAAAFKrcAAbEAAAABAAoAAAAGAAEAAAADAAkACwAMAAEACQAAABkAAAACAAAAAbEAAAABAAoAAAAGAAEAAAAEAAEADQAAAAIADg=="
                    val classBytes = Base64.getDecoder().decode(b64)
                    val tempJar = File(agentJar.parentFile, "coroutines-javaagent-temp.jar")
                    ZipInputStream(agentJar.inputStream()).use { zin ->
                        ZipOutputStream(tempJar.outputStream()).use { zout ->
                            var entry = zin.nextEntry
                            var hasClass = false
                            while (entry != null) {
                                // Copy every existing entry as-is; if AgentPremain.class is already
                                // present (e.g. a future jar ships it again) we keep that one and
                                // skip our patch below, rather than risk writing a duplicate/stale copy.
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
        val splash1x = file("src/main/resources/artwork/inferna_splash.png")
        val splash2x = file("src/main/resources/artwork/inferna_splash@2x.png")
        val splashJpeg = file("src/main/resources/artwork/inferna_ide_ce_splash_screen.jpeg")
        val pluginIconSvg = file("src/main/resources/META-INF/pluginIcon.svg")
        val dashboardSvg = file("src/main/resources/icons/infernaDashboard.svg")
        val icon512 = file("src/main/resources/icons/inferna_icon_512x512.png")
        val icon128 = file("src/main/resources/icons/inferna_icon_128x128.png")
        val icon32 = file("src/main/resources/icons/inferna_icon_32x32.png")
        val icon16 = file("src/main/resources/icons/inferna_icon_16x16.png")
        val brandingJar = layout.buildDirectory.file("inferna-branding.jar")
        inputs.files(splash1x, splash2x, splashJpeg, pluginIconSvg, dashboardSvg, icon512, icon128, icon32, icon16)
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
                val bDashSvg = dashboardSvg.readBytes()

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
                addEntry("artwork/inferna_ide_ce_splash_screen.jpeg", bJpeg)
                addEntry("splash.png", b1x)

                // Desktop OS panel, taskbar, dock, and window frame icons
                addEntry("inferna.svg", bSvg)
                addEntry("inferna_16.svg", bSvg)
                addEntry("idea-ce.svg", bSvg)
                addEntry("idea-ce_16.svg", bSvg)
                addEntry("idea-ce_16@2x.svg", bSvg)
                addEntry("idea-ce-eap.svg", bSvg)
                addEntry("idea-ce-eap_16.svg", bSvg)
                addEntry("idea-ce-eap_16@2x.svg", bSvg)
                addEntry("icons/infernaDashboard.svg", bDashSvg)
                addEntry("icon.png", icon512.readBytes())
                addEntry("icon_512.png", icon512.readBytes())
                addEntry("icon_128.png", icon128.readBytes())
                addEntry("icon_32.png", icon32.readBytes())
                addEntry("icon_16.png", icon16.readBytes())

                val buildNumber = providers.gradleProperty("appInfoBuildNumber").get()
                val majorVersion = providers.gradleProperty("appInfoMajorVersion").get()
                val minorVersion = providers.gradleProperty("appInfoMinorVersion").get()
                val majorReleaseDate = providers.gradleProperty("appInfoMajorReleaseDate").get()
                val buildDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))

                val appInfoXml = """
                    <component xmlns="http://jetbrains.org/intellij/schema/application-info">
                      <version major="$majorVersion" minor="$minorVersion"/>
                      <company name="Inferna Project" url="https://github.com/inferna"/>
                      <build number="$buildNumber" date="$buildDate" majorReleaseDate="$majorReleaseDate" />
                      <logo url="/artwork/inferna_splash.png"/>
                      <icon svg="/inferna.svg" svg-small="/inferna_16.svg"/>
                      <icon-eap svg="/inferna.svg" svg-small="/inferna_16.svg"/>
                      <names product="Inferna" fullname="Inferna IDE" script="inferna" motto="Biopunk Standalone JVM Environment"/>
                      <essential-plugin>com.intellij.java</essential-plugin>
                      <essential-plugin>com.intellij.java.ide</essential-plugin>
                      <essential-plugin>com.intellij.modules.json</essential-plugin>
                    </component>
                """.trimIndent()

                addEntry("idea/IdeaApplicationInfo.xml", appInfoXml.toByteArray(Charsets.UTF_8))
                addEntry("idea/ApplicationInfo.xml", appInfoXml.toByteArray(Charsets.UTF_8))
            }
        }
    }

    runIde {
        dependsOn(prepareBranding)
        val brandingJar = layout.buildDirectory.file("inferna-branding.jar").get().asFile
        classpath = files(brandingJar, classpath)
        jvmArgs(
            "-Xbootclasspath/a:${brandingJar.absolutePath}",
            "-Dintellij.platform.load.app.info.from.resources=true",
            "-Dsplash=true",
            "-Didea.show.splash.longer=true",
            "-Xmx4g",
            "-XX:+UseG1GC",
            "-Dawt.useSystemAAFontSettings=on",
            "-Dgradle.compatibility.update.interval=0",
            "-Dgradle.compatibility.config.url=",
            "-Didea.fatal.error.notification=disabled",
            "-Didea.is.internal=false"
        )
        doFirst {
            fileTree(layout.buildDirectory.dir("idea-sandbox")).matching {
                include("**/system/splash/**")
                include("**/system/icon-cache*")
                include("**/system/.appinfo")
            }.forEach { it.delete() }
        }
    }
}
