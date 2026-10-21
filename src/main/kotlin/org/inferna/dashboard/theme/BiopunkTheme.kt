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

package org.inferna.dashboard.theme

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Font

object BiopunkTheme {
    val TEXT_PRIMARY = JBColor(0x221414, 0xE0D0D0)
    val TEXT_SECONDARY = JBColor(0x4A3636, 0x9E8686)
    val TEXT_MUTED = JBColor(0x6E5A5A, 0x6E5858)
    val ACCENT_CRIMSON = JBColor(0x961020, 0xE63946)
    val SECTION_HEADER = JBColor(0x752028, 0x8F3038)

    val FONT_MONO_REGULAR: Font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
    val FONT_MONO_BOLD: Font = Font(Font.MONOSPACED, Font.BOLD, JBUI.scale(12))

    fun renderAsciiGauge(percentage: Int, blocks: Int = 10): String {
        val safePercentage = percentage.coerceIn(0, 100)
        val filled = (safePercentage * blocks) / 100
        val empty = blocks - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }

    /**
     * A section title embedded directly inside its own horizontal rule — the platform's
     * native titled-group pattern (same as IntelliJ Settings pages) — instead of a plain
     * label row sitting above a separate full-width separator line.
     */
    fun Panel.biopunkGroup(title: String, init: Panel.() -> Unit): Row {
        val titleLabel = JBLabel(title).apply {
            font = JBFont.small().asBold()
            foreground = SECTION_HEADER
        }
        return group(titleLabel, false, init)
    }
}