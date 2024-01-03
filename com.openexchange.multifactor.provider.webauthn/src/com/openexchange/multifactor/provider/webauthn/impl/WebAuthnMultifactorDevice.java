
package com.openexchange.multifactor.provider.webauthn.impl;

import com.yubico.webauthn.data.ByteArray;

/**
 * 
 * {@link WebAuthnMultifactorDevice}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class WebAuthnMultifactorDevice {

    /**
     * Gets the ID of the device
     * 
     * @return The ID of the device
     */
    public ByteArray getId() {
        //Not yet implemented
        //We do not support WebAuthn device registration yet
        return null;
    }
}
