/*
 * Inferna Core - Biopunk JVM & Spring Developer Environment
 * Copyright (C) 2026 Lucas Henrique Novais de Araújo Petrola <petrolalucas@gmail.com>
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

package org.inferna.spring

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.inferna.icons.InfernaIcons
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (SpringBeanResolver.isNativeSpringPluginActive()) return
        when (val uElement = element.toUElement()) {
            is UField -> collectForField(uElement, element, result)
            is UParameter -> collectForParameter(uElement, element, result)
            else -> return
        }
    }

    private fun collectForField(
        uField: UField,
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!uField.hasAnnotation(SpringBeanResolver.AUTOWIRED)) return
        addMarker(uField, uField.type, uField.sourcePsi?.firstChild ?: element, result)
    }

    private fun collectForParameter(
        uParameter: UParameter,
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val method = uParameter.uastParent as? UMethod ?: return
        if (!SpringBeanResolver.isInjectedConstructor(method)) return
        addMarker(uParameter, uParameter.type, element, result)
    }

    private fun addMarker(
        annotated: UAnnotated,
        type: com.intellij.psi.PsiType,
        anchor: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val candidates = SpringBeanResolver.candidatesFor(type)
        if (candidates.isEmpty()) return

        val qualifier = SpringBeanResolver.qualifierValue(annotated)
        val targets: List<PsiClass> = SpringBeanResolver.filterByQualifierAndPrimary(candidates, qualifier)

        val lineMarker = NavigationGutterIconBuilder.create(InfernaIcons.SpringBean)
            .setTargets(targets)
            .setTooltipText("Inferna: Navigate to Spring Bean Declaration")
            .createLineMarkerInfo(anchor)

        result.add(lineMarker)
    }
}
