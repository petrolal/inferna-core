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
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope

data class ScalaTelemetryStats(
    val fileCount: Int,
    val runtimeVersion: String?,
    val isScala3: Boolean,
    val compilerPipeline: String
)

class ScalaTelemetryProvider : BiometricProvider<ScalaTelemetryStats> {

    override fun isApplicable(project: Project): Boolean {
        val scope = ProjectScope.getContentScope(project)
        val hasFiles = FilenameIndex.getAllFilesByExt(project, "scala", scope).isNotEmpty()
        if (hasFiles) return true

        return ModuleManager.getInstance(project).modules.any { module ->
            FacetManager.getInstance(module).allFacets.any {
                it.typeId.toString().contains("scala", ignoreCase = true)
            }
        }
    }

    override fun scan(project: Project): ScalaTelemetryStats {
        val scope = ProjectScope.getContentScope(project)
        val scalaType = FileTypeManager.getInstance().getFileTypeByExtension("scala")
        val scalaFiles = FileTypeIndex.getFiles(scalaType, scope).size

        var detectedVersion: String? = null
        var foundScala3 = false

        OrderEnumerator.orderEntries(project).librariesOnly().forEachLibrary { lib ->
            val name = lib.name ?: ""
            when {
                name.contains("scala3-library", ignoreCase = true) ||
                        name.contains("scala3-compiler", ignoreCase = true) -> {
                    foundScala3 = true
                    detectedVersion = extractVersion(name) ?: "3.x"
                    return@forEachLibrary false
                }
                name.contains("scala-library", ignoreCase = true) -> {
                    if (detectedVersion == null) {
                        detectedVersion = extractVersion(name) ?: "2.13.x"
                    }
                }
            }
            true
        }

        val pipeline = when {
            foundScala3 -> "Zinc / TASTy Pipeline"
            detectedVersion != null -> "Zinc Incremental (Scalac)"
            else -> "Scalac standard"
        }

        return ScalaTelemetryStats(
            fileCount = scalaFiles,
            runtimeVersion = detectedVersion,
            isScala3 = foundScala3,
            compilerPipeline = pipeline
        )
    }

    private fun extractVersion(libName: String): String? {
        val regex = Regex("""(?i)scala(?:3)?-(?:library|compiler)[_-]([0-9]+\.[0-9]+\.[0-9]+)""")
        return regex.find(libName)?.groupValues?.get(1)
    }
}