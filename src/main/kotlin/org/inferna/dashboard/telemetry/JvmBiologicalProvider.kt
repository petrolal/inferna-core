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

package org.inferna.dashboard.telemetry

import com.intellij.openapi.project.Project
import java.lang.management.ManagementFactory

data class JvmStats(
    val usedMb: Long,
    val maxMb: Long,
    val percentage: Int,
    val activeThreads: Int,
    val gcCount: Long
)

class JvmBiologicalProvider : BiometricProvider<JvmStats> {

    override fun isApplicable(project: Project): Boolean = true

    override fun scan(project: Project): JvmStats {
        val heapUsage = ManagementFactory.getMemoryMXBean().heapMemoryUsage
        val used = heapUsage.used / (1024 * 1024)
        val max = heapUsage.max / (1024 * 1024)
        val percentage = if (max > 0) ((used.toDouble() / max) * 100).toInt() else 0
        val threads = ManagementFactory.getThreadMXBean().threadCount
        val gcCycles = ManagementFactory.getGarbageCollectorMXBeans().sumOf { it.collectionCount }

        return JvmStats(used, max, percentage, threads, gcCycles)
    }
}