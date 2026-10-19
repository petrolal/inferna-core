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

package org.inferna.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object InfernaIcons {
    @JvmField
    val Dashboard: Icon = IconLoader.getIcon("/icons/infernaDashboard.svg", InfernaIcons::class.java)

    @JvmField
    val SpringBean: Icon = IconLoader.getIcon("/icons/springBeanGutter.svg", InfernaIcons::class.java)
}
