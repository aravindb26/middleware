
package com.openexchange.multifactor.provider.webauthn.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

/**
 * {@link WebAuthnCredentialRepository}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class WebAuthnCredentialRepository implements CredentialRepository {

    private Collection<WebAuthnMultifactorDevice> getWebAuthnDevices(@SuppressWarnings("unused") String username) {
        //Prepared for future use.
        //At the moment we do not support WebAuthn, so we do not have a list of WebAuthn devices and just return an empty list.
        return Collections.emptyList();
    }

    private Collection<PublicKeyCredentialDescriptor> getWebAuthnCredentials(String username) {
        //@formatter:off
        return getWebAuthnDevices(username).stream().map( webAuthnDevice -> 
            PublicKeyCredentialDescriptor.builder()
            .id(webAuthnDevice.getId())
            .type(PublicKeyCredentialType.PUBLIC_KEY)
            .build()
        ).collect(Collectors.toList());
        //@formatter:on
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return new HashSet<>(getWebAuthnCredentials(username));
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return Optional.empty();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return Collections.emptySet();
    }
}
