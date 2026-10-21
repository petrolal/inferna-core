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

package org.inferna.dashboard.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class RefreshBiometricsAction(
    private val onRefresh: () -> Unit
) : DumbAwareAction("Refresh Biometrics", "Trigger telemetry rescan", AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) = onRefresh()
}

class PurgeMemoryAction :
    DumbAwareAction("Purge Heap (GC)", "Run System.gc()", AllIcons.Actions.GC) {
    override fun actionPerformed(e: AnActionEvent) = System.gc()
}

class RescanSpringAction(
    private val onRescan: () -> Unit
) : DumbAwareAction("Rescan Spring Beans", "Force bean re-indexing", AllIcons.Nodes.Services) {
    override fun actionPerformed(e: AnActionEvent) = onRescan()
}