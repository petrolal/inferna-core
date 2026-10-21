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
import org.inferna.dashboard.telemetry.ScalaTelemetryProvider
import org.inferna.dashboard.telemetry.ScalaTelemetryStats
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.theme.BiopunkTheme.biopunkGroup
import org.inferna.dashboard.ui.BiometricSectionRenderer
import org.inferna.scala.DeepScalaStats
import org.inferna.scala.ScalaAnalyzerBridge
import java.awt.Dimension
import javax.swing.JLabel

/**
 * Queries [ScalaAnalyzerBridge] purely via [ApplicationManager.getApplication].getService — never
 * imports anything from `org.jetbrains.plugins.scala`. The service is null whenever
 * `:modules:scala`'s optional descriptor hasn't been loaded (Scala plugin absent/disabled),
 * so this section degrades to the heuristic [ScalaTelemetryProvider] stats with no crash.
 */
class ScalaBiometricsSection(
    private val provider: ScalaTelemetryProvider = ScalaTelemetryProvider()
) : BiometricSectionRenderer {

    companion object {
        private const val LABEL_WIDTH_DP = 85
    }

    private lateinit var versionLabel: JLabel
    private lateinit var pipelineLabel: JLabel
    private lateinit var astLabel: JLabel

    override fun isApplicable(project: Project): Boolean = provider.isApplicable(project)

    override fun render(panel: Panel, project: Project, parentDisposable: Disposable) {
        panel.apply {
            biopunkGroup("SCALA SYNAPTIC ENGINE") {
                row {
                    label("Runtime:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    versionLabel = label("Detecting Scala SDK...")
                        .applyToComponent {
                            font = BiopunkTheme.FONT_MONO_REGULAR
                            foreground = BiopunkTheme.ACCENT_CRIMSON
                        }.component
                }
                row {
                    label("Bridge:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    pipelineLabel = label("Detecting bridge...")
                        .applyToComponent { foreground = BiopunkTheme.TEXT_MUTED }
                        .component
                }
                row {
                    label("Deep AST:").applyToComponent {
                        preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                    }
                    astLabel = label("Scala plugin not active")
                        .applyToComponent { foreground = BiopunkTheme.TEXT_MUTED }
                        .component
                }
            }
        }
    }

    override fun refresh(project: Project) {
        if (!::versionLabel.isInitialized || project.isDisposed) return

        ReadAction.nonBlocking<Pair<ScalaTelemetryStats, DeepScalaStats?>> {
            val bridge = ApplicationManager.getApplication().getService(ScalaAnalyzerBridge::class.java)
            provider.scan(project) to bridge?.inspectProject(project)
        }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { (stats, deep) ->
                val versionTag = stats.runtimeVersion ?: "Unknown"
                val dialectTag = if (stats.isScala3) "Scala 3 (Dotty)" else "Scala 2"
                versionLabel.text = "$dialectTag [$versionTag] — ${stats.fileCount} sources"
                pipelineLabel.text = stats.compilerPipeline
                astLabel.text = deep?.let {
                    "objects=${it.objectSingletons} case=${it.caseClasses} implicit/given=${it.implicitOrGivenDefs}"
                } ?: "Scala plugin not active"
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}