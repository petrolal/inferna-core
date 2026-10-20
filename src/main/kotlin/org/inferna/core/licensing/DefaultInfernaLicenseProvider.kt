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

package org.inferna.core.licensing

/**
 * Open-core fallback registered by this repository on `org.inferna.core.licenseProvider` with
 * `order="last"`: always reports [LicenseTier.COMMUNITY] and never enables a feature flag. Carries
 * no knowledge of what a paid tier does or how it is entitled — an external provider with a
 * higher order (e.g. a separately distributed `inferna-pro` plugin) simply outranks this one.
 */
class DefaultInfernaLicenseProvider : InfernaLicenseProvider {
    override fun getTier(): LicenseTier = LicenseTier.COMMUNITY

    override fun isFeatureEnabled(featureId: String): Boolean = false
}
