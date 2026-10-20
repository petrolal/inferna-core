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

package org.inferna.spring

import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.inferna.spring.interop.SpringValueReferenceSupport

/**
 * Resolves `${some.key}` placeholders inside @Value to the matching key in a
 * .properties file anywhere in the project, so ctrl+click / "Go to Declaration"
 * from a Spring property placeholder lands on its definition.
 *
 * Scoped to .properties only for now: it reads the candidate file's generic
 * PsiFile text directly (no dependency on the Properties plugin's own PSI, just
 * its FileType for discovery), which keeps this simple and polyglot-safe but
 * does not (yet) cover application.yml - see docs/STANDALONE_ROADMAP.md Phase 1.
 */
class SpringValueReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        SpringValueReferenceSupport.registerValuePlaceholderReferences(
            registrar,
            SpringBeanResolver.VALUE
        ) { _, host ->
            if (SpringBeanResolver.isNativeSpringPluginActive()) {
                return@registerValuePlaceholderReferences PsiReference.EMPTY_ARRAY
            }
            PLACEHOLDER.findAll(host.text)
                .mapNotNull { match ->
                    val keyGroup = match.groups[1] ?: return@mapNotNull null
                    val range = TextRange(keyGroup.range.first, keyGroup.range.last + 1)
                    SpringPropertyReference(host, range, keyGroup.value) as PsiReference
                }
                .toList()
                .toTypedArray()
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("""\$\{\s*([A-Za-z0-9_.\-]+)(?::[^}]*)?\s*}""")
    }
}

private class SpringPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val key: String
) : PsiReferenceBase<PsiElement>(element, range, /* soft = */ true) {

    override fun resolve(): PsiElement? {
        val project = element.project
        val scope = GlobalSearchScope.projectScope(project)
        for (virtualFile in FileTypeIndex.getFiles(PropertiesFileType.INSTANCE, scope)) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
            val offset = findPropertyKeyOffset(psiFile.text, key) ?: continue
            psiFile.findElementAt(offset)?.let { return it }
        }
        return null
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private fun findPropertyKeyOffset(text: String, key: String): Int? {
        var offset = 0
        for (line in text.split("\n")) {
            val trimmed = line.trimStart()
            val leadingWhitespace = line.length - trimmed.length
            if (trimmed.isNotEmpty() && trimmed[0] != '#' && trimmed[0] != '!') {
                val separatorIndex = trimmed.indexOfFirst { it == '=' || it == ':' }
                if (separatorIndex > 0 && trimmed.substring(0, separatorIndex).trim() == key) {
                    return offset + leadingWhitespace
                }
            }
            offset += line.length + 1
        }
        return null
    }
}
