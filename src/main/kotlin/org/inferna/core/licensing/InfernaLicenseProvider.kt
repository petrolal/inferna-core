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

package org.inferna.core.licensing

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Extension point (`org.inferna.core.licenseProvider`) through which an external plugin — e.g.
 * a closed-source `inferna-pro` distributed outside this repository — reports entitlement. The
 * Core itself ships only [DefaultInfernaLicenseProvider], registered last, so the Core always
 * builds, verifies, and runs standalone with no paid logic present. No implementation detail of
 * any commercial tier lives here; this interface is the entire contract.
 */
interface InfernaLicenseProvider {
    fun getTier(): LicenseTier

    fun isFeatureEnabled(featureId: String): Boolean

    companion object {
        val EP_NAME: ExtensionPointName<InfernaLicenseProvider> =
            ExtensionPointName.create("org.inferna.core.licenseProvider")
    }
}
