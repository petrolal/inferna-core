package org.inferna.spring

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.inferna.icons.InfernaIcons
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

        val candidates = findSpringBeanCandidates(targetClass)
        if (candidates.isEmpty()) return

        val anchor = uField.sourcePsi?.firstChild ?: element
        val lineMarker = NavigationGutterIconBuilder.create(InfernaIcons.SpringBean)
            .setTargets(candidates)
            .setTooltipText("Inferna: Navigate to Spring Bean Declaration")
            .createLineMarkerInfo(anchor)

        result.add(lineMarker)
    }

    // Uses the class hierarchy index (ClassInheritorsSearch) instead of scanning every class in
    // the project, and caches the result per target PsiClass so repeated highlight passes over the
    // same @Autowired field type don't re-run the search on every keystroke.
    private fun findSpringBeanCandidates(targetClass: PsiClass): List<PsiClass> {
        return CachedValuesManager.getCachedValue(targetClass) {
            val candidates = if (targetClass.isInterface) {
                val scope = GlobalSearchScope.projectScope(targetClass.project)
                ClassInheritorsSearch.search(targetClass, scope, true)
                    .filter { hasSpringStereotype(it) }
                    .toList()
            } else if (hasSpringStereotype(targetClass)) {
                listOf(targetClass)
            } else {
                emptyList()
            }
            CachedValueProvider.Result.create(candidates, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    private fun hasSpringStereotype(psiClass: PsiClass): Boolean {
        return springAnnotations.any { psiClass.hasAnnotation(it) }
    }
}
