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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import org.inferna.dashboard.telemetry.ScalaTelemetryProvider
import org.inferna.dashboard.telemetry.ScalaTelemetryStats
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.theme.BiopunkTheme.biopunkHeader
import org.inferna.dashboard.ui.BiometricSectionRenderer
import java.awt.Dimension
import javax.swing.JLabel

class ScalaBiometricsSection(
    private val provider: ScalaTelemetryProvider = ScalaTelemetryProvider()
) : BiometricSectionRenderer {

    companion object {
        private const val LABEL_WIDTH_DP = 85
    }

    private lateinit var versionLabel: JLabel
    private lateinit var pipelineLabel: JLabel

    override fun render(panel: Panel, project: Project, parentDisposable: Disposable) {
        if (!provider.isApplicable(project)) return

        panel.apply {
            biopunkHeader("SCALA SYNAPTIC ENGINE")
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
        }
    }

    override fun refresh(project: Project) {
        if (!::versionLabel.isInitialized || project.isDisposed) return

        ReadAction.nonBlocking<ScalaTelemetryStats> {
            provider.scan(project)
        }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { stats ->
                val versionTag = stats.runtimeVersion ?: "Unknown"
                val dialectTag = if (stats.isScala3) "Scala 3 (Dotty)" else "Scala 2"
                versionLabel.text = "$dialectTag [$versionTag] — ${stats.fileCount} sources"
                pipelineLabel.text = stats.compilerPipeline
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}