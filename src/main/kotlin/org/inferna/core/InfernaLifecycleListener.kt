package org.inferna.core

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.ui.LafManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.advanced.AdvancedSettings

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
