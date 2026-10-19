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

package org.inferna.spring.interop;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.jvm.JvmModifiersOwner;
import com.intellij.lang.jvm.actions.AnnotationAttributeRequest;
import com.intellij.lang.jvm.actions.AnnotationAttributeValueRequestKt;
import com.intellij.lang.jvm.actions.AnnotationRequest;
import com.intellij.lang.jvm.actions.AnnotationRequestsKt;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;

import java.util.List;

/**
 * Thin Java bridge onto {@code JvmElementActionFactories} and the {@code AnnotationRequest}
 * builders. Same reason as {@link SpringValueReferenceSupport}: these are Kotlin
 * {@code internal} to the platform's own module and unresolvable from this plugin's
 * Kotlin sources directly, despite being ordinary public bytecode from Java's point
 * of view.
 */
public final class JvmAnnotationQuickFixSupport {

    private JvmAnnotationQuickFixSupport() {
    }

    /** Builds the IDE's own cross-language (Java/Kotlin) "add @Annotation(attr = value)" fixes. */
    public static List<IntentionAction> createAddStringAnnotationActions(
            JvmModifiersOwner target,
            String annotationFqName,
            String attributeName,
            String attributeValue
    ) {
        AnnotationAttributeRequest attribute = AnnotationAttributeValueRequestKt.stringAttribute(attributeName, attributeValue);
        AnnotationRequest request = AnnotationRequestsKt.annotationRequest(annotationFqName, attribute);
        return JvmElementActionFactories.createAddAnnotationActions(target, request);
    }
}
