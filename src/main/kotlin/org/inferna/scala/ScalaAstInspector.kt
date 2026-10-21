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

package org.inferna.scala

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

data class ScalaAstMetrics(
    val objectSingletons: Int,
    val caseClasses: Int,
    val implicitOrGivenDefs: Int
)

class ScalaAstInspector {

    fun inspectProject(project: Project): ScalaAstMetrics {
        val scope = ProjectScope.getContentScope(project)
        val scalaFiles = FileTypeIndex.getFiles(ScalaFileType.INSTANCE, scope)
        val psiManager = PsiManager.getInstance(project)

        var singletonObjects = 0
        var caseClasses = 0
        var implicitsCount = 0

        for (virtualFile in scalaFiles) {
            val psiFile = psiManager.findFile(virtualFile) ?: continue

            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    when (element) {
                        is ScObject -> singletonObjects++
                        is ScClass -> {
                            if (element.isCase) caseClasses++
                        }
                        is ScFunction -> {
                            // Detect Scala 2 implicits and Scala 3 givens
                            if (element.hasModifierProperty("implicit") || element.text.startsWith("given")) {
                                implicitsCount++
                            }
                        }
                    }
                    super.visitElement(element)
                }
            })
        }

        return ScalaAstMetrics(
            objectSingletons = singletonObjects,
            caseClasses = caseClasses,
            implicitOrGivenDefs = implicitsCount
        )
    }
}