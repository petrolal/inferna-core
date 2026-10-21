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

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.KotlinFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeManager

data class ProjectMatrixStats(
    val kotlinFiles: Int,
    val javaFiles: Int,
    val isMixedStubsOverhead: Boolean
)

class ProjectMatrixProvider : BiometricProvider<ProjectMatrixStats> {

    override fun isApplicable(project: Project): Boolean = true

    override fun scan(project: Project): ProjectMatrixStats {
        val scope = ProjectScope.getContentScope(project)
        val kotlinCount = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).size
        val javaCount = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope).size

        // Compilação em duas fases ocorre quando há presença relevante de ambos
        val isMixed = kotlinCount > 0 && javaCount > 0

        return ProjectMatrixStats(
            kotlinFiles = kotlinCount,
            javaFiles = javaCount,
            isMixedStubsOverhead = isMixed
        )
    }
}

class PolyglotMatrixScanner {

    fun countScalaFiles(project: Project): Int {
        val scope = ProjectScope.getContentScope(project)
        val scalaType = FileTypeManager.getInstance().getFileTypeByExtension("scala")
        return FileTypeIndex.getFiles(scalaType, scope).size
    }

    fun countGroovyFiles(project: Project): Int {
        val scope = ProjectScope.getContentScope(project)
        val groovyType = FileTypeManager.getInstance().getFileTypeByExtension("groovy")
        return FileTypeIndex.getFiles(groovyType, scope).size
    }
}