
package com.openexchange.multifactor.provider.webauthn.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

/**
 * {@link CompositeCredentialRepository}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class CompositeCredentialRepository implements CredentialRepository {

    final List<CredentialRepository> repositories;

    /**
     * Initializes a new {@link CompositeCredentialRepository}.
     *
     * @param repositories A list of {@link CredentialRepository} instances to use
     */
    public CompositeCredentialRepository(List<CredentialRepository> repositories) {
        this.repositories = repositories;
    }

    /**
     * Initializes a new {@link CompositeCredentialRepository}.
     *
     * @param repositories A list of {@link CredentialRepository} instances to use
     */
    public CompositeCredentialRepository(CredentialRepository... repositories) {
        this.repositories = Arrays.asList(repositories);
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        Set<PublicKeyCredentialDescriptor> ret = new HashSet<>();
        repositories.forEach(r -> ret.addAll(r.getCredentialIdsForUsername(username)));
        return ret;
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        for (CredentialRepository repo : repositories) {
            Optional<ByteArray> userHandle = repo.getUserHandleForUsername(username);
            if (userHandle.isPresent()) {
                return userHandle;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        for (CredentialRepository repo : repositories) {
            Optional<String> userName = repo.getUsernameForUserHandle(userHandle);
            if (userName.isPresent()) {
                return userName;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        for (CredentialRepository repo : repositories) {
            Optional<RegisteredCredential> registeredCredential = repo.lookup(credentialId, userHandle);
            if (registeredCredential.isPresent()) {
                return registeredCredential;
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        Set<RegisteredCredential> ret = new HashSet<>();
        repositories.forEach(r -> ret.addAll(r.lookupAll(credentialId)));
        return ret;
    }
}
