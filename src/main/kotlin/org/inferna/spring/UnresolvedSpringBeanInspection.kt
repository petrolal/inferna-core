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

package org.inferna.spring

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.jvm.JvmModifiersOwner
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiType
import org.inferna.spring.interop.JvmAnnotationQuickFixSupport
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

/**
 * Flags @Autowired injection points (fields and implicitly-injected constructor
 * parameters, per [SpringBeanResolver.isInjectedConstructor]) that resolve to zero
 * Spring bean candidates, or to more than one with nothing to disambiguate them.
 * Shares candidate/qualifier/primary resolution with [SpringBeanLineMarkerProvider]
 * so both features agree on what "resolved" means.
 */
class UnresolvedSpringBeanInspection : AbstractBaseUastLocalInspectionTool(UField::class.java, UParameter::class.java) {

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (SpringBeanResolver.isNativeSpringPluginActive()) return null
        if (!field.hasAnnotation(SpringBeanResolver.AUTOWIRED)) return null
        return describeInjectionPoint(field, field.type, manager, isOnTheFly)
    }

    override fun checkParameter(parameter: UParameter, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (SpringBeanResolver.isNativeSpringPluginActive()) return null
        val method = parameter.uastParent as? UMethod ?: return null
        if (!SpringBeanResolver.isInjectedConstructor(method)) return null
        return describeInjectionPoint(parameter, parameter.type, manager, isOnTheFly)
    }

    private fun describeInjectionPoint(
        annotated: UAnnotated,
        type: PsiType,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val anchor = (annotated.sourcePsi as? PsiNameIdentifierOwner)?.nameIdentifier
            ?: annotated.sourcePsi
            ?: return null

        val candidates = SpringBeanResolver.candidatesFor(type)
        if (candidates.isEmpty()) {
            // Nothing to point a quick-fix at: the project has no @Component-family
            // class assignable to the target type yet.
            return arrayOf(
                manager.createProblemDescriptor(
                    anchor,
                    "No Spring bean candidate found for injection type '${type.presentableText}'",
                    isOnTheFly,
                    emptyArray<LocalQuickFix>(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            )
        }

        val qualifier = SpringBeanResolver.qualifierValue(annotated)
        val resolved = SpringBeanResolver.filterByQualifierAndPrimary(candidates, qualifier)
        if (resolved.size <= 1) return null

        val fixes = qualifierQuickFixes(annotated, resolved, anchor.containingFile)
        return arrayOf(
            manager.createProblemDescriptor(
                anchor,
                "Multiple Spring bean candidates (${resolved.size}) found for this injection point; " +
                    "disambiguate with @Qualifier or @Primary",
                isOnTheFly,
                fixes,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    // createAddAnnotationActions works across Java and Kotlin alike (it targets the generic
    // JvmModifiersOwner the light-PSI layer exposes for both), so one code path covers both
    // languages instead of hand-rolling PSI mutation per language.
    private fun qualifierQuickFixes(
        annotated: UAnnotated,
        candidates: List<com.intellij.psi.PsiClass>,
        containingFile: com.intellij.psi.PsiFile
    ): Array<LocalQuickFix> {
        val target = annotated.javaPsi as? JvmModifiersOwner ?: return emptyArray()
        return candidates.mapNotNull { candidate ->
            val beanName = SpringBeanResolver.beanName(candidate)
            val actions = JvmAnnotationQuickFixSupport.createAddStringAnnotationActions(
                target, SpringBeanResolver.QUALIFIER, "value", beanName
            )
            val action = actions.firstOrNull() ?: return@mapNotNull null
            IntentionWrapper.wrapToQuickFix(action, containingFile)
        }.toTypedArray()
    }
}
