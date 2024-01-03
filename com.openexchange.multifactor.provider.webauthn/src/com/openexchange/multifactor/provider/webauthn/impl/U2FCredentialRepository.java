
package com.openexchange.multifactor.provider.webauthn.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.multifactor.MultifactorRequest;
import com.openexchange.multifactor.provider.u2f.impl.U2FMultifactorDevice;
import com.openexchange.multifactor.provider.u2f.storage.U2FMultifactorDeviceStorage;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.exception.Base64UrlException;

/**
 * {@link U2FCredentialRepository} - A {@link CredentialRepository} for supporting U2F devices for backwards compatibility reason
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class U2FCredentialRepository implements CredentialRepository {

    private static final Logger LOG = LoggerFactory.getLogger(U2FCredentialRepository.class);

    private U2FMultifactorDeviceStorage u2fStorage;
    private MultifactorRequest multifactorRequest;

    /**
     * Initializes a new {@link U2FCredentialRepository}.
     *
     * @param u2fStorage The underlying device storage to use
     * @param multifactorRequest The request
     */
    public U2FCredentialRepository(U2FMultifactorDeviceStorage u2fStorage, MultifactorRequest multifactorRequest) {
        this.u2fStorage = u2fStorage;
        this.multifactorRequest = multifactorRequest;
    }

    /**
     * Internal method to query U2F devices for the given request's user
     *
     * @param multifactorRequest The request to get the devices for
     * @return A list of devices for the given request
     * @throws OXException
     */
    private Collection<U2FMultifactorDevice> getU2FDevices(MultifactorRequest multifactorRequest) throws OXException {
        return u2fStorage.getDevices(multifactorRequest.getContextId(), multifactorRequest.getUserId());
    }

    /**
     * Internal method to get a list of credentials for the given request's user
     *
     * @param multifactorRequest The request to get the credentials for
     * @return A set of credentials for the given request
     * @throws OXException
     */
    private Collection<PublicKeyCredentialDescriptor> getU2FCredentials(MultifactorRequest multifactorRequest) throws OXException {
        //@formatter:off
        return getU2FDevices(multifactorRequest).stream().map( u2fDevice -> {
            try {
                //@formatter:off
                return PublicKeyCredentialDescriptor.builder()
                                                    .id(ByteArray.fromBase64Url(u2fDevice.getKeyHandle()))
                                                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                                                    .build();
                //@formatter:on
            } catch (@SuppressWarnings("unused") Base64UrlException e) {
                return null;
            }
        }).collect(Collectors.toList());
        //@formatter:on
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        try {
            return new HashSet<>(getU2FCredentials(this.multifactorRequest));
        } catch (OXException e) {
            LOG.error(e.getMessage());
        }
        return Collections.emptySet();
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
        try {
            Collection<U2FMultifactorDevice> u2fDevices = u2fStorage.getDevices(multifactorRequest.getContextId(), multifactorRequest.getUserId());
            //The WebAuthn "credential ID" is the "U2F KeyHandle"
            Optional<U2FMultifactorDevice> u2fDevice = u2fDevices.stream().filter(d -> d.getKeyHandle().equals(credentialId.getBase64Url())).findFirst();
            if (u2fDevice.isPresent()) {
                //@formatter:off
                RegisteredCredential registeredCredentials = RegisteredCredential.builder()
                                                                                 .credentialId(credentialId)
                                                                                 .userHandle(userHandle)
                                                                                 .publicKeyEs256Raw(ByteArray.fromBase64Url(u2fDevice.get().getPublicKey()))
                                                                                 .signatureCount(u2fDevice.get().getCounter())
                                                                                 .build();
                //@formatter:on
                return Optional.of(registeredCredentials);
            }
        } catch (OXException e) {
            LOG.error(e.getMessage());
        } catch (Base64UrlException e) {
            LOG.error(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return Collections.emptySet();
    }
}
