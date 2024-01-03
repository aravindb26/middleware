/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.oidc.tools.verifier;

import com.nimbusds.openid.connect.sdk.claims.CommonClaimsSet;
import com.openexchange.exception.OXException;

/**
 * {@link IDTokenVerifier}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 */
public interface TokenVerifier<C extends CommonClaimsSet> {

    /**
     * Perform checks to verify that the given token is valid.
     * 
     * @param claimSet The claimSet to check
     * @throws OXException Signals that the token did not pass the checks.
     */
    void verify(C claimSet) throws OXException;

}
