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

package org.inferna.scala

import com.intellij.openapi.project.Project

/** Neutral DTO — carries no `org.jetbrains.plugins.scala.*` type, safe to load with no Scala plugin present. */
data class DeepScalaStats(
    val objectSingletons: Int,
    val caseClasses: Int,
    val implicitOrGivenDefs: Int
)

/**
 * Compile-time-safe seam between the always-loaded core and `:modules:scala`. The core never
 * imports a Scala PSI type; it only ever sees this interface and [DeepScalaStats]. The real
 * implementation is registered by `inferna-scala.xml`, loaded by the platform only when
 * `org.intellij.scala` is present, so [ApplicationManager.getApplication().getService] returns
 * null on any IDE without the Scala plugin installed instead of throwing.
 */
interface ScalaAnalyzerBridge {
    fun inspectProject(project: Project): DeepScalaStats
}
