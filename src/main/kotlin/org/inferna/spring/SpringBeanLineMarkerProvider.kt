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
