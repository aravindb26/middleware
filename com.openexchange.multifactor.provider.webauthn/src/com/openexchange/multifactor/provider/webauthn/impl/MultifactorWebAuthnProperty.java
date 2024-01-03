
package com.openexchange.multifactor.provider.webauthn.impl;

import com.openexchange.config.lean.Property;
import com.openexchange.multifactor.MultifactorProperties;

/**
 * {@link MultifactorWebAuthnProperty} configuration properties for the WebAuthn multifactor authentication
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public enum MultifactorWebAuthnProperty implements Property {

    /**
     * Defines if the WebAuthn provider is enabled.
     * 
     * Only providers which are "enabled" can be used by a user.
     */
    enabled(Boolean.FALSE),

    /**
     * Defines the ID of the WebAuthn Relying Party propagated to the client. Empty to use the host-name
     */
    rpId(""),

    /**
     * Defines the lifetime of a WebAuthn challenge token for authentication; i.e this is the time the client needs to resolve the challenge.
     */
    tokenLifetime(Integer.valueOf(60));

    private static final String PREFIX = MultifactorProperties.PREFIX + "webAuthn.";
    private Object defaultValue;

    /**
     * Initializes a new {@link MultifactorWebAuthnProperty}.
     *
     * @param defaultValue The default value for the property
     */
    MultifactorWebAuthnProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
