package org.inferna.spring.interop;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceProvider;
import com.intellij.psi.UastReferenceRegistrar;
import kotlin.jvm.functions.Function2;
import org.jetbrains.uast.UExpression;

/**
 * Thin Java bridge onto {@code UastReferenceRegistrar} / {@code UastPatterns}.
 *
 * Both are Kotlin {@code internal} to the platform's own module, so the Kotlin
 * compiler refuses to resolve them from this plugin's Kotlin sources ("Unresolved
 * reference") even though the classes are public, loadable bytecode and Java call
 * sites compile against them without complaint. This file exists only to cross
 * that module boundary; callers should go through {@link #registerValuePlaceholderReferences}
 * from Kotlin rather than reference these platform classes directly.
 */
public final class SpringValueReferenceSupport {

    private SpringValueReferenceSupport() {
    }

    @FunctionalInterface
    public interface PlaceholderReferenceFactory {
        PsiReference[] create(UExpression expression, PsiLanguageInjectionHost host);
    }

    public static void registerValuePlaceholderReferences(
            PsiReferenceRegistrar registrar,
            String annotationFqName,
            PlaceholderReferenceFactory factory
    ) {
        ElementPattern<UExpression> pattern = UastPatterns.injectionHostUExpression().annotationParam("value", annotationFqName);
        Function2<UExpression, PsiLanguageInjectionHost, PsiReference[]> delegate = factory::create;
        UastReferenceProvider provider = UastReferenceRegistrar.uastInjectionHostReferenceProvider(delegate);
        UastReferenceRegistrar.registerUastReferenceProvider(registrar, pattern, provider);
    }
}
