
package com.openexchange.multifactor.provider.webauthn.impl;

import java.time.Duration;
import com.openexchange.multifactor.MultifactorToken;
import com.yubico.webauthn.AssertionRequest;

/**
 * {@link WebAuthnToken} - Represents the temporary Challenge/Assertion token for WebAuthn authentication
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class WebAuthnToken extends MultifactorToken<AssertionRequest> {

    /**
     * Initializes a new {@link WebAuthnToken}.
     *
     * @param value The {@link AssertionRequest}
     * @param lifeTime The token lifetime
     */
    public WebAuthnToken(AssertionRequest value, Duration lifeTime) {
        super(value, lifeTime);
    }
}
