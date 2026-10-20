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

import com.intellij.openapi.application.ApplicationManager

/**
 * The one call site every feature check should use. Delegates to whichever
 * [InfernaLicenseProvider] is first on the `org.inferna.core.licenseProvider` extension point —
 * an external provider registering with a higher priority (lower `order`) than this repository's
 * own `order="last"` [DefaultInfernaLicenseProvider] becomes the active one automatically, with
 * no code change here.
 */
interface InfernaLicenseService {
    fun getTier(): LicenseTier

    fun isFeatureEnabled(featureId: String): Boolean

    companion object {
        fun getInstance(): InfernaLicenseService =
            ApplicationManager.getApplication().getService(InfernaLicenseService::class.java)
    }
}

class InfernaLicenseServiceImpl : InfernaLicenseService {
    private val activeProvider: InfernaLicenseProvider
        get() = InfernaLicenseProvider.EP_NAME.extensionList.firstOrNull()
            ?: DefaultInfernaLicenseProvider()

    override fun getTier(): LicenseTier = activeProvider.getTier()

    override fun isFeatureEnabled(featureId: String): Boolean = activeProvider.isFeatureEnabled(featureId)
}
