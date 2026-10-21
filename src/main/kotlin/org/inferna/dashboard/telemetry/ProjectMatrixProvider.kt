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

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

data class ProjectMatrixStats(
    val kotlinFiles: Int,
    val javaFiles: Int,
    val scalaFiles: Int,
    val groovyFiles: Int,
    val isMixedStubsOverhead: Boolean
)

class ProjectMatrixProvider : BiometricProvider<ProjectMatrixStats> {

    override fun isApplicable(project: Project): Boolean = true

    override fun scan(project: Project): ProjectMatrixStats {
        val scope = ProjectScope.getContentScope(project)
        val fileTypeManager = FileTypeManager.getInstance()
        // Looked up by extension rather than `org.jetbrains.kotlin.idea.KotlinFileType` on purpose:
        // any reference into that internal package makes the Kotlin plugin's own loader flag this
        // whole plugin "incompatible with Kotlin in K2 mode" and refuse to load it at all.
        val kotlinCount = FileTypeIndex.getFiles(fileTypeManager.getFileTypeByExtension("kt"), scope).size
        val javaCount = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope).size
        val scalaCount = FileTypeIndex.getFiles(fileTypeManager.getFileTypeByExtension("scala"), scope).size
        val groovyCount = FileTypeIndex.getFiles(fileTypeManager.getFileTypeByExtension("groovy"), scope).size

        // Dual-phase (Kapt/stub) build overhead occurs whenever more than one JVM source dialect coexists
        val isMixed = listOf(kotlinCount, javaCount, scalaCount, groovyCount).count { it > 0 } > 1

        return ProjectMatrixStats(
            kotlinFiles = kotlinCount,
            javaFiles = javaCount,
            scalaFiles = scalaCount,
            groovyFiles = groovyCount,
            isMixedStubsOverhead = isMixed
        )
    }
}