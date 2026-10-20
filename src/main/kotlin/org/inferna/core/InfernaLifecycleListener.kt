/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araújo Petrola <petrolalucas@gmail.com>
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

package org.inferna.core

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Frame
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name

private const val INFERNA_THEME_NAME = "Inferna Visceral Dark"
private const val INFERNA_THEME_PROVIDER_ID = "org.inferna.theme.visceral"
private const val FIRST_RUN_KEY = "org.inferna.core.theme.initialized"
private const val BRANDING_SETTING_ID = "org.inferna.branding.enabled"
private const val SPLASH_1X_RESOURCE = "/artwork/inferna_splash.png"
private const val SPLASH_2X_RESOURCE = "/artwork/inferna_splash@2x.png"

private val FRAME_ICON_RESOURCES = listOf(
    "/icons/inferna_icon_16x16.png",
    "/icons/inferna_icon_24x24.png",
    "/icons/inferna_icon_32x32.png",
    "/icons/inferna_icon_48x48.png",
    "/icons/inferna_icon_64x64.png",
    "/icons/inferna_icon_128x128.png",
    "/icons/inferna_icon_256x256.png",
    "/icons/inferna_icon_512x512.png"
)

class InfernaLifecycleListener : AppLifecycleListener, LafManagerListener {
    private val logger = Logger.getInstance(InfernaLifecycleListener::class.java)

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        logger.info("Inferna Visceral platform plugin initializing...")
    }

    override fun appStarted() {
        runOnEdt {
            initializeThemeIfNeeded()
            syncBrandingForCurrentTheme()
        }
    }

    override fun lookAndFeelChanged(source: LafManager) {
        if (isInfernaActive(source)) {
            syncEditorColorScheme()
        }
        syncBrandingForCurrentTheme()
    }

    private fun initializeThemeIfNeeded() {
        val props = PropertiesComponent.getInstance()
        val lafManager = LafManager.getInstance()
        if (!props.getBoolean(FIRST_RUN_KEY, false)) {
            props.setValue(FIRST_RUN_KEY, true)
            val infernaLaf = lafManager.findLaf(INFERNA_THEME_PROVIDER_ID)
            if (infernaLaf != null && lafManager.currentUIThemeLookAndFeel?.id != INFERNA_THEME_PROVIDER_ID) {
                lafManager.setCurrentLookAndFeel(infernaLaf, true)
                lafManager.updateUI()
                logger.info("Activated Inferna Visceral Dark theme on initial plugin setup.")
            }
            syncEditorColorScheme()
        }
    }

    private fun syncEditorColorScheme() {
        val editorColorsManager = EditorColorsManager.getInstance()
        val infernaScheme = editorColorsManager.getScheme(INFERNA_THEME_NAME)
            ?: editorColorsManager.allSchemes.firstOrNull { it.name == INFERNA_THEME_NAME }
        if (infernaScheme != null && editorColorsManager.globalScheme.name != INFERNA_THEME_NAME) {
            editorColorsManager.setGlobalScheme(infernaScheme)
            logger.info("Synchronized Inferna Visceral Dark editor color scheme.")
        }
    }

    private fun isInfernaActive(lafManager: LafManager = LafManager.getInstance()): Boolean {
        val current = lafManager.currentUIThemeLookAndFeel ?: return false
        return current.id == INFERNA_THEME_PROVIDER_ID || current.name == INFERNA_THEME_NAME
    }

    private fun syncBrandingForCurrentTheme() {
        // Splash/icon branding is a toggle in Settings | Advanced Settings; when off, the
        // default IDE splash cache is restored and window icons are left alone.
        val active = isInfernaActive() && AdvancedSettings.getBoolean(BRANDING_SETTING_ID)
        if (active) {
            applyFrameIcons()
        }
        AppExecutorUtil.getAppExecutorService().execute {
            try {
                syncSplashCache(active)
            } catch (e: Exception) {
                logger.debug("Could not synchronize Inferna splash screen cache: ${e.message}")
            }
        }
    }

    private fun applyFrameIcons() {
        val icons: List<Image> = FRAME_ICON_RESOURCES.mapNotNull { path ->
            InfernaLifecycleListener::class.java.getResourceAsStream(path)?.use { stream ->
                runCatching { ImageIO.read(stream) }.getOrNull()
            }
        }
        if (icons.isEmpty()) return
        for (frame in Frame.getFrames()) {
            if (frame.isDisplayable) {
                frame.iconImages = icons
            }
        }
    }

    /**
     * Synchronizes IntelliJ Platform's binary `.ij` splash cache in `<systemPath>/splash/`
     * with the Inferna artwork when the Inferna theme is active, and restores the original
     * IDE splash cache from backup if the user switches to a different theme.
     */
    private fun syncSplashCache(infernaActive: Boolean) {
        val splashDir = Path.of(PathManager.getSystemPath(), "splash")
        if (!splashDir.exists() || !Files.isDirectory(splashDir)) return

        val ijFiles = Files.list(splashDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.extension == "ij" }.toList()
        }

        for (ijFile in ijFiles) {
            val backupFile = splashDir.resolve("${ijFile.name}.inferna-backup")
            val stampFile = splashDir.resolve("${ijFile.name}.inferna-stamp")

            if (!infernaActive) {
                if (backupFile.exists()) {
                    Files.copy(backupFile, ijFile, StandardCopyOption.REPLACE_EXISTING)
                    Files.deleteIfExists(stampFile)
                    logger.info("Restored default IDE splash cache for ${ijFile.name}.")
                }
                continue
            }

            if (stampFile.exists() && backupFile.exists()) {
                continue
            }

            if (!backupFile.exists()) {
                Files.copy(ijFile, backupFile, StandardCopyOption.REPLACE_EXISTING)
            }

            val dimensions = readSplashDimensions(backupFile) ?: continue
            val (width, height) = dimensions
            val resourcePath = if (width > 640) SPLASH_2X_RESOURCE else SPLASH_1X_RESOURCE
            val sourceImage = InfernaLifecycleListener::class.java.getResourceAsStream(resourcePath)?.use {
                ImageIO.read(it)
            } ?: continue

            writeSplashCacheFile(ijFile, sourceImage, width, height)
            Files.writeString(stampFile, INFERNA_THEME_PROVIDER_ID)
            logger.info("Applied Inferna Visceral splash screen to IDE splash cache (${width}x${height}).")
        }
    }

    private fun readSplashDimensions(file: Path): Pair<Int, Int>? {
        val bytes = Files.newInputStream(file).use { it.readNBytes(8) }
        if (bytes.size < 8) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val width = buffer.int
        val height = buffer.int
        if (width !in 64..4096 || height !in 64..4096) return null
        return width to height
    }

    private fun writeSplashCacheFile(target: Path, source: BufferedImage, width: Int, height: Int) {
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = scaled.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.drawImage(source, 0, 0, width, height, null)
        } finally {
            g2d.dispose()
        }

        val pixels = IntArray(width * height)
        scaled.getRGB(0, 0, width, height, pixels, 0, width)

        val buffer = ByteBuffer.allocate(8 + pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.asIntBuffer().put(pixels)

        val tempFile = Files.createTempFile(target.parent, target.name, ".tmp")
        try {
            Files.write(tempFile, buffer.array())
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(tempFile)
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
}

