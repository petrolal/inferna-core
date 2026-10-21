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

package org.inferna.dashboard.telemetry

import com.intellij.facet.FacetManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

data class GroovyTelemetryStats(
    val fileCount: Int
)

class GroovyTelemetryProvider : BiometricProvider<GroovyTelemetryStats> {

    override fun isApplicable(project: Project): Boolean {
        val scope = ProjectScope.getContentScope(project)
        // Use FileTypeIndex, not FilenameIndex.getAllFilesByExt — the latter was found to miss
        // files that FileTypeIndex correctly reports (see ScalaTelemetryProvider).
        val groovyType = FileTypeManager.getInstance().getFileTypeByExtension("groovy")
        val hasFiles = FileTypeIndex.getFiles(groovyType, scope).isNotEmpty()
        if (hasFiles) return true

        return ModuleManager.getInstance(project).modules.any { module ->
            FacetManager.getInstance(module).allFacets.any {
                it.typeId.toString().contains("groovy", ignoreCase = true)
            }
        }
    }

    override fun scan(project: Project): GroovyTelemetryStats {
        val scope = ProjectScope.getContentScope(project)
        val groovyType = FileTypeManager.getInstance().getFileTypeByExtension("groovy")
        val groovyFiles = FileTypeIndex.getFiles(groovyType, scope).size
        return GroovyTelemetryStats(fileCount = groovyFiles)
    }
}
