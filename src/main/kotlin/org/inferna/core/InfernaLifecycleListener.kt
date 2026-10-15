package org.inferna.core

import com.intellij.configurationStore.BundledSchemeEP
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.UIThemeProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.advanced.AdvancedSettings

private const val INFERNA_THEME_NAME = "Inferna Visceral Dark"
private const val INFERNA_THEME_PROVIDER_ID = "org.inferna.theme.visceral"
// The platform resolves a required default LAF per active UI mode (LafManagerImplKt.getDefaultLaf)
// and throws IllegalStateException if that anchor isn't registered. Since we run in "islands" mode
// (ide.ui.theme.custom.islands=true), "Islands Dark"/"Islands Light" are that mode's anchors and
// must stay registered even though we never want them selectable; only the classic/new-UI theme
// providers (for modes we don't use) are safe to remove outright.
private val THEME_PROVIDER_IDS_TO_KEEP = setOf(INFERNA_THEME_PROVIDER_ID, "Islands Dark", "Islands Light")
// Inferna's editor scheme declares parent_scheme="Darcula" and inherits any attribute it doesn't
// override from it, so Darcula must stay registered - removing it would break those inherited
// colors. "Islands Dark" is the color scheme paired with the kept "Islands Dark" theme provider
// (same default-LAF-anchor risk), so it stays too. Every other bundled scheme is safe to drop.
private val COLOR_SCHEME_NAMES_TO_KEEP = setOf(INFERNA_THEME_NAME, "Darcula", "Islands Dark")
private val BUNDLED_COLOR_SCHEME_EP = ExtensionPointName.create<BundledSchemeEP>("com.intellij.bundledColorScheme")

class InfernaLifecycleListener : AppLifecycleListener {
    private val logger = Logger.getInstance(InfernaLifecycleListener::class.java)

    init {
        // Suppress remote Gradle JVM compatibility updates to prevent JavaVersion.parse crashes
        // caused by newer Java versions (e.g. Java 25+) listed in JetBrains' online compatibility matrix.
        System.setProperty("gradle.compatibility.update.interval", "0")
        System.setProperty("gradle.compatibility.config.url", "")
    }

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        logger.info("=========================================")
        logger.info(" Inferna IDE Engine Initializing...     ")
        logger.info(" Runtime: JVM 21 | Target: Biopunk OS   ")
        logger.info("=========================================")

        // appFrameCreated runs off the EDT in this platform build; AdvancedSettings.setBoolean can
        // synchronously fire a listener (IslandsUICustomization) that opens a "restart?" dialog,
        // which requires the EDT and crashes otherwise.
        runOnEdt {
            try {
                AdvancedSettings.setBoolean("ide.ui.theme.custom.islands", true)
            } catch (e: Exception) {
                logger.debug("AdvancedSettings ide.ui.theme.custom.islands not available: ${e.message}")
            }

            restrictToInfernaOnly()
        }
    }

    override fun appStarted() {
        logger.info("Inferna Platform active. All subsystems dispatched.")
        runOnEdt {
            enforceInfernaTheme()
        }
    }

    // Unregisters every other bundled UI theme and editor color scheme so Settings > Appearance
    // never offers them: both themeProvider and bundledColorScheme are declared dynamic="true" by
    // the platform itself (meant to support runtime add/remove), so this is a supported mutation,
    // not a hack - unlike patching the IDE's own files. Must run before any theme is re-applied so
    // nothing still references the providers we're about to remove.
    // unregisterExtension(T) is deprecated in favor of a predicate-based bulk API that needs the
    // raw ExtensionComponentAdapter, not the resolved bean; removing a short, fixed list of known
    // instances by identity at startup is exactly what it was built for, so we keep using it.
    @Suppress("DEPRECATION")
    private fun restrictToInfernaOnly() {
        val themePoint = UIThemeProvider.EP_NAME.point
        UIThemeProvider.EP_NAME.extensionList
            .filter { it.id !in THEME_PROVIDER_IDS_TO_KEEP }
            .forEach { themePoint.unregisterExtension(it) }

        val schemePoint = BUNDLED_COLOR_SCHEME_EP.point
        BUNDLED_COLOR_SCHEME_EP.extensionList
            .filter { it.path?.endsWith("inferna.xml", ignoreCase = true) != true }
            .forEach { schemePoint.unregisterExtension(it) }

        // The EP unregistration above only stops future (re)loading; by appFrameCreated every
        // bundled scheme is already loaded into EditorColorsManagerImpl's own registry, so it must
        // be evicted there too for the Color Scheme picker to stop listing it.
        val editorColorsManager = EditorColorsManager.getInstance()
        if (editorColorsManager is EditorColorsManagerImpl) {
            editorColorsManager.allSchemes
                .filter { it.name !in COLOR_SCHEME_NAMES_TO_KEEP }
                .forEach { editorColorsManager.removeScheme(it) }
        }

        logger.info("Restricted installed themes and color schemes to Inferna Visceral (+ required platform defaults: Islands Dark/Light, Darcula).")
    }

    private fun runOnEdt(action: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDispatchThread) {
            app.invokeLater(action, ModalityState.any())
        } else {
            action()
        }
    }

    // Since Inferna is now the only registered theme/scheme, this just self-corrects any session
    // that still has a different one persisted from before this lock existed (e.g. an install that
    // picked Darcula pre-upgrade); it's a no-op once already on Inferna.
    private fun enforceInfernaTheme() {
        val lafManager = LafManager.getInstance()
        val infernaLaf = lafManager.installedThemes.firstOrNull { it.name == INFERNA_THEME_NAME }
        when {
            infernaLaf == null ->
                logger.warn("Inferna Visceral theme not found among installed themes; cannot enforce.")
            lafManager.currentUIThemeLookAndFeel?.name != INFERNA_THEME_NAME -> {
                lafManager.setCurrentLookAndFeel(infernaLaf, true)
                lafManager.updateUI()
                logger.info("Inferna Visceral enforced as the active theme.")
            }
        }

        val editorColorsManager = EditorColorsManager.getInstance()
        val infernaScheme = editorColorsManager.getScheme(INFERNA_THEME_NAME)
            ?: editorColorsManager.allSchemes.firstOrNull { it.name == INFERNA_THEME_NAME }
        when {
            infernaScheme == null ->
                logger.warn("Inferna Visceral Dark editor color scheme not found among installed schemes.")
            editorColorsManager.globalScheme.name != INFERNA_THEME_NAME -> {
                editorColorsManager.setGlobalScheme(infernaScheme)
                logger.info("Inferna Visceral Dark enforced as the active editor color scheme.")
            }
        }
    }
}
