/*
 * Shared local-IDE resolution, applied from the root and every :modules:* build script so a
 * Nix/local IDE install is picked up consistently everywhere instead of duplicating this logic
 * per module. Sets `localIdeDir: File?` as an extra project property.
 */

val localIdeDir = providers.gradleProperty("localPlatformPath").orNull?.let { file(it) }
    ?: file("/run/current-system/sw/bin/intellij-idea-oss").takeIf { it.exists() }?.let { binary ->
        binary.toPath().toRealPath().parent?.parent?.toFile()?.takeIf { dir ->
            dir.resolve("product-info.json").exists()
        }
    }
    ?: rootProject.file(".intellijPlatform/localPlatformArtifacts").takeIf { it.isDirectory }?.let { artifactsDir ->
        artifactsDir.listFiles { f -> f.name.startsWith("localIde-") && f.name.endsWith(".xml") }
            ?.firstNotNullOfOrNull { xml ->
                Regex("""artifact name="([^"]+)"""").find(xml.readText())?.groupValues?.get(1)?.let { relPath ->
                    File("/", relPath).takeIf { dir -> dir.resolve("product-info.json").exists() }
                }
            }
    }

extra["localIdeDir"] = localIdeDir
