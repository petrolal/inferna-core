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
