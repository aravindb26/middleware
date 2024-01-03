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

package com.openexchange.saml.validation;

import org.opensaml.saml.saml2.core.Assertion;


/**
 * The result object of a response validation via {@link ValidationStrategy}.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public class AuthnResponseValidationResult {

    private final Assertion bearerAssertion;

    /**
     * Initializes a new {@link AuthnResponseValidationResult}.
     * @param bearerAssertion
     * @param requestInfo
     */
    public AuthnResponseValidationResult(Assertion bearerAssertion) {
        super();
        this.bearerAssertion = bearerAssertion;
    }

    /**
     * Gets the bearer assertion determined during validation of the authentication response.
     *
     * @return The bearer assertion
     */
    public Assertion getBearerAssertion() {
        return bearerAssertion;
    }

}
