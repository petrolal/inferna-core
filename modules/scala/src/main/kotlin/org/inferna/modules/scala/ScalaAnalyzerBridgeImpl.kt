/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araujo Petrola <petrolalucas@gmail.com>
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

package org.inferna.modules.scala

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.inferna.scala.DeepScalaStats
import org.inferna.scala.ScalaAnalyzerBridge
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

/**
 * Real `ScClass`/`ScObject`/`ScFunction` AST walk. This class is only ever classloaded when
 * `org.intellij.scala` is active, since it's registered exclusively through the optional
 * `inferna-scala.xml` descriptor — never referenced from the always-loaded core.
 */
class ScalaAnalyzerBridgeImpl : ScalaAnalyzerBridge {

    override fun inspectProject(project: Project): DeepScalaStats {
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
                        is ScClass -> if (element.isCase) caseClasses++
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

        return DeepScalaStats(
            objectSingletons = singletonObjects,
            caseClasses = caseClasses,
            implicitOrGivenDefs = implicitsCount
        )
    }
}
