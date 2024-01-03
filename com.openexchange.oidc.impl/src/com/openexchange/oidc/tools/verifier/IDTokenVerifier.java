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

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.AuthorizedParty;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.openexchange.exception.OXException;
import com.openexchange.oidc.OIDCExceptionCode;

/**
 * {@link IDTokenVerifier}
 *
 * ID token claims verifier.
 *
 * <p>Related specifications:
 *
 * <ul>
 * <li>OpenID Connect Core 1.0, section 12.2
 * </ul>
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 */
public class IDTokenVerifier implements TokenVerifier<IDTokenClaimsSet> {

    private final Issuer expectedIssuer;
    private final Subject expectedSubject;
    private final Date issueTime;
    private final List<Audience> expectedAudience;
    private final Date expectedAuthenticationTime;
    private final AuthorizedParty expectedAuthorizedParty;

    private static final Logger LOG = LoggerFactory.getLogger(IDTokenVerifier.class);

    /**
     * Constructs an IDTokenVerifier with the specified parameters.
     *
     * @param issuer The expected issuer of the ID token.
     * @param subject The expected subject (user) of the ID token.
     * @param issueTime The expected issue time of the ID token.
     * @param audience The expected audience of the ID token.
     * @param authenticationTime The expected authentication time of the ID token.
     * @param authorizedParty The expected authorized party of the ID token.
     */
    public IDTokenVerifier(Issuer issuer, Subject subject, Date issueTime, List<Audience> audience, Date authenticationTime, AuthorizedParty authorizedParty) {
        this.expectedIssuer = issuer;
        this.expectedSubject = subject;
        this.issueTime = issueTime;
        this.expectedAudience = audience;
        this.expectedAuthenticationTime = authenticationTime;
        this.expectedAuthorizedParty = authorizedParty;
    }

    /**
     * Verifies the claims of an ID token.
     *
     * @param claimSet The ID token's claims set to be verified.
     * @throws OXException If any of the claims fail to match the expected values.
     */
    @Override
    public void verify(IDTokenClaimsSet claimSet) throws OXException {
        // iss Claim Value MUST be the same as in the ID Token issued when the original authentication occurred,
        assertStringClaimEquals("iss", expectedIssuer.getValue(), claimSet.getIssuer().getValue());

        // sub Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        assertStringClaimEquals("sub", expectedSubject.getValue(), claimSet.getSubject().getValue());

        // iat Claim MUST represent the time that the new ID Token is issued
        if (claimSet.getIssueTime().compareTo(issueTime) < 0) {
            LOG.debug("Invalid iat claim value. Expected {} but was {}", issueTime, claimSet.getIssueTime());
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid iat claim value");
        }

        // if the ID Token contains an auth_time Claim, its value MUST represent the time of the original authentication - not the time that the new ID token is issued,
        if (expectedAuthenticationTime != null && expectedAuthenticationTime.compareTo(claimSet.getAuthenticationTime()) != 0) {
            LOG.debug("Invalid auth_time claim value. Expected {} but was {}", expectedAuthenticationTime, claimSet.getAuthenticationTime());
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid auth_time claim value");
        }

        // aud Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        if (!expectedAudience.equals(claimSet.getAudience())) {
            LOG.debug("Invalid aud claim value. Expected {} but was {}", expectedAudience, claimSet.getAudience());
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid aud claim value");
        }

        // azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred; if no azp Claim was present in the original ID Token, one MUST NOT be present in the new ID Token
        if (expectedAuthorizedParty == null && claimSet.getAuthorizedParty() != null) {
            LOG.debug("Invalid azp claim value. No azp Claim was present in the original ID Token");
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid azp claim value");
        } else if (expectedAuthorizedParty != null && !expectedAuthorizedParty.equals(claimSet.getAuthorizedParty())) {
            LOG.debug("Invalid azp claim value. Expected {} but was {}", expectedAuthorizedParty, claimSet.getAuthorizedParty());
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid azp claim value");
        }
    }

    /**
     * Helper method to assert string claims equality.
     *
     * @param claimName The name of the claim to check.
     * @param expectedClaim The expected value of the claim.
     * @param claim The actual value of the claim.
     * @throws OXException If the claim values do not match.
     */
    private void assertStringClaimEquals(String claimName, String expectedClaim, String claim) throws OXException {
        if (!expectedClaim.equals(claim)) {
            LOG.debug("Invalid {} claim value. Expected {} but was {}", claimName, expectedClaim, claim);
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid " + claimName + " claim value");
        }
    }
}
