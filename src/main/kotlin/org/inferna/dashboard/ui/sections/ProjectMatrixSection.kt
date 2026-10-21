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
import org.inferna.dashboard.telemetry.ProjectMatrixProvider
import org.inferna.dashboard.telemetry.ProjectMatrixStats
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.theme.BiopunkTheme.biopunkHeader
import org.inferna.dashboard.ui.BiometricSectionRenderer
import java.awt.Dimension
import javax.swing.JLabel

class ProjectMatrixSection(
    private val provider: ProjectMatrixProvider = ProjectMatrixProvider()
) : BiometricSectionRenderer {

    companion object {
        private const val LABEL_WIDTH_DP = 85
    }

    private lateinit var matrixLabel: JLabel
    private lateinit var overheadLabel: JLabel

    override fun render(panel: Panel, project: Project, parentDisposable: Disposable) {
        if (!provider.isApplicable(project)) return

        panel.apply {
            biopunkHeader("POLYGONAL MATRIX")
            row {
                label("Sources:").applyToComponent {
                    preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                }
                matrixLabel = label("Scanning workspace...")
                    .applyToComponent {
                        font = BiopunkTheme.FONT_MONO_REGULAR
                        foreground = BiopunkTheme.TEXT_PRIMARY
                    }.component
            }
            row {
                label("Pipeline:").applyToComponent {
                    preferredSize = Dimension(JBUI.scale(LABEL_WIDTH_DP), preferredSize.height)
                }
                overheadLabel = label("Analyzing build path...")
                    .applyToComponent { foreground = BiopunkTheme.TEXT_MUTED }
                    .component
            }
        }
    }

    override fun refresh(project: Project) {
        if (!::matrixLabel.isInitialized || project.isDisposed) return

        ReadAction.nonBlocking<ProjectMatrixStats> {
            provider.scan(project)
        }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { stats ->
                val sourceTokens = buildList {
                    if (stats.kotlinFiles > 0) add("KT: ${stats.kotlinFiles}")
                    if (stats.javaFiles > 0) add("Java: ${stats.javaFiles}")
                    if (stats.scalaFiles > 0) add("Scala: ${stats.scalaFiles}")
                    if (stats.groovyFiles > 0) add("Groovy: ${stats.groovyFiles}")
                }

                matrixLabel.text = if (sourceTokens.isEmpty()) {
                    "Empty Source Path"
                } else {
                    sourceTokens.joinToString(" | ")
                }

                overheadLabel.text = if (stats.isMixedStubsOverhead) {
                    "Dual-phase build overhead (Kapt/Stubs)"
                } else {
                    "Single-pass optimized compilation"
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}