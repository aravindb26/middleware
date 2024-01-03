
package com.openexchange.oidc.impl.tests;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.AuthorizedParty;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.openexchange.exception.OXException;
import com.openexchange.oidc.tools.verifier.IDTokenVerifier;

class OIDCIDTokenVerifierTest {

    private IDTokenVerifier idTokenVerifier;
    private IDTokenClaimsSet claimSet;

    private final String subject = "anton@context1.ox.test";
    private final String issuer = "https://example.com";
    private final Date issue_time = new Date(System.currentTimeMillis());
    private final Date exp_time = new Date(System.currentTimeMillis() + 3600 * 1000);;
    private final String audience = "appsuite-client";
    private final Date authentication_time = new Date(System.currentTimeMillis());
    private final AuthorizedParty authorized_party = new AuthorizedParty("client123");

    @BeforeEach
    void setUp() throws ParseException {
        claimSet = new IDTokenClaimsSet(new JWTClaimsSet.Builder().subject(subject)
                                                                  .issuer(issuer)
                                                                  .issueTime(issue_time)
                                                                  .audience(audience)
                                                                  .expirationTime(exp_time)
                                                                  .build());

        claimSet.setAuthenticationTime(authentication_time);
        claimSet.setAuthorizedParty(authorized_party);

        idTokenVerifier = new IDTokenVerifier(claimSet.getIssuer(),
                                              claimSet.getSubject(),
                                              claimSet.getIssueTime(),
                                              claimSet.getAudience(),
                                              claimSet.getAuthenticationTime(),
                                              claimSet.getAuthorizedParty());
    }

    @Test
    void testValidClaimsSet() {
        // Ensure that the verification does not throw an exception for valid claims.
        Assertions.assertDoesNotThrow(() -> idTokenVerifier.verify(claimSet));
    }

    @Test
    void testInvalidIssuer() {
        // Modify the issuer value to an invalid one.
        Issuer invalidIssuer = new Issuer("https://evil.com");
        claimSet.setIssuer(invalidIssuer);

        // Ensure that verification throws an exception for an invalid issuer.
        Assertions.assertThrows(OXException.class, () -> idTokenVerifier.verify(claimSet));
    }

    @Test
    void testInvalidSubject() throws ParseException {
        // Modify the subject value to an invalid one.
        String invalidSubject = "hacker123";
        claimSet = new IDTokenClaimsSet(new JWTClaimsSet.Builder().subject(invalidSubject)
                                                                  .issuer(issuer)
                                                                  .issueTime(issue_time)
                                                                  .audience(audience)
                                                                  .expirationTime(exp_time)
                                                                  .build());

        claimSet.setAuthenticationTime(authentication_time);
        claimSet.setAuthorizedParty(authorized_party);

        // Ensure that verification throws an exception for an invalid subject.
        Assertions.assertThrows(OXException.class, () -> idTokenVerifier.verify(claimSet));
    }

    @Test
    void testInvalidAudience() {
        // Modify the audience value to an invalid one.
        List<Audience> invalidAudience = Arrays.asList(new Audience("invalidClient"));
        claimSet.setAudience(invalidAudience);

        // Ensure that verification throws an exception for an invalid audience.
        Assertions.assertThrows(OXException.class, () -> idTokenVerifier.verify(claimSet));
    }

    @Test
    void testMissingAuthorizedParty() {
        // Set the expectedAuthorizedParty to null.
        idTokenVerifier = new IDTokenVerifier(claimSet.getIssuer(),
                                              claimSet.getSubject(),
                                              claimSet.getIssueTime(),
                                              claimSet.getAudience(),
                                              claimSet.getAuthenticationTime(),
                                              null);

        // Ensure that verification throws an exception when 'azp' is present in claims.
        Assertions.assertThrows(OXException.class, () -> idTokenVerifier.verify(claimSet));
    }

    @Test
    void testInvalidAuthorizedParty() {
        // Modify the authorized party value to an invalid one.
        AuthorizedParty invalidAuthorizedParty = new AuthorizedParty("invalidClient");
        claimSet.setAuthorizedParty(invalidAuthorizedParty);

        // Ensure that verification throws an exception for an invalid authorized party.
        Assertions.assertThrows(OXException.class, () -> idTokenVerifier.verify(claimSet));
    }
}
