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

package org.inferna.modules.groovy

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.inferna.groovy.DeepGroovyStats
import org.inferna.groovy.GroovyAnalyzerBridge
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod

private const val COMPILE_STATIC_FQN = "groovy.transform.CompileStatic"

class GroovyAnalyzerBridgeImpl : GroovyAnalyzerBridge {
    override fun inspectProject(project: Project): DeepGroovyStats {
        val scope = ProjectScope.getContentScope(project)
        val groovyFiles = FileTypeIndex.getFiles(GroovyFileType.GROOVY_FILE_TYPE, scope)
        val psiManager = PsiManager.getInstance(project)
        var typeDefinitions = 0
        var traits = 0
        var closures = 0
        var compileStaticMembers = 0

        for (virtualFile in groovyFiles) {
            val psiFile = psiManager.findFile(virtualFile) ?: continue
            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    when (element) {
                        is GrTypeDefinition -> {
                            typeDefinitions++
                            if (element.isTrait) traits++
                            if (element.hasCompileStatic()) compileStaticMembers++
                        }
                        is GrClosableBlock -> closures++
                        is GrMethod -> if (element.hasCompileStatic()) compileStaticMembers++
                    }
                    super.visitElement(element)
                }
            })
        }

        return DeepGroovyStats(typeDefinitions, traits, closures, compileStaticMembers)
    }

    private fun PsiModifierListOwner.hasCompileStatic(): Boolean =
        modifierList?.findAnnotation(COMPILE_STATIC_FQN) != null
}
