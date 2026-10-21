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

package org.inferna.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.inferna.branding.InfernaBrandingManager

class InfernaStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val app = ApplicationManager.getApplication()
        val syncTask = {
            InfernaBrandingManager.initializeThemeIfNeeded()
            InfernaBrandingManager.syncBranding()
        }
        if (app.isDispatchThread) {
            syncTask()
        } else {
            app.invokeLater(syncTask, ModalityState.any())
        }
    }
}