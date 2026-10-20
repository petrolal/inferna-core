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

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.getContainingUClass

/**
 * Candidate-bean resolution shared by the LineMarker, inspection, and Biometrics Dashboard.
 * Automatically detects when IntelliJ IDEA Ultimate's native `com.intellij.spring` plugin is
 * active so editor markers/inspections defer to Ultimate without duplicate icons or warnings.
 */
object SpringBeanResolver {
    const val AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"
    const val QUALIFIER = "org.springframework.beans.factory.annotation.Qualifier"
    const val PRIMARY = "org.springframework.context.annotation.Primary"
    const val VALUE = "org.springframework.beans.factory.annotation.Value"
    const val BEAN = "org.springframework.context.annotation.Bean"
    private const val NATIVE_SPRING_PLUGIN_ID = "com.intellij.spring"

    private val STEREOTYPES = setOf(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Repository",
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.context.annotation.Configuration",
        "org.springframework.boot.autoconfigure.SpringBootApplication"
    )

    /**
     * Returns true when IntelliJ IDEA Ultimate's bundled `com.intellij.spring` plugin is
     * installed and enabled, allowing Inferna's fallback editor markers/inspections to step
     * aside and avoid duplicating Ultimate's native Spring tooling.
     */
    fun isNativeSpringPluginActive(): Boolean {
        val pluginId = PluginId.getId(NATIVE_SPRING_PLUGIN_ID)
        return PluginManagerCore.getPlugin(pluginId) != null && !PluginManagerCore.isDisabled(pluginId)
    }

    fun hasStereotype(psiClass: PsiClass): Boolean = STEREOTYPES.any { psiClass.hasAnnotation(it) }

    /**
     * A constructor is an implicit injection point when Spring would pick it up
     * without an explicit @Autowired: either it is annotated directly, or it is
     * the only constructor the class declares (Spring Framework's single-constructor
     * implicit-autowiring rule) on a Spring-managed component.
     */
    fun isInjectedConstructor(method: org.jetbrains.uast.UMethod): Boolean {
        if (!method.isConstructor) return false
        if (method.hasAnnotation(AUTOWIRED)) return true
        val psiClass = method.getContainingUClass()?.javaPsi ?: return false
        if (!hasStereotype(psiClass)) return false
        return psiClass.constructors.size == 1
    }

    fun candidatesFor(type: PsiType): List<PsiClass> {
        val targetClass = (type as? PsiClassType)?.resolve() ?: return emptyList()
        return candidatesFor(targetClass)
    }

    fun candidatesFor(targetClass: PsiClass): List<PsiClass> {
        return CachedValuesManager.getCachedValue(targetClass) {
            val candidates = when {
                targetClass.isInterface -> {
                    val scope = GlobalSearchScope.projectScope(targetClass.project)
                    ClassInheritorsSearch.search(targetClass, scope, true)
                        .filter { hasStereotype(it) }
                        .toList()
                }
                hasStereotype(targetClass) -> listOf(targetClass)
                else -> emptyList()
            }
            CachedValueProvider.Result.create(candidates, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    fun qualifierValue(annotated: UAnnotated): String? {
        val annotation = annotated.findAnnotation(QUALIFIER) ?: return null
        return annotation.findAttributeValue("value")?.evaluate() as? String
    }

    /** The bean name Spring would register this class under. */
    fun beanName(psiClass: PsiClass): String {
        for (stereotype in STEREOTYPES) {
            val annotation = psiClass.getAnnotation(stereotype) ?: continue
            val explicit = AnnotationUtil.getStringAttributeValue(annotation, "value")
            if (!explicit.isNullOrBlank()) return explicit
        }
        return psiClass.name.orEmpty().replaceFirstChar { it.lowercase() }
    }

    /**
     * Counts distinct Spring bean declarations across the project (stereotype-annotated
     * classes plus `@Bean` factory methods) for the Inferna Biometrics Dashboard.
     */
    fun countProjectBeans(project: Project): Int {
        val facade = JavaPsiFacade.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)
        val allScope = GlobalSearchScope.allScope(project)
        val foundClasses = mutableSetOf<PsiClass>()
        for (stereotype in STEREOTYPES) {
            val annotationClass = facade.findClass(stereotype, allScope) ?: continue
            foundClasses.addAll(AnnotatedElementsSearch.searchPsiClasses(annotationClass, projectScope).findAll())
        }
        val beanMethods = mutableSetOf<PsiMethod>()
        facade.findClass(BEAN, allScope)?.let { beanAnnotation ->
            beanMethods.addAll(AnnotatedElementsSearch.searchPsiMethods(beanAnnotation, projectScope).findAll())
        }
        return foundClasses.size + beanMethods.size
    }

    private fun classQualifier(psiClass: PsiClass): String? {
        val annotation = psiClass.getAnnotation(QUALIFIER) ?: return null
        return AnnotationUtil.getStringAttributeValue(annotation, "value")
    }

    /**
     * Narrows [candidates] down using, in order: an explicit @Qualifier at the
     * injection point, then an @Primary candidate. Falls back to the full list
     * when neither disambiguates anything (callers treat a result with more
     * than one entry as still ambiguous).
     */
    fun filterByQualifierAndPrimary(candidates: List<PsiClass>, qualifier: String?): List<PsiClass> {
        if (candidates.size <= 1) return candidates
        if (qualifier != null) {
            val matched = candidates.filter { beanName(it) == qualifier || classQualifier(it) == qualifier }
            if (matched.isNotEmpty()) return matched
        }
        val primary = candidates.filter { it.hasAnnotation(PRIMARY) }
        return if (primary.isNotEmpty()) primary else candidates
    }
}

