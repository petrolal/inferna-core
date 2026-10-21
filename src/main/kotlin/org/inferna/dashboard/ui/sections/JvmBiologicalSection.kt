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
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import org.inferna.dashboard.telemetry.JvmBiologicalProvider
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.theme.BiopunkTheme.biopunkGroup
import org.inferna.dashboard.ui.BiometricSectionRenderer
import javax.swing.JLabel

class JvmBiologicalSection(
    private val provider: JvmBiologicalProvider = JvmBiologicalProvider()
) : BiometricSectionRenderer {

    private lateinit var heapLabel: JLabel
    private lateinit var circulationLabel: JLabel

    override fun render(panel: Panel, project: Project, parentDisposable: Disposable) {
        panel.apply {
            biopunkGroup("JVM BIOLOGICAL STATE") {
                row("Heap:") {
                    heapLabel = label("Monitoring...")
                        .applyToComponent {
                            font = BiopunkTheme.FONT_MONO_REGULAR
                            foreground = BiopunkTheme.ACCENT_CRIMSON
                        }.component
                }
                row("Circulation:") {
                    circulationLabel = label("Scanning threads...")
                        .applyToComponent { foreground = BiopunkTheme.TEXT_SECONDARY }
                        .component
                }
            }
        }
    }

    override fun refresh(project: Project) {
        if (!::heapLabel.isInitialized) return
        val stats = provider.scan(project)
        val gauge = BiopunkTheme.renderAsciiGauge(stats.percentage)
        heapLabel.text = "${stats.usedMb}MB / ${stats.maxMb}MB $gauge [${stats.percentage}%]"
        circulationLabel.text = "${stats.activeThreads} threads | ${stats.gcCount} GC cycles"
    }
}