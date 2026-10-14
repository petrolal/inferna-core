package org.inferna.spring

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElement

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    private val autowiredAnnotation = "org.springframework.beans.factory.annotation.Autowired"
    private val springAnnotations = setOf(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Repository",
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController"
    )

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uField = element.toUElement() as? UField ?: return

        // Intercept injection points
        if (!uField.hasAnnotation(autowiredAnnotation)) return

        val fieldType = uField.type
        val targetClass = (fieldType as? PsiClassType)?.resolve() ?: return
        val project = element.project

        val candidates = mutableListOf<PsiClass>()

        if (targetClass.isInterface) {
            AllClassesSearch.search(GlobalSearchScope.projectScope(project), project).forEach { psiClass ->
                if (psiClass.isInheritor(targetClass, true) && hasSpringStereotype(psiClass)) {
                    candidates.add(psiClass)
                }
            }
        } else if (hasSpringStereotype(targetClass)) {
            candidates.add(targetClass)
        }

        if (candidates.isNotEmpty()) {
            val anchor = uField.sourcePsi?.firstChild ?: element
            val lineMarker = NavigationGutterIconBuilder.create(AllIcons.Nodes.Services)
                .setTargets(candidates)
                .setTooltipText("Inferna: Navigate to Spring Bean Declaration")
                .createLineMarkerInfo(anchor)

            result.add(lineMarker)
        }
    }

    private fun hasSpringStereotype(psiClass: PsiClass): Boolean {
        return springAnnotations.any { psiClass.hasAnnotation(it) }
    }
}
