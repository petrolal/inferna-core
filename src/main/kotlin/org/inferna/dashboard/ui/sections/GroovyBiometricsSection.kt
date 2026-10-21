/*
 * Inferna Visceral — Biopunk Theme, Splash Branding & Biometrics for IntelliJ IDEA
 * Copyright (C) 2026 Lucas Petrola <petrolalucas@gmail.com>
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

package org.inferna.dashboard.ui.sections

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import org.inferna.dashboard.telemetry.GroovyTelemetryProvider
import org.inferna.dashboard.telemetry.GroovyTelemetryStats
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.theme.BiopunkTheme.biopunkGroup
import org.inferna.dashboard.ui.BiometricSectionRenderer
import org.inferna.groovy.DeepGroovyStats
import org.inferna.groovy.GroovyAnalyzerBridge
import java.awt.Dimension
import javax.swing.JLabel

/**
 * Queries [GroovyAnalyzerBridge] purely via [ApplicationManager.getApplication].getService — never
 * imports anything from `org.jetbrains.plugins.groovy`. The service is null whenever
 * `:modules:groovy`'s optional descriptor hasn't been loaded (Groovy support disabled), so this
 * section degrades to the heuristic [GroovyTelemetryProvider] stats with no crash.
 */
class GroovyBiometricsSection(
    private val provider: GroovyTelemetryProvider = GroovyTelemetryProvider()
) : BiometricSectionRenderer {

    companion object {
        private const val LABEL_WIDTH_DP = 85
    }

    private lateinit var filesLabel: JLabel
    private lateinit var staticLabel: JLabel
    private lateinit var astLabel: JLabel

    override fun isApplicable(project: Project): Boolean = provider.isApplicable(project)

    override fun render(panel: Panel, project: Project, parentDisposable: Disposable) {
        panel.apply {
            biopunkGroup("GROOVY METABOLIC TRACE") {
                row {
                    label("Sources:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    filesLabel = label("Scanning...")
                        .applyToComponent {
                            font = BiopunkTheme.FONT_MONO_REGULAR
                            foreground = BiopunkTheme.ACCENT_CRIMSON
                        }.component
                }
                row {
                    label("Static:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    staticLabel = label("Detecting bridge...")
                        .applyToComponent { foreground = BiopunkTheme.TEXT_MUTED }
                        .component
                }
                row {
                    label("Deep AST:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    astLabel = label("Groovy plugin not active")
                        .applyToComponent { foreground = BiopunkTheme.TEXT_MUTED }
                        .component
                }
            }
        }
    }

    override fun refresh(project: Project) {
        if (!::filesLabel.isInitialized || project.isDisposed) return

        ReadAction.nonBlocking<Pair<GroovyTelemetryStats, DeepGroovyStats?>> {
            val bridge = ApplicationManager.getApplication().getService(GroovyAnalyzerBridge::class.java)
            provider.scan(project) to bridge?.inspectProject(project)
        }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { (stats, deep) ->
                val versionTag = stats.runtimeVersion?.let { "Groovy $it" } ?: "Groovy"
                filesLabel.text = "$versionTag — ${stats.fileCount} sources"
                staticLabel.text = deep?.let {
                    val percent = if (it.compileStaticEligible > 0) {
                        (it.compileStaticMembers * 100) / it.compileStaticEligible
                    } else 0
                    "${it.compileStaticMembers}/${it.compileStaticEligible} @CompileStatic ($percent%)"
                } ?: "Groovy plugin not active"
                astLabel.text = deep?.let {
                    "types=${it.typeDefinitions} traits=${it.traits} closures=${it.closures}"
                } ?: "Groovy plugin not active"
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}
